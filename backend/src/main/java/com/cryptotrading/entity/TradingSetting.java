package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "trading_settings", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coin_symbols", nullable = false, columnDefinition = "json")
    private List<String> coinSymbols;

    @Column(name = "base_period")
    private Integer basePeriod = 20;

    @Column(name = "buy_threshold_pct", precision = 5, scale = 2)
    private BigDecimal buyThresholdPct = new BigDecimal("-6.00");

    @Column(name = "sell_target_pct", precision = 5, scale = 2)
    private BigDecimal sellTargetPct = new BigDecimal("4.00");

    @Column(name = "stop_loss_pct", precision = 5, scale = 2)
    private BigDecimal stopLossPct = new BigDecimal("-8.00");

    @Column(name = "max_holdings_per_coin")
    private Integer maxHoldingsPerCoin = 2;

    // ⚠️ DEPRECATED: 이 필드는 더 이상 사용되지 않습니다.
    // 일일 한도는 이제 매일 00:00 KST에 업비트 총자산을 조회하여 자동 계산됩니다.
    // 기존 데이터 호환성을 위해 컬럼은 유지하지만, 새로운 로직에서는 사용하지 않습니다.
    @Deprecated
    @Column(name = "daily_limit_amount", precision = 15, scale = 2)
    private BigDecimal dailyLimitAmount = new BigDecimal("1000000.00");

    @Column(name = "use_ai_analysis")
    private Boolean useAiAnalysis = false;

    @Column(name = "use_trailing_stop")
    private Boolean useTrailingStop = true;

    @Column(name = "trailing_stop_pct", precision = 5, scale = 2)
    private BigDecimal trailingStopPct = new BigDecimal("-4.00");

    // RSI 설정
    @Column(name = "rsi_period")
    private Integer rsiPeriod = 14;

    @Column(name = "rsi_buy_threshold")
    private Integer rsiBuyThreshold = 32;

    @Column(name = "rsi_sell_threshold")
    private Integer rsiSellThreshold = 68;

    // 볼린저 밴드 설정
    @Column(name = "bb_period")
    private Integer bbPeriod = 20;

    @Column(name = "bb_multiplier")
    private Integer bbMultiplier = 2;

    // 거래량 설정
    @Column(name = "volume_threshold")
    private Integer volumeThreshold = 140;

    // 일일 최대 거래금액 (초기 자본 대비 %)
    @Column(name = "daily_trade_limit_pct")
    private Integer dailyTradeLimitPct = 20;

    // 단일 종목 최대 비중 (총 자본 대비 %)
    @Column(name = "max_position_pct")
    private Integer maxPositionPct = 25;

    // 긴급 정지 조건 - 일일 손실률 (%)
    @Column(name = "daily_stop_loss_pct")
    private Integer dailyStopLossPct = -5;

    // ⭐⭐⭐ Day 29 추가: 급락장 보호 기능 (3개 필드) ⭐⭐⭐
    // 시장 추세 필터 (BTC MA20 기준)
    @Column(name = "use_market_trend_filter")
    private Boolean useMarketTrendFilter = false;

    // 누적 손실 한도 (초기 자본 대비 %)
    @Column(name = "cumulative_loss_limit_pct")
    private Integer cumulativeLossLimitPct = -10;

    // 연속 손절 제한 횟수
    @Column(name = "consecutive_stop_loss_limit")
    private Integer consecutiveStopLossLimit = 3;

    // ⭐⭐⭐ 변경: 1회 매수 비율(%) → 1회 고정 매수 금액(원) ⭐⭐⭐
    // 라운드로빈 OFF 시 각 코인에 이 금액만큼 매수
    // 최소 5,000원 이상 (업비트 최소 주문금액)
    @Column(name = "fixed_buy_amount", precision = 15, scale = 2)
    private BigDecimal fixedBuyAmount = new BigDecimal("10000.00");

    // 일일 한도 복구 옵션
    @Column(name = "use_daily_limit_recovery")
    private Boolean useDailyLimitRecovery = false;

    // ⭐⭐⭐ 변경: 매수 방식 선택 (라운드로빈 vs 고정금액) ⭐⭐⭐
    // true: 라운드로빈 (일일 한도를 매수 신호 수로 균등 분배)
    // false: 고정 금액 (fixedBuyAmount만큼 각 코인에 매수)
    @Column(name = "use_round_robin")
    @Builder.Default
    private Boolean useRoundRobin = true; // 기본값: true (라운드로빈 방식)

    @Column(name = "additional_drop_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal additionalDropPct = new BigDecimal("1.0");

    @Column(name = "use_stop_loss", nullable = false)
    @Builder.Default
    private Boolean useStopLoss = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User user;
}