package com.cryptotrading.service;

import com.cryptotrading.dto.profit.CoinProfitDTO;
import com.cryptotrading.dto.profit.PeriodProfitDTO;
import com.cryptotrading.dto.profit.ProfitSummaryDTO;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.repository.CoinInfoRepository;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 수익 분석 서비스
 * Day 31: 기간별/코인별 수익 분석 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitService {

    private final TransactionRepository transactionRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final CoinInfoRepository coinInfoRepository;

    /**
     * 전체 기간별 수익 요약 조회
     */
    public ProfitSummaryDTO getProfitSummary(String userId) {
        log.info("수익 요약 조회 - userId: {}", userId);
        
        // 초기 투자금 조회 (거래 설정의 dailyLimitAmount 사용)
        BigDecimal initialInvestment = getInitialInvestment(userId);
        
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate oneYearAgo = today.minusYears(1);
        
        // 오늘 수익
        PeriodProfitDTO todayProfit = calculatePeriodProfit(userId, today, today);
        
        // 이번달 수익
        PeriodProfitDTO monthProfit = calculatePeriodProfit(userId, monthStart, today);
        
        // 올해 수익
        PeriodProfitDTO yearProfit = calculatePeriodProfit(userId, yearStart, today);
        
        // 1년간 수익
        PeriodProfitDTO oneYearProfit = calculatePeriodProfit(userId, oneYearAgo, today);
        
        // 누적 총 수익
        PeriodProfitDTO totalProfit = calculateTotalProfit(userId);
        
        return ProfitSummaryDTO.builder()
                .todayProfit(todayProfit.getTotalProfit())
                .todayProfitPct(calculateProfitPct(todayProfit.getTotalProfit(), initialInvestment))
                .todayTradeCount(todayProfit.getTradeCount())
                .monthProfit(monthProfit.getTotalProfit())
                .monthProfitPct(calculateProfitPct(monthProfit.getTotalProfit(), initialInvestment))
                .monthTradeCount(monthProfit.getTradeCount())
                .yearProfit(yearProfit.getTotalProfit())
                .yearProfitPct(calculateProfitPct(yearProfit.getTotalProfit(), initialInvestment))
                .yearTradeCount(yearProfit.getTradeCount())
                .oneYearProfit(oneYearProfit.getTotalProfit())
                .oneYearProfitPct(calculateProfitPct(oneYearProfit.getTotalProfit(), initialInvestment))
                .oneYearTradeCount(oneYearProfit.getTradeCount())
                .totalProfit(totalProfit.getTotalProfit())
                .totalProfitPct(calculateProfitPct(totalProfit.getTotalProfit(), initialInvestment))
                .totalTradeCount(totalProfit.getTradeCount())
                .initialInvestment(initialInvestment)
                .build();
    }

    /**
     * 특정 기간 수익 상세 조회
     */
    public PeriodProfitDTO getPeriodProfit(String userId, String period) {
        log.info("기간별 수익 조회 - userId: {}, period: {}", userId, period);
        
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;
        
        switch (period.toLowerCase()) {
            case "today":
                startDate = today;
                break;
            case "month":
                startDate = today.withDayOfMonth(1);
                break;
            case "year":
                startDate = today.withDayOfYear(1);
                break;
            case "oneyear":
                startDate = today.minusYears(1);
                break;
            case "total":
                return calculateTotalProfit(userId);
            default:
                startDate = today.minusDays(30);
        }
        
        return calculatePeriodProfit(userId, startDate, endDate);
    }

    /**
     * 코인별 수익 분석 조회
     */
    public List<CoinProfitDTO> getCoinProfits(String userId) {
        log.info("코인별 수익 조회 - userId: {}", userId);
        
        // 매도 완료된 거래 조회
        List<Transaction> soldTransactions = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);
        
        // 보유 중인 거래 조회
        List<Transaction> holdingTransactions = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        
        // 코인별로 그룹화
        Map<String, List<Transaction>> soldBySymbol = soldTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCoinSymbol));
        
        Map<String, List<Transaction>> holdingBySymbol = holdingTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCoinSymbol));
        
        // 모든 거래된 코인 심볼 수집
        Set<String> allSymbols = new HashSet<>();
        allSymbols.addAll(soldBySymbol.keySet());
        allSymbols.addAll(holdingBySymbol.keySet());
        
        // 코인별 수익 계산
        List<CoinProfitDTO> coinProfits = new ArrayList<>();
        
        for (String symbol : allSymbols) {
            List<Transaction> sold = soldBySymbol.getOrDefault(symbol, Collections.emptyList());
            List<Transaction> holding = holdingBySymbol.getOrDefault(symbol, Collections.emptyList());
            
            CoinProfitDTO dto = calculateCoinProfit(symbol, sold, holding);
            coinProfits.add(dto);
        }
        
        // 총 수익 기준 내림차순 정렬
        coinProfits.sort((a, b) -> b.getTotalProfit().compareTo(a.getTotalProfit()));
        
        return coinProfits;
    }

    /**
     * 기간별 수익 계산 (내부 메서드)
     */
    private PeriodProfitDTO calculatePeriodProfit(String userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // 해당 기간에 매도된 거래 조회
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndSoldAtBetween(userId, startDateTime, endDateTime);
        
        if (transactions.isEmpty()) {
            return PeriodProfitDTO.builder()
                    .period(startDate + " ~ " + endDate)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalProfit(BigDecimal.ZERO)
                    .profitPct(0.0)
                    .tradeCount(0)
                    .winCount(0)
                    .loseCount(0)
                    .winRate(0.0)
                    .avgProfit(BigDecimal.ZERO)
                    .maxProfit(BigDecimal.ZERO)
                    .maxLoss(BigDecimal.ZERO)
                    .dailyProfits(Collections.emptyList())
                    .build();
        }
        
        // 통계 계산
        BigDecimal totalProfit = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int winCount = (int) transactions.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        int loseCount = (int) transactions.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
                .count();
        
        BigDecimal maxProfit = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxLoss = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        int tradeCount = transactions.size();
        BigDecimal avgProfit = tradeCount > 0 
                ? totalProfit.divide(BigDecimal.valueOf(tradeCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        double winRate = tradeCount > 0 ? (double) winCount / tradeCount * 100 : 0.0;
        
        // 일별 수익 추이
        Map<LocalDate, BigDecimal> dailyMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getSoldAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, 
                                t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO, 
                                BigDecimal::add)
                ));
        
        Map<LocalDate, Long> dailyCountMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getSoldAt().toLocalDate(),
                        Collectors.counting()
                ));
        
        List<PeriodProfitDTO.DailyProfitDTO> dailyProfits = dailyMap.entrySet().stream()
                .map(e -> PeriodProfitDTO.DailyProfitDTO.builder()
                        .date(e.getKey())
                        .profit(e.getValue())
                        .tradeCount(dailyCountMap.get(e.getKey()).intValue())
                        .build())
                .sorted(Comparator.comparing(PeriodProfitDTO.DailyProfitDTO::getDate))
                .collect(Collectors.toList());
        
        // 수익률 계산
        BigDecimal initialInvestment = getInitialInvestment(userId);
        Double profitPct = calculateProfitPct(totalProfit, initialInvestment);
        
        return PeriodProfitDTO.builder()
                .period(startDate + " ~ " + endDate)
                .startDate(startDate)
                .endDate(endDate)
                .totalProfit(totalProfit)
                .profitPct(profitPct)
                .tradeCount(tradeCount)
                .winCount(winCount)
                .loseCount(loseCount)
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .avgProfit(avgProfit)
                .maxProfit(maxProfit)
                .maxLoss(maxLoss)
                .dailyProfits(dailyProfits)
                .build();
    }

    /**
     * 누적 총 수익 계산
     */
    private PeriodProfitDTO calculateTotalProfit(String userId) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);
        
        if (transactions.isEmpty()) {
            return PeriodProfitDTO.builder()
                    .period("전체")
                    .totalProfit(BigDecimal.ZERO)
                    .profitPct(0.0)
                    .tradeCount(0)
                    .winCount(0)
                    .loseCount(0)
                    .winRate(0.0)
                    .avgProfit(BigDecimal.ZERO)
                    .maxProfit(BigDecimal.ZERO)
                    .maxLoss(BigDecimal.ZERO)
                    .dailyProfits(Collections.emptyList())
                    .build();
        }
        
        // 통계 계산
        BigDecimal totalProfit = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int winCount = (int) transactions.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        int loseCount = (int) transactions.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
                .count();
        
        BigDecimal maxProfit = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxLoss = transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        int tradeCount = transactions.size();
        BigDecimal avgProfit = tradeCount > 0 
                ? totalProfit.divide(BigDecimal.valueOf(tradeCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        double winRate = tradeCount > 0 ? (double) winCount / tradeCount * 100 : 0.0;
        
        // 수익률 계산
        BigDecimal initialInvestment = getInitialInvestment(userId);
        Double profitPct = calculateProfitPct(totalProfit, initialInvestment);
        
        // 기간 계산
        LocalDate firstTradeDate = transactions.stream()
                .map(t -> t.getSoldAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        
        return PeriodProfitDTO.builder()
                .period("전체")
                .startDate(firstTradeDate)
                .endDate(LocalDate.now())
                .totalProfit(totalProfit)
                .profitPct(profitPct)
                .tradeCount(tradeCount)
                .winCount(winCount)
                .loseCount(loseCount)
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .avgProfit(avgProfit)
                .maxProfit(maxProfit)
                .maxLoss(maxLoss)
                .dailyProfits(Collections.emptyList())
                .build();
    }

    /**
     * 코인별 수익 계산 (내부 메서드)
     */
    private CoinProfitDTO calculateCoinProfit(String symbol, List<Transaction> sold, List<Transaction> holding) {
        // 코인 한글명 조회
        String coinName = coinInfoRepository.findBySymbol(symbol)
                .map(coin -> coin.getNameKr())
                .orElse(symbol.replace("KRW-", ""));
        
        // 매도 거래 통계
        BigDecimal totalProfit = sold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalBuyAmount = sold.stream()
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSellAmount = sold.stream()
                .map(t -> {
                    if (t.getSoldPrice() != null && t.getQuantity() != null) {
                        return t.getSoldPrice().multiply(t.getQuantity());
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int winCount = (int) sold.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        int loseCount = (int) sold.stream()
                .filter(t -> t.getProfitLoss() != null && t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
                .count();
        
        BigDecimal maxProfit = sold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxLoss = sold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        // 평균가 계산
        BigDecimal avgBuyPrice = BigDecimal.ZERO;
        BigDecimal avgSellPrice = BigDecimal.ZERO;
        
        if (!sold.isEmpty()) {
            BigDecimal totalQuantitySold = sold.stream()
                    .map(t -> t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalQuantitySold.compareTo(BigDecimal.ZERO) > 0) {
                avgBuyPrice = totalBuyAmount.divide(totalQuantitySold, 0, RoundingMode.HALF_UP);
                avgSellPrice = totalSellAmount.divide(totalQuantitySold, 0, RoundingMode.HALF_UP);
            }
        }
        
        // 보유 중 정보
        int currentHoldingCount = holding.size();
        BigDecimal currentHoldingAmount = holding.stream()
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 미실현 손익 (현재가 필요 - 여기서는 0으로 설정, 프론트엔드에서 계산)
        BigDecimal unrealizedProfit = BigDecimal.ZERO;
        
        // 최근 거래 시간
        LocalDateTime lastTradeAt = sold.stream()
                .map(Transaction::getSoldAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        int totalTradeCount = sold.size();
        double winRate = totalTradeCount > 0 ? (double) winCount / totalTradeCount * 100 : 0.0;
        double profitPct = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0 
                ? totalProfit.doubleValue() / totalBuyAmount.doubleValue() * 100 
                : 0.0;
        
        return CoinProfitDTO.builder()
                .coinSymbol(symbol)
                .coinName(coinName)
                .totalProfit(totalProfit)
                .profitPct(Math.round(profitPct * 100.0) / 100.0)
                .totalTradeCount(totalTradeCount)
                .winCount(winCount)
                .loseCount(loseCount)
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .totalBuyAmount(totalBuyAmount)
                .totalSellAmount(totalSellAmount)
                .avgBuyPrice(avgBuyPrice)
                .avgSellPrice(avgSellPrice)
                .maxProfit(maxProfit)
                .maxLoss(maxLoss)
                .currentHoldingCount(currentHoldingCount)
                .currentHoldingAmount(currentHoldingAmount)
                .unrealizedProfit(unrealizedProfit)
                .lastTradeAt(lastTradeAt)
                .build();
    }

    /**
     * 초기 투자금 조회
     */
    private BigDecimal getInitialInvestment(String userId) {
        return tradingSettingRepository.findByUserId(userId)
                .map(TradingSetting::getDailyLimitAmount)
                .orElse(BigDecimal.valueOf(1000000)); // 기본값 100만원
    }

    /**
     * 수익률 계산
     */
    private Double calculateProfitPct(BigDecimal profit, BigDecimal investment) {
        if (investment == null || investment.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        double pct = profit.doubleValue() / investment.doubleValue() * 100;
        return Math.round(pct * 100.0) / 100.0;
    }
}