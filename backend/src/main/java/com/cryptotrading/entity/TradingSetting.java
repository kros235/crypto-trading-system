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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", 
                insertable = false, updatable = false)
    private User user;
}