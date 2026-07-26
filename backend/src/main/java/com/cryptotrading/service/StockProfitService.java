package com.cryptotrading.service;

import com.cryptotrading.dto.profit.StockPeriodProfitDTO;
import com.cryptotrading.dto.profit.StockProfitDTO;
import com.cryptotrading.dto.profit.StockProfitSummaryDTO;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockInfoRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import com.cryptotrading.repository.StockTradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 주식 수익 분석 서비스
 * Day 63: Phase 1 ProfitService(Day 31) 1:1 재사용 구조
 * - coinSymbol → stockCode, CoinInfoRepository → StockInfoRepository
 * - Transaction/TradingSetting → StockTransaction/StockTradingSetting
 * - 계산 로직/반올림 방식은 Phase 1과 완전히 동일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockProfitService {

    private final StockTransactionRepository stockTransactionRepository;
    private final StockTradingSettingRepository stockTradingSettingRepository;
    private final StockInfoRepository stockInfoRepository;

    /**
     * 전체 기간별 수익 요약 조회
     */
    public StockProfitSummaryDTO getProfitSummary(String userId) {
        log.info("[주식] 수익 요약 조회 - userId: {}", userId);

        BigDecimal initialInvestment = getInitialInvestment(userId);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate oneYearAgo = today.minusYears(1);

        StockPeriodProfitDTO todayProfit = calculatePeriodProfit(userId, today, today);
        StockPeriodProfitDTO monthProfit = calculatePeriodProfit(userId, monthStart, today);
        StockPeriodProfitDTO yearProfit = calculatePeriodProfit(userId, yearStart, today);
        StockPeriodProfitDTO oneYearProfit = calculatePeriodProfit(userId, oneYearAgo, today);
        StockPeriodProfitDTO totalProfit = calculateTotalProfit(userId);

        return StockProfitSummaryDTO.builder()
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
    public StockPeriodProfitDTO getPeriodProfit(String userId, String period) {
        log.info("[주식] 기간별 수익 조회 - userId: {}, period: {}", userId, period);

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
     * 사용자 지정 기간 수익 상세 조회
     * ⭐ [Day 63 신규] Phase 1에는 없던 사용자 지정 기간 API (StockHoldingsView 조회 버튼용)
     */
    public StockPeriodProfitDTO getPeriodProfitByRange(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("[주식] 사용자 지정 기간 수익 조회 - userId: {}, {} ~ {}", userId, startDate, endDate);
        return calculatePeriodProfit(userId, startDate, endDate);
    }

    /**
     * 종목별 수익 분석 조회
     */
    public List<StockProfitDTO> getStockProfits(String userId) {
        log.info("[주식] 종목별 수익 조회 - userId: {}", userId);

        List<StockTransaction> soldTransactions = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);

        List<StockTransaction> holdingTransactions = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);

        Map<String, List<StockTransaction>> soldByCode = soldTransactions.stream()
                .collect(Collectors.groupingBy(StockTransaction::getStockCode));

        Map<String, List<StockTransaction>> holdingByCode = holdingTransactions.stream()
                .collect(Collectors.groupingBy(StockTransaction::getStockCode));

        Set<String> allCodes = new HashSet<>();
        allCodes.addAll(soldByCode.keySet());
        allCodes.addAll(holdingByCode.keySet());

        List<StockProfitDTO> stockProfits = new ArrayList<>();

        for (String code : allCodes) {
            List<StockTransaction> sold = soldByCode.getOrDefault(code, Collections.emptyList());
            List<StockTransaction> holding = holdingByCode.getOrDefault(code, Collections.emptyList());

            stockProfits.add(calculateStockProfit(code, sold, holding));
        }

        stockProfits.sort((a, b) -> b.getTotalProfit().compareTo(a.getTotalProfit()));

        return stockProfits;
    }

    /**
     * 기간별 수익 계산 (내부 메서드) - Phase 1 calculatePeriodProfit과 동일 로직
     */
    private StockPeriodProfitDTO calculatePeriodProfit(String userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<StockTransaction> transactions = stockTransactionRepository
                .findSoldTransactionsByDateRange(userId, startDateTime, endDateTime);

        if (transactions.isEmpty()) {
            return StockPeriodProfitDTO.builder()
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

        BigDecimal totalProfit = sumProfitLoss(transactions);

        int winCount = countByProfitSign(transactions, true);
        int loseCount = countByProfitSign(transactions, false);

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

        List<StockPeriodProfitDTO.DailyProfitDTO> dailyProfits = dailyMap.entrySet().stream()
                .map(e -> StockPeriodProfitDTO.DailyProfitDTO.builder()
                        .date(e.getKey())
                        .profit(e.getValue())
                        .tradeCount(dailyCountMap.get(e.getKey()).intValue())
                        .build())
                .sorted(Comparator.comparing(StockPeriodProfitDTO.DailyProfitDTO::getDate))
                .collect(Collectors.toList());

        BigDecimal initialInvestment = getInitialInvestment(userId);
        Double profitPct = calculateProfitPct(totalProfit, initialInvestment);

        return StockPeriodProfitDTO.builder()
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
     * 누적 총 수익 계산 (내부 메서드) - Phase 1 calculateTotalProfit과 동일 로직
     */
    private StockPeriodProfitDTO calculateTotalProfit(String userId) {
        List<StockTransaction> transactions = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);

        if (transactions.isEmpty()) {
            return StockPeriodProfitDTO.builder()
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

        BigDecimal totalProfit = sumProfitLoss(transactions);

        int winCount = countByProfitSign(transactions, true);
        int loseCount = countByProfitSign(transactions, false);

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

        BigDecimal initialInvestment = getInitialInvestment(userId);
        Double profitPct = calculateProfitPct(totalProfit, initialInvestment);

        LocalDate firstTradeDate = transactions.stream()
                .map(t -> t.getSoldAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        return StockPeriodProfitDTO.builder()
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
     * 종목별 수익 계산 (내부 메서드) - Phase 1 calculateCoinProfit과 동일 로직
     */
    private StockProfitDTO calculateStockProfit(String stockCode, List<StockTransaction> sold, List<StockTransaction> holding) {
        String stockName = stockInfoRepository.findById(stockCode)
                .map(com.cryptotrading.entity.StockInfo::getStockName)
                .orElse(stockCode);

        BigDecimal totalProfit = sumProfitLoss(sold);

        BigDecimal totalBuyAmount = sold.stream()
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = sold.stream()
                .map(t -> {
                    if (t.getSoldPrice() != null && t.getQuantity() != null) {
                        return t.getSoldPrice().multiply(BigDecimal.valueOf(t.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int winCount = countByProfitSign(sold, true);
        int loseCount = countByProfitSign(sold, false);

        BigDecimal maxProfit = sold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxLoss = sold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgBuyPrice = BigDecimal.ZERO;
        BigDecimal avgSellPrice = BigDecimal.ZERO;

        if (!sold.isEmpty()) {
            BigDecimal totalQuantitySold = sold.stream()
                    .map(t -> t.getQuantity() != null ? BigDecimal.valueOf(t.getQuantity()) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalQuantitySold.compareTo(BigDecimal.ZERO) > 0) {
                avgBuyPrice = totalBuyAmount.divide(totalQuantitySold, 0, RoundingMode.HALF_UP);
                avgSellPrice = totalSellAmount.divide(totalQuantitySold, 0, RoundingMode.HALF_UP);
            }
        }

        int currentHoldingCount = holding.size();
        BigDecimal currentHoldingAmount = holding.stream()
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 미실현 손익 - Phase 1과 동일하게 0으로 두고 프론트에서 현재가 반영하여 계산
        BigDecimal unrealizedProfit = BigDecimal.ZERO;

        LocalDateTime lastTradeAt = sold.stream()
                .map(StockTransaction::getSoldAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        int totalTradeCount = sold.size();
        double winRate = totalTradeCount > 0 ? (double) winCount / totalTradeCount * 100 : 0.0;
        double profitPct = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.doubleValue() / totalBuyAmount.doubleValue() * 100
                : 0.0;

        return StockProfitDTO.builder()
                .stockCode(stockCode)
                .stockName(stockName)
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

    private BigDecimal sumProfitLoss(List<StockTransaction> transactions) {
        return transactions.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countByProfitSign(List<StockTransaction> transactions, boolean positive) {
        return (int) transactions.stream()
                .filter(t -> t.getProfitLoss() != null &&
                        (positive ? t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0
                                  : t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0))
                .count();
    }

    /**
     * 초기 투자금 조회 (stock_trading_settings.dailyLimitAmount)
     */
    private BigDecimal getInitialInvestment(String userId) {
        return stockTradingSettingRepository.findByUserId(userId)
                .map(StockTradingSetting::getDailyLimitAmount)
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