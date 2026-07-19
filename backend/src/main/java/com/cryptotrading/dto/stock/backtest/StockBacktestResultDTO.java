package com.cryptotrading.dto.stock.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 주식/ETF 백테스트 결과 DTO
 * Phase 2 Day 62: Phase 1 BacktestResultDTO(coinSymbol 기준) 재사용 구조
 * - coinSymbols → stockCodes, CoinPerformance → StockPerformance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBacktestResultDTO {

    // 기본 정보
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private List<String> stockCodes;

    // 수익 요약
    private BigDecimal initialBalance;
    private BigDecimal finalBalance;
    private BigDecimal totalProfit;
    private BigDecimal totalProfitRate;
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;

    // 거래 통계
    private int totalTrades;
    private int buyCount;
    private int sellCount;
    private int winCount;
    private int loseCount;
    private BigDecimal winRate;
    private BigDecimal avgProfit;
    private BigDecimal avgLoss;
    private BigDecimal profitFactor;

    // 종목별 성과
    private List<StockPerformance> stockPerformances;

    // 일별 자산 변동 (차트용)
    private List<DailyBalance> dailyBalances;

    // 거래 내역
    private List<BacktestTrade> trades;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockPerformance {
        private String stockCode;
        private int tradeCount;
        private int winCount;
        private int loseCount;
        private BigDecimal totalProfit;
        private BigDecimal profitRate;
        private BigDecimal avgHoldingDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyBalance {
        private LocalDate date;
        private BigDecimal balance;
        private BigDecimal profitRate;
        private BigDecimal holdingValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestTrade {
        private String stockCode;
        private String type;            // BUY, SELL
        private LocalDate tradeDate;
        private BigDecimal price;
        private Integer quantity;       // ⭐ 주식은 정수 수량 (코인은 BigDecimal)
        private BigDecimal amount;
        private BigDecimal profit;
        private BigDecimal profitRate;
        private String signal;
        private Integer holdingDays;    // ⭐ [Day 62 신규] 매도 시 보유 거래일 수
    }
}