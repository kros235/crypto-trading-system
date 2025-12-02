package com.cryptotrading.dto.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResultDTO {
    
    // 기본 정보
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private List<String> coinSymbols;
    
    // 수익 요약
    private BigDecimal initialBalance;
    private BigDecimal finalBalance;
    private BigDecimal totalProfit;
    private BigDecimal totalProfitRate;
    private BigDecimal maxDrawdown;           // 최대 낙폭
    private BigDecimal sharpeRatio;           // 샤프 비율 (위험 대비 수익)
    
    // 거래 통계
    private int totalTrades;
    private int buyCount;
    private int sellCount;
    private int winCount;
    private int loseCount;
    private BigDecimal winRate;               // 승률
    private BigDecimal avgProfit;             // 평균 수익
    private BigDecimal avgLoss;               // 평균 손실
    private BigDecimal profitFactor;          // 총 이익 / 총 손실
    
    // 코인별 성과
    private List<CoinPerformance> coinPerformances;
    
    // 일별 자산 변동 (차트용)
    private List<DailyBalance> dailyBalances;
    
    // 거래 내역
    private List<BacktestTrade> trades;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoinPerformance {
        private String coinSymbol;
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
        private String coinSymbol;
        private String type;            // BUY, SELL
        private LocalDate tradeDate;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal amount;
        private BigDecimal profit;      // 매도 시에만
        private BigDecimal profitRate;  // 매도 시에만
        private String signal;          // 매매 신호 사유
    }
}