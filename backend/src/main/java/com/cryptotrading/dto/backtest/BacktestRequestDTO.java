package com.cryptotrading.dto.backtest;

import jakarta.validation.constraints.*;
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
public class BacktestRequestDTO {

    @NotEmpty(message = "거래할 코인을 선택해주세요")
    private List<String> coinSymbols;

    @NotNull(message = "시작일을 입력해주세요")
    private LocalDate startDate;

    @NotNull(message = "종료일을 입력해주세요")
    private LocalDate endDate;

    @NotNull(message = "초기 투자금을 입력해주세요")
    @DecimalMin(value = "100000", message = "최소 10만원 이상 입력해주세요")
    private BigDecimal initialBalance;

    // 거래 설정 (선택 - 없으면 기본값 사용)
    @Min(value = 7, message = "이동평균선 기간은 7일 이상")
    @Max(value = 30, message = "이동평균선 기간은 30일 이하")
    @Builder.Default
    private Integer basePeriod = 20;

    @DecimalMax(value = "0", message = "매수 기준은 0 이하")
    @DecimalMin(value = "-20", message = "매수 기준은 -20% 이상")
    @Builder.Default
    private BigDecimal buyThresholdPct = new BigDecimal("-3");

    @DecimalMin(value = "0.5", message = "목표 수익률은 0.5% 이상")
    @DecimalMax(value = "50", message = "목표 수익률은 50% 이하")
    @Builder.Default
    private BigDecimal sellTargetPct = new BigDecimal("3");

    @DecimalMax(value = "0", message = "손절매 기준은 0 이하")
    @DecimalMin(value = "-30", message = "손절매 기준은 -30% 이상")
    @Builder.Default
    private BigDecimal stopLossPct = new BigDecimal("-5");

    @Min(value = 1, message = "최소 1건 이상")
    @Max(value = 10, message = "최대 10건 이하")
    @Builder.Default
    private Integer maxHoldingsPerCoin = 3;

    @Builder.Default
    private Boolean useTrailingStop = false;

    @DecimalMin(value = "1", message = "트레일링 스톱은 1% 이상")
    @DecimalMax(value = "10", message = "트레일링 스톱은 10% 이하")
    @Builder.Default
    private BigDecimal trailingStopPct = new BigDecimal("5");

    @Min(value = 5, message = "RSI 기간은 5일 이상")
    @Max(value = 50, message = "RSI 기간은 50일 이하")
    @Builder.Default
    private Integer rsiPeriod = 14;

    @Min(value = 10, message = "RSI 매수 임계값은 10 이상")
    @Max(value = 50, message = "RSI 매수 임계값은 50 이하")
    @Builder.Default
    private Integer rsiBuyThreshold = 30;

    @Min(value = 50, message = "RSI 매도 임계값은 50 이상")
    @Max(value = 90, message = "RSI 매도 임계값은 90 이하")
    @Builder.Default
    private Integer rsiSellThreshold = 70;

    // 볼린저 밴드 설정
    @Min(value = 10, message = "볼린저 밴드 기간은 10일 이상")
    @Max(value = 50, message = "볼린저 밴드 기간은 50일 이하")
    @Builder.Default
    private Integer bbPeriod = 20;

    @Min(value = 1, message = "표준편차 승수는 1 이상")
    @Max(value = 4, message = "표준편차 승수는 4 이하")
    @Builder.Default
    private Integer bbMultiplier = 2;

    // 거래량 설정
    @Min(value = 100, message = "거래량 기준은 100% 이상")
    @Max(value = 500, message = "거래량 기준은 500% 이하")
    @Builder.Default
    private Integer volumeThreshold = 150;

    // 리스크 관리 설정

    // 일일 최대 거래금액 (초기 자본 대비 %)
    @Min(value = 10, message = "일일 거래 한도는 10% 이상")
    @Max(value = 100, message = "일일 거래 한도는 100% 이하")
    @Builder.Default
    private Integer dailyTradeLimitPct = 100; // 기본값: 제한 없음 (100%)

    // 단일 종목 최대 비중 (총 자본 대비 %)
    @Min(value = 10, message = "종목 비중은 10% 이상")
    @Max(value = 100, message = "종목 비중은 100% 이하")
    @Builder.Default
    private Integer maxPositionPct = 100; // 기본값: 제한 없음 (100%)

    // 긴급 정지 조건 - 일일 손실률 (%)
    @Min(value = -50, message = "긴급 정지는 -50% 이상")
    @Max(value = 0, message = "긴급 정지는 0% 이하")
    @Builder.Default
    private Integer dailyStopLossPct = -100; // 기본값: 사용 안함 (-100%)

    // 급락장 보호 기능
    private Boolean useMarketTrendFilter = false; // 시장 추세 필터 (BTC MA20)
    private Integer cumulativeLossLimitPct = -10; // 누적 손실 한도 (%)
    private Integer consecutiveStopLossLimit = 3; // 연속 손절 제한 횟수

    // ⭐⭐⭐ 수정: 1회 매수 비율(%) → 1회 고정 매수 금액(원) ⭐⭐⭐
    // 라운드로빈 OFF 시 각 코인에 이 금액만큼 매수
    // 최소 5,000원 이상 (업비트 최소 주문금액)
    @DecimalMin(value = "5000", message = "1회 매수 금액은 최소 5,000원 이상")
    @DecimalMax(value = "10000000", message = "1회 매수 금액은 최대 1,000만원 이하")
    @Builder.Default
    private BigDecimal fixedBuyAmount = new BigDecimal("10000");

    // 일일 한도 복구 옵션
    @Builder.Default
    private Boolean useDailyLimitRecovery = false;

    // ⭐⭐⭐ 수정: 매수 방식 선택 (라운드로빈 vs 고정금액) ⭐⭐⭐
    // true: 라운드로빈 (일일 한도를 매수 신호 수로 균등 분배)
    // false: 고정 금액 (fixedBuyAmount만큼 각 코인에 매수)
    @Builder.Default
    private Boolean useRoundRobin = true;

    @DecimalMin(value = "0.0", message = "추가 매수 하락률은 0% 이상")
    @DecimalMax(value = "10.0", message = "추가 매수 하락률은 10% 이하")
    @Builder.Default
    private BigDecimal additionalDropPct = new BigDecimal("0.5");

    @Builder.Default
    private Boolean useStopLoss = true;
}