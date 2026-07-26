package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 주식 특정 기간 수익 상세 DTO
 * Day 63: Phase 1 PeriodProfitDTO 1:1 재사용 구조 (코인 → 주식)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPeriodProfitDTO {

    private String period;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalProfit;
    private Double profitPct;
    private Integer tradeCount;
    private Integer winCount;
    private Integer loseCount;
    private Double winRate;

    private BigDecimal avgProfit;
    private BigDecimal maxProfit;
    private BigDecimal maxLoss;

    private List<DailyProfitDTO> dailyProfits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyProfitDTO {
        private LocalDate date;
        private BigDecimal profit;
        private Integer tradeCount;
    }
}