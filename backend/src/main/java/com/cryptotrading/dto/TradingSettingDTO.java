package com.cryptotrading.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSettingDTO {

    private Long id;

    @NotEmpty(message = "거래할 코인을 최소 1개 이상 선택해주세요")
    private List<String> coinSymbols;

    @Min(value = 7, message = "기준 기간은 최소 7일 이상이어야 합니다")
    private Integer basePeriod;

    @DecimalMin(value = "-20.00", message = "매수 기준은 -20% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "매수 기준은 0% 이하여야 합니다")
    private BigDecimal buyThresholdPct;

    @DecimalMin(value = "0.00", message = "목표 수익률은 0% 이상이어야 합니다")
    @DecimalMax(value = "50.00", message = "목표 수익률은 50% 이하여야 합니다")
    private BigDecimal sellTargetPct;

    @DecimalMin(value = "-30.00", message = "손절매는 -30% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "손절매는 0% 이하여야 합니다")
    private BigDecimal stopLossPct;

    @Min(value = 1, message = "종목당 최대 보유 건수는 최소 1건 이상이어야 합니다")
    private Integer maxHoldingsPerCoin;

    // ⚠️ DEPRECATED: 일일 한도는 이제 총자산 기준으로 자동 계산됩니다.
    // 프론트엔드 호환성을 위해 필드는 유지하지만, 실제 로직에서는 사용하지 않습니다.
    @Deprecated
    private BigDecimal dailyLimitAmount;

    private Boolean useAiAnalysis;
    private Boolean useTrailingStop;

    @DecimalMin(value = "-20.00", message = "트레일링 스톱은 -20% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "트레일링 스톱은 0% 이하여야 합니다")
    private BigDecimal trailingStopPct;

    // ★★★ RSI 설정 ★★★
    @Min(value = 5, message = "RSI 기간은 최소 5일 이상이어야 합니다")
    @Max(value = 50, message = "RSI 기간은 최대 50일 이하여야 합니다")
    private Integer rsiPeriod;

    @Min(value = 10, message = "RSI 매수 임계값은 최소 10 이상이어야 합니다")
    @Max(value = 50, message = "RSI 매수 임계값은 최대 50 이하여야 합니다")
    private Integer rsiBuyThreshold;

    @Min(value = 50, message = "RSI 매도 임계값은 최소 50 이상이어야 합니다")
    @Max(value = 90, message = "RSI 매도 임계값은 최대 90 이하여야 합니다")
    private Integer rsiSellThreshold;

    // ★★★ 볼린저 밴드 설정 ★★★
    @Min(value = 10, message = "볼린저 밴드 기간은 최소 10일 이상이어야 합니다")
    @Max(value = 50, message = "볼린저 밴드 기간은 최대 50일 이하여야 합니다")
    private Integer bbPeriod;

    @Min(value = 1, message = "표준편차 승수는 최소 1 이상이어야 합니다")
    @Max(value = 4, message = "표준편차 승수는 최대 4 이하여야 합니다")
    private Integer bbMultiplier;

    // ★★★ 거래량 설정 ★★★
    @Min(value = 100, message = "거래량 기준은 최소 100% 이상이어야 합니다")
    @Max(value = 500, message = "거래량 기준은 최대 500% 이하여야 합니다")
    private Integer volumeThreshold;

    // ★★★ 리스크 관리 설정 ★★★
    // 일일 최대 거래금액 (초기 자본 대비 %)
    @Min(value = 10, message = "일일 거래 한도는 최소 10% 이상이어야 합니다")
    @Max(value = 100, message = "일일 거래 한도는 최대 100% 이하여야 합니다")
    private Integer dailyTradeLimitPct;

    // 단일 종목 최대 비중 (총 자본 대비 %)
    @Min(value = 10, message = "종목 비중은 최소 10% 이상이어야 합니다")
    @Max(value = 100, message = "종목 비중은 최대 100% 이하여야 합니다")
    private Integer maxPositionPct;

    // 긴급 정지 조건 - 일일 손실률 (%)
    @Min(value = -50, message = "긴급 정지는 최소 -50% 이상이어야 합니다")
    @Max(value = 0, message = "긴급 정지는 최대 0% 이하여야 합니다")
    private Integer dailyStopLossPct;

    // ⭐⭐⭐ Day 29 추가: 급락장 보호 기능 ⭐⭐⭐
    private Boolean useMarketTrendFilter;

    @Min(value = -50, message = "누적 손실 한도는 -50% 이상이어야 합니다")
    @Max(value = 0, message = "누적 손실 한도는 0% 이하여야 합니다")
    private Integer cumulativeLossLimitPct;

    @Min(value = 1, message = "연속 손절 제한은 1회 이상이어야 합니다")
    @Max(value = 10, message = "연속 손절 제한은 10회 이하여야 합니다")
    private Integer consecutiveStopLossLimit;

    // ⭐⭐⭐ 변경: 1회 고정 매수 금액 (원) ⭐⭐⭐
    // 라운드로빈 OFF 시 각 코인에 매수할 금액
    @DecimalMin(value = "5000.00", message = "1회 매수 금액은 최소 5,000원 이상이어야 합니다 (업비트 최소 주문금액)")
    @DecimalMax(value = "10000000.00", message = "1회 매수 금액은 최대 1,000만원 이하여야 합니다")
    private BigDecimal fixedBuyAmount;

    // 일일 한도 복구 옵션
    private Boolean useDailyLimitRecovery;

    // ⭐⭐⭐ 변경: 매수 방식 선택 (라운드로빈 vs 고정금액) ⭐⭐⭐
    // true: 라운드로빈 방식 (일일 한도 균등 분배)
    // false: 고정 금액 방식 (fixedBuyAmount 사용)
    private Boolean useRoundRobin;

    @DecimalMin(value = "0.00", message = "추가 매수 하락률은 0% 이상이어야 합니다")
    @DecimalMax(value = "10.00", message = "추가 매수 하락률은 10% 이하여야 합니다")
    private BigDecimal additionalDropPct;

    private Boolean useStopLoss;
}