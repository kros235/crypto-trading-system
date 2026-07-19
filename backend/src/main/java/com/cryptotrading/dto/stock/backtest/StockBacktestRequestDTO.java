package com.cryptotrading.dto.stock.backtest;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 주식/ETF 백테스트 요청 DTO
 * Phase 2 Day 62: StockBacktestService + StockBacktestView
 *
 * ⭐⭐⭐ [Day 62 신규] Phase 1 BacktestRequestDTO(coinSymbols 기준) 재사용 구조 ⭐⭐⭐
 * - coinSymbols → stockCodes
 * - 코인 대비 변동성이 낮은 레버리지 ETF 특성 반영: 기본값 조정 (backtest_master_plan.md 기준)
 * - maxHoldingDays 필드 신규 추가 (레버리지 ETF decay 방지, Phase 1에는 없음)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBacktestRequestDTO {

    @NotEmpty(message = "거래할 종목을 선택해주세요")
    private List<String> stockCodes;

    @NotNull(message = "시작일을 입력해주세요")
    private LocalDate startDate;

    @NotNull(message = "종료일을 입력해주세요")
    private LocalDate endDate;

    @NotNull(message = "초기 투자금을 입력해주세요")
    @DecimalMin(value = "100000", message = "최소 10만원 이상 입력해주세요")
    private BigDecimal initialBalance;

    // 거래 설정 (선택 - 없으면 기본값 사용, 기본값은 StockTradingSetting과 동일)
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
    private BigDecimal sellTargetPct = new BigDecimal("2.5");

    @DecimalMax(value = "0", message = "손절매 기준은 0 이하")
    @DecimalMin(value = "-30", message = "손절매 기준은 -30% 이상")
    @Builder.Default
    private BigDecimal stopLossPct = new BigDecimal("-5");

    @Min(value = 1, message = "최소 1건 이상")
    @Max(value = 10, message = "최대 10건 이하")
    @Builder.Default
    private Integer maxHoldingsPerStock = 3;

    @Builder.Default
    private Boolean useTrailingStop = true;

    @DecimalMin(value = "1", message = "트레일링 스톱은 1% 이상")
    @DecimalMax(value = "10", message = "트레일링 스톱은 10% 이하")
    @Builder.Default
    private BigDecimal trailingStopPct = new BigDecimal("2.5");

    @Min(value = 5, message = "RSI 기간은 5일 이상")
    @Max(value = 50, message = "RSI 기간은 50일 이하")
    @Builder.Default
    private Integer rsiPeriod = 14;

    @Min(value = 10, message = "RSI 매수 임계값은 10 이상")
    @Max(value = 50, message = "RSI 매수 임계값은 50 이하")
    @Builder.Default
    private Integer rsiBuyThreshold = 35;

    @Min(value = 50, message = "RSI 매도 임계값은 50 이상")
    @Max(value = 90, message = "RSI 매도 임계값은 90 이하")
    @Builder.Default
    private Integer rsiSellThreshold = 65;

    // 볼린저 밴드 설정
    @Min(value = 10, message = "볼린저 밴드 기간은 10일 이상")
    @Max(value = 50, message = "볼린저 밴드 기간은 50일 이하")
    @Builder.Default
    private Integer bbPeriod = 20;

    @Min(value = 1, message = "표준편차 승수는 1 이상")
    @Max(value = 4, message = "표준편차 승수는 4 이하")
    @Builder.Default
    private Integer bbMultiplier = 2;

    // 거래량 설정 (ETF 특성 반영: 120%)
    @Min(value = 100, message = "거래량 기준은 100% 이상")
    @Max(value = 500, message = "거래량 기준은 500% 이하")
    @Builder.Default
    private Integer volumeThreshold = 120;

    // ⭐⭐⭐ [Day 62 신규] 레버리지 ETF decay 방지: 최대 보유 거래일 ⭐⭐⭐
    // StockSignalDetectorService의 실거래 로직과 동일하게, 도달 시 강제 매도 처리
    @Min(value = 1, message = "최대 보유일은 1일 이상")
    @Max(value = 60, message = "최대 보유일은 60일 이하")
    @Builder.Default
    private Integer maxHoldingDays = 20;

    // 리스크 관리 설정
    @Min(value = 10, message = "일일 거래 한도는 10% 이상")
    @Max(value = 100, message = "일일 거래 한도는 100% 이하")
    @Builder.Default
    private Integer dailyTradeLimitPct = 20;

    @Min(value = 10, message = "종목 비중은 10% 이상")
    @Max(value = 100, message = "종목 비중은 100% 이하")
    @Builder.Default
    private Integer maxPositionPct = 25;

    @Min(value = -50, message = "긴급 정지는 -50% 이상")
    @Max(value = 0, message = "긴급 정지는 0% 이하")
    @Builder.Default
    private Integer dailyStopLossPct = -5;

    // 급락장 보호 기능
    // ⚠️ [Day 62 주의] 코인(Phase 1)은 BTC MA20을 시장 기준으로 사용하나,
    // 국내 주식은 대표 지수 프록시 종목이 아직 설정되어 있지 않아 Day 62 기준
    // 이 옵션은 요청을 받아도 실제 필터링에는 영향을 주지 않는다 (추후 KOSPI/KOSDAQ 연동 예정).
    @Builder.Default
    private Boolean useMarketTrendFilter = false;

    @Builder.Default
    private Integer cumulativeLossLimitPct = -10;

    @Builder.Default
    private Integer consecutiveStopLossLimit = 3;

    // 1회 고정 매수 금액(원) - 라운드로빈 OFF 시 사용
    @DecimalMin(value = "10000", message = "1회 매수 금액은 최소 10,000원 이상")
    @DecimalMax(value = "10000000", message = "1회 매수 금액은 최대 1,000만원 이하")
    @Builder.Default
    private BigDecimal fixedBuyAmount = new BigDecimal("100000");

    @Builder.Default
    private Boolean useDailyLimitRecovery = false;

    @Builder.Default
    private Boolean useRoundRobin = true;

    @DecimalMin(value = "0.0", message = "추가 매수 하락률은 0% 이상")
    @DecimalMax(value = "10.0", message = "추가 매수 하락률은 10% 이하")
    @Builder.Default
    private BigDecimal additionalDropPct = new BigDecimal("1.0");

    @Builder.Default
    private Boolean useStopLoss = true;
}