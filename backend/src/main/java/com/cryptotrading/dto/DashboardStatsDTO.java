package com.cryptotrading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // 보유 자산 정보
    private BigDecimal totalHoldingAmount;      // 총 투자금액
    private BigDecimal totalCurrentValue;       // 현재 평가액
    private BigDecimal totalProfitLoss;         // 총 평가손익
    private BigDecimal totalProfitLossPct;      // 총 수익률

    // 실현 손익 (매도 완료)
    private BigDecimal realizedProfitLoss;      // 실현 손익
    private int soldCount;                      // 매도 완료 건수

    // 거래 통계
    private long totalBuyCount;                 // 총 매수 건수
    private long totalSellCount;                // 총 매도 건수
    private int currentHoldingCount;            // 현재 보유 건수

    // 일일 거래 정보
    private BigDecimal todayBuyAmount;          // 오늘 매수 금액
    private BigDecimal todaySellAmount;         // 오늘 매도 금액
    private int todayBuyCount;                  // 오늘 매수 건수
    private int todaySellCount;                 // 오늘 매도 건수

    // 거래 한도 정보
    private BigDecimal dailyLimitAmount;        // 일일 거래 한도
    private BigDecimal remainingDailyLimit;     // 남은 일일 한도
}