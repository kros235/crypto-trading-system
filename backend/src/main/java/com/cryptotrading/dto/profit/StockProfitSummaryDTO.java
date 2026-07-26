package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주식 기간별 수익 요약 DTO
 * Day 63: Phase 1 ProfitSummaryDTO 1:1 재사용 구조 (코인 → 주식)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProfitSummaryDTO {

    private BigDecimal todayProfit;
    private Double todayProfitPct;
    private Integer todayTradeCount;

    private BigDecimal monthProfit;
    private Double monthProfitPct;
    private Integer monthTradeCount;

    private BigDecimal yearProfit;
    private Double yearProfitPct;
    private Integer yearTradeCount;

    private BigDecimal oneYearProfit;
    private Double oneYearProfitPct;
    private Integer oneYearTradeCount;

    private BigDecimal totalProfit;
    private Double totalProfitPct;
    private Integer totalTradeCount;

    // 초기 투자금 (수익률 계산 기준, stock_trading_settings.dailyLimitAmount)
    private BigDecimal initialInvestment;
}