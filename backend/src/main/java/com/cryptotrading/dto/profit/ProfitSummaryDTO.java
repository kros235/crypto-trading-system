package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 기간별 수익 요약 DTO
 * Day 31: 기간별/코인별 수익 분석 기능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSummaryDTO {
    
    // 오늘 수익
    private BigDecimal todayProfit;
    private Double todayProfitPct;
    private Integer todayTradeCount;
    
    // 이번달 수익
    private BigDecimal monthProfit;
    private Double monthProfitPct;
    private Integer monthTradeCount;
    
    // 올해 수익
    private BigDecimal yearProfit;
    private Double yearProfitPct;
    private Integer yearTradeCount;
    
    // 1년간 수익
    private BigDecimal oneYearProfit;
    private Double oneYearProfitPct;
    private Integer oneYearTradeCount;
    
    // 누적 총 수익
    private BigDecimal totalProfit;
    private Double totalProfitPct;
    private Integer totalTradeCount;
    
    // 초기 투자금 (수익률 계산 기준)
    private BigDecimal initialInvestment;
}