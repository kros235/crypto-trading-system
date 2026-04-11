package com.cryptotrading.service;

import com.cryptotrading.dto.kis.KisQuoteDTO;
import com.cryptotrading.dto.stock.StockTransactionDTO;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockInfoRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주식 거래 내역 서비스
 * Phase 2 Day 58
 *
 * Phase 1 TransactionService 패턴 재사용 (~75%)
 * 주요 차이점:
 *  - 수수료: 0.05%(코인) → 0.015%(주식)
 *  - quantity: BigDecimal → Integer
 *  - 현재가 조회: KisApiService.getCurrentPrice(userId, stockCode) 사용
 *  - 현재가 변환: quote.getCurrentPriceDecimal() 활용 (null-safe)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final StockInfoRepository stockInfoRepository;
    private final KisApiService kisApiService;

    // ──────────────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StockTransactionDTO> getAllTransactions(String userId, Pageable pageable) {
        return stockTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::enrichWithCurrentPrice);
    }

    @Transactional(readOnly = true)
    public Page<StockTransactionDTO> searchTransactions(
            String userId, String stockCode, TransactionStatus status,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return stockTransactionRepository
                .searchTransactions(userId, stockCode, status, startDate, endDate, pageable)
                .map(this::enrichWithCurrentPrice);
    }

    @Transactional(readOnly = true)
    public List<StockTransactionDTO> getHoldings(String userId) {
        return stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING)
                .stream()
                .map(this::enrichWithCurrentPrice)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockTransactionDTO getTransaction(String userId, Long transactionId) {
        StockTransaction tx = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        if (!tx.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return enrichWithCurrentPrice(tx);
    }

    // ──────────────────────────────────────────────────────────
    // 수정
    // ──────────────────────────────────────────────────────────

    @Transactional
    public StockTransactionDTO updateTransaction(String userId, Long transactionId,
                                                  StockTransactionDTO dto) {
        StockTransaction tx = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        if (!tx.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        if (dto.getNote() != null) {
            tx.setNote(dto.getNote());
        }
        return enrichWithCurrentPrice(stockTransactionRepository.save(tx));
    }
    @Transactional
    public StockTransactionDTO sellTransaction(String userId, Long transactionId,
                                                BigDecimal soldPrice) {
        StockTransaction tx = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));
        if (!tx.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        if (tx.getStatus() != TransactionStatus.HOLDING) {
            throw new IllegalArgumentException("보유 중인 자산만 매도할 수 있습니다.");
        }

        tx.setSoldAt(LocalDateTime.now());
        tx.setSoldPrice(soldPrice);
        tx.setStatus(TransactionStatus.SOLD);

        // 손익 계산 (주식: quantity가 Integer)
        BigDecimal sellAmount = soldPrice.multiply(BigDecimal.valueOf(tx.getQuantity()));
        BigDecimal profitLoss = sellAmount.subtract(tx.getTotalAmount());
        BigDecimal profitLossPct = profitLoss
                .divide(tx.getTotalAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        tx.setProfitLoss(profitLoss);
        tx.setProfitLossPct(profitLossPct);

        log.info("[주식 수동매도] transactionId={}, stockCode={}, profit={}원 ({}%)",
                transactionId, tx.getStockCode(), profitLoss, profitLossPct);

        return enrichWithCurrentPrice(stockTransactionRepository.save(tx));
    }

    // ──────────────────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────────────────

    private StockTransactionDTO enrichWithCurrentPrice(StockTransaction tx) {
        StockTransactionDTO dto = StockTransactionDTO.fromEntity(tx);

        // 종목명 세팅
        stockInfoRepository.findById(tx.getStockCode())
                .ifPresent(info -> dto.setStockName(info.getStockName()));

        // 보유 중일 때만 현재가 조회
        if (tx.getStatus() == TransactionStatus.HOLDING) {
            try {
                KisQuoteDTO.CurrentPrice quote =
                        kisApiService.getCurrentPrice(tx.getUserId(), tx.getStockCode());

                if (quote != null) {
                    // ⭐ getCurrentPriceDecimal(): null-safe BigDecimal 변환 (KisQuoteDTO 기존 메서드)
                    BigDecimal currentPrice = quote.getCurrentPriceDecimal();
                    dto.setCurrentPrice(currentPrice);

                    BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(tx.getQuantity()));
                    BigDecimal currentProfitLoss = currentValue.subtract(tx.getTotalAmount());
                    BigDecimal currentProfitLossPct = currentProfitLoss
                            .divide(tx.getTotalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    dto.setCurrentProfitLoss(currentProfitLoss);
                    dto.setCurrentProfitLossPct(currentProfitLossPct);
                }
            } catch (Exception e) {
                log.warn("[주식 현재가 조회 실패] stockCode={}: {}", tx.getStockCode(), e.getMessage());
            }
        }

        return dto;
    }
}