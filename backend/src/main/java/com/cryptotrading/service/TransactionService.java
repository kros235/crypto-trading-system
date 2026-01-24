package com.cryptotrading.service;

import com.cryptotrading.dto.DashboardStatsDTO;
import com.cryptotrading.dto.TransactionDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.Transaction.TransactionType;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final CoinInfoService coinInfoService;

    /**
     * 거래 내역 생성
     */
    @Transactional
    public TransactionDTO createTransaction(String userId, TransactionDTO dto) {
        dto.setUserId(userId);
        
        // 거래 수수료 계산 (업비트 기본 0.05%)
        if (dto.getFee() == null) {
            dto.setFee(dto.getTotalAmount().multiply(new BigDecimal("0.0005"))
                    .setScale(2, RoundingMode.HALF_UP));
        }

        Transaction transaction = dto.toEntity();
        Transaction saved = transactionRepository.save(transaction);
        
        log.info("거래 생성: userId={}, coinSymbol={}, type={}, amount={}", 
                userId, dto.getCoinSymbol(), dto.getType(), dto.getTotalAmount());
        
        return TransactionDTO.fromEntity(saved);
    }

    /**
     * 거래 내역 전체 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getAllTransactions(String userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return transactions.map(this::enrichWithCurrentPrice);
    }

    /**
     * 거래 내역 검색 (복합 조건)
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> searchTransactions(
            String userId, 
            String coinSymbol, 
            TransactionStatus status,
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.searchTransactions(
                userId, coinSymbol, status, startDate, endDate, pageable);
        
        return transactions.map(this::enrichWithCurrentPrice);
    }

    /**
     * 보유 중인 자산 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getHoldings(String userId) {
        List<Transaction> holdings = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        
        return holdings.stream()
                .map(this::enrichWithCurrentPrice)
                .collect(Collectors.toList());
    }

    /**
     * 특정 거래 상세 조회
     */
    @Transactional(readOnly = true)
    public TransactionDTO getTransaction(String userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        return enrichWithCurrentPrice(transaction);
    }

    /**
     * 거래 내역 수정 (메모 등)
     */
    @Transactional
    public TransactionDTO updateTransaction(String userId, Long transactionId, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        // 메모만 수정 가능
        if (dto.getNote() != null) {
            transaction.setNote(dto.getNote());
        }
        
        Transaction updated = transactionRepository.save(transaction);
        return TransactionDTO.fromEntity(updated);
    }

    /**
     * 매도 처리
     */
    @Transactional
    public TransactionDTO sellTransaction(String userId, Long transactionId, BigDecimal soldPrice) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        if (transaction.getStatus() != TransactionStatus.HOLDING) {
            throw new IllegalArgumentException("보유 중인 자산만 매도할 수 있습니다.");
        }
        
        // 매도 정보 업데이트
        transaction.setSoldAt(LocalDateTime.now());
        transaction.setSoldPrice(soldPrice);
        transaction.setStatus(TransactionStatus.SOLD);
        
        // 손익 계산
        BigDecimal buyAmount = transaction.getTotalAmount();
        BigDecimal sellAmount = soldPrice.multiply(transaction.getQuantity());
        BigDecimal profitLoss = sellAmount.subtract(buyAmount);
        BigDecimal profitLossPct = profitLoss.divide(buyAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        transaction.setProfitLoss(profitLoss);
        transaction.setProfitLossPct(profitLossPct);
        
        Transaction updated = transactionRepository.save(transaction);
        
        log.info("매도 완료: transactionId={}, profit={}원 ({}%)", 
                transactionId, profitLoss, profitLossPct);
        
        return TransactionDTO.fromEntity(updated);
    }

    /**
     * 대시보드 통계 조회
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(String userId) {
        // 보유 자산 조회
        List<Transaction> holdings = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        
        BigDecimal totalHoldingAmount = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        
        // 각 보유 자산의 현재가 조회 및 평가액 계산
        for (Transaction holding : holdings) {
            totalHoldingAmount = totalHoldingAmount.add(holding.getTotalAmount());
            
            try {
                UpbitTickerDTO ticker = coinInfoService.getCurrentPrice(holding.getCoinSymbol());
                BigDecimal currentPrice = ticker.getTradePrice();
                BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());
                totalCurrentValue = totalCurrentValue.add(currentValue);
            } catch (Exception e) {
                log.warn("현재가 조회 실패: {}", holding.getCoinSymbol());
                totalCurrentValue = totalCurrentValue.add(holding.getTotalAmount());
            }
        }
        
        // 총 수익률 계산
        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalHoldingAmount);
        BigDecimal totalProfitLossPct = totalHoldingAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalProfitLoss.divide(totalHoldingAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        // 실현 손익
        BigDecimal realizedProfitLoss = transactionRepository.sumTotalProfitLoss(userId);
        
        // 거래 통계
        long totalBuyCount = transactionRepository.countBuyTransactions(userId);
        long totalSellCount = transactionRepository.countSellTransactions(userId);
        
        // 오늘 거래 정보
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        BigDecimal todayBuyAmount = transactionRepository.sumDailyTransactionAmount(
                userId, TransactionType.BUY, todayStart, todayEnd);
        BigDecimal todaySellAmount = transactionRepository.sumDailyTransactionAmount(
                userId, TransactionType.SELL, todayStart, todayEnd);
        
        // 거래 설정에서 일일 한도 조회
        TradingSetting setting = tradingSettingRepository.findByUserId(userId).orElse(null);
        BigDecimal dailyLimitAmount = setting != null 
                ? setting.getDailyLimitAmount() 
                : new BigDecimal("1000000");
        BigDecimal remainingDailyLimit = dailyLimitAmount.subtract(todayBuyAmount);
        
        int todayBuyCount = transactionRepository.countByUserIdAndTypeAndCreatedAtBetween(
                userId, TransactionType.BUY, todayStart, todayEnd);
        int todaySellCount = transactionRepository.countByUserIdAndTypeAndCreatedAtBetween(
                userId, TransactionType.SELL, todayStart, todayEnd);

        return DashboardStatsDTO.builder()
                .totalHoldingAmount(totalHoldingAmount)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPct(totalProfitLossPct)
                .realizedProfitLoss(realizedProfitLoss)
                .totalBuyCount(totalBuyCount)
                .totalSellCount(totalSellCount)
                .currentHoldingCount(holdings.size())
                .todayBuyAmount(todayBuyAmount)
                .todaySellAmount(todaySellAmount)
                .todayBuyCount(todayBuyCount)
                .todaySellCount(todaySellCount)
                .dailyLimitAmount(dailyLimitAmount)
                .remainingDailyLimit(remainingDailyLimit)
                .build();
    }

    /**
     * 현재가 정보를 포함한 TransactionDTO 생성
     */
    private TransactionDTO enrichWithCurrentPrice(Transaction transaction) {
        TransactionDTO dto = TransactionDTO.fromEntity(transaction);
        
        // 보유 중인 자산인 경우 현재가 조회
        if (transaction.getStatus() == TransactionStatus.HOLDING) {
            try {
                UpbitTickerDTO ticker = coinInfoService.getCurrentPrice(transaction.getCoinSymbol());
                BigDecimal currentPrice = ticker.getTradePrice();
                dto.setCurrentPrice(currentPrice);
            
                // 현재 평가 손익 계산
                BigDecimal currentValue = currentPrice.multiply(transaction.getQuantity());
                BigDecimal currentProfitLoss = currentValue.subtract(transaction.getTotalAmount());
                BigDecimal currentProfitLossPct = currentProfitLoss
                        .divide(transaction.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            
                dto.setCurrentProfitLoss(currentProfitLoss);
                dto.setCurrentProfitLossPct(currentProfitLossPct);
            } catch (Exception e) {
                log.warn("현재가 조회 실패: {}", transaction.getCoinSymbol());
            }
        }
    
        return dto;
    }
}