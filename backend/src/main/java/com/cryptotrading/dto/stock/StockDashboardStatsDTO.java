package com.cryptotrading.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주식 대시보드 통계 DTO
 * Day 59: Phase 1 DashboardStatsDTO 패턴 재사용, 주식 전용 필드 추가
 *
 * 차이점:
 *  - quantity: Integer (코인은 BigDecimal)
 *  - holdingDaysWarningCount: 레버리지 ETF 보유기간 경고 건수
 *  - exchangeRate: 현재 USD/KRW 환율 (환노출형 ETF용)
 *  - exchangeRateChangePct: 전일 대비 환율 변동률 (%)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDashboardStatsDTO {

    // ── 보유 자산 정보 ──────────────────────────────
    private BigDecimal totalHoldingAmount;      // 총 투자금액 (매수가 기준)
    private BigDecimal totalCurrentValue;       // 현재 평가액
    private BigDecimal totalProfitLoss;         // 평가 손익
    private BigDecimal totalProfitLossPct;      // 평가 수익률 (%)

    // ── 실현 손익 ───────────────────────────────────
    private BigDecimal realizedProfitLoss;      // 오늘 실현 손익
    private int soldCount;                      // 오늘 매도 건수

    // ── 거래 통계 ───────────────────────────────────
    private long totalBuyCount;                 // 총 매수 건수
    private long totalSellCount;                // 총 매도 건수
    private int currentHoldingCount;            // 현재 보유 건수

    // ── 일일 거래 정보 ──────────────────────────────
    private BigDecimal todayBuyAmount;          // 오늘 매수 금액
    private BigDecimal todaySellAmount;         // 오늘 매도 금액
    private int todayBuyCount;                  // 오늘 매수 건수
    private int todaySellCount;                 // 오늘 매도 건수

    // ── 거래 한도 ───────────────────────────────────
    private BigDecimal dailyLimitAmount;        // 일일 거래 한도
    private BigDecimal remainingDailyLimit;     // 남은 일일 한도

    // ── 주식 전용 필드 ─────────────────────────────
    /** 레버리지 ETF 보유기간 경고 건수 (15일 이상) */
    private int holdingDaysWarningCount;

    /** 레버리지 ETF 보유기간 긴급경고 건수 (20일 이상) */
    private int holdingDaysUrgentCount;

    /** 현재 봇 활성화 여부 */
    private boolean botEnabled;

    /** 현재 장 운영 여부 */
    private boolean marketOpen;

    // ── 환율 정보 (환노출형 ETF용) ──────────────────
    /** 현재 USD/KRW 환율 */
    private BigDecimal exchangeRate;

    /** 전일 대비 환율 변동액 */
    private BigDecimal exchangeRateChange;

    /** 전일 대비 환율 변동률 (%) */
    private BigDecimal exchangeRateChangePct;
}