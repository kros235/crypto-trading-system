package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 특정 기간 수익 상세 DTO
 * Day 31: 기간별/코인별 수익 분석 기능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodProfitDTO {
    
    private String period;           // 기간 구분 (today, month, year, oneYear, total)
    private LocalDate startDate;     // 시작일
    private LocalDate endDate;       // 종료일
    
    private BigDecimal totalProfit;  // 해당 기간 총 수익
    private Double profitPct;        // 수익률
    private Integer tradeCount;      // 거래 건수
    private Integer winCount;        // 익절 건수
    private Integer loseCount;       // 손절 건수
    private Double winRate;          // 승률
    
    private BigDecimal avgProfit;    // 건당 평균 수익
    private BigDecimal maxProfit;    // 최대 수익 거래
    private BigDecimal maxLoss;      // 최대 손실 거래
    
    // 일별 수익 추이 (차트용)
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