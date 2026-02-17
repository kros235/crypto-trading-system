package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 거래 설정 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 */
@Entity
@Table(name = "stock_trading_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTradingSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "stock_codes", nullable = false, columnDefinition = "JSON")
    private String stockCodes;

    @Column(name = "base_period")
    @Builder.Default
    private Integer basePeriod = 20;

    @Column(name = "buy_threshold_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal buyThresholdPct = new BigDecimal("-3.00");

    @Column(name = "sell_target_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sellTargetPct = new BigDecimal("2.50");

    @Column(name = "stop_loss_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal stopLossPct = new BigDecimal("-5.00");

    @Column(name = "max_holdings_per_stock")
    @Builder.Default
    private Integer maxHoldingsPerStock = 3;

    @Column(name = "daily_limit_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dailyLimitAmount = new BigDecimal("1000000.00");

    @Column(name = "use_trailing_stop")
    @Builder.Default
    private Boolean useTrailingStop = true;

    @Column(name = "trailing_stop_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal trailingStopPct = new BigDecimal("-2.50");

    // 기술적 지표 설정
    @Column(name = "rsi_period")
    @Builder.Default
    private Integer rsiPeriod = 14;

    @Column(name = "rsi_buy_threshold")
    @Builder.Default
    private Integer rsiBuyThreshold = 35;

    @Column(name = "rsi_sell_threshold")
    @Builder.Default
    private Integer rsiSellThreshold = 65;

    @Column(name = "bb_period")
    @Builder.Default
    private Integer bbPeriod = 20;

    @Column(name = "bb_multiplier")
    @Builder.Default
    private Integer bbMultiplier = 2;

    @Column(name = "volume_threshold")
    @Builder.Default
    private Integer volumeThreshold = 120;

    // 리스크 관리 설정
    @Column(name = "daily_trade_limit_pct")
    @Builder.Default
    private Integer dailyTradeLimitPct = 20;

    @Column(name = "max_position_pct")
    @Builder.Default
    private Integer maxPositionPct = 25;

    @Column(name = "daily_stop_loss_pct")
    @Builder.Default
    private Integer dailyStopLossPct = -5;

    @Column(name = "use_market_trend_filter")
    @Builder.Default
    private Boolean useMarketTrendFilter = false;

    @Column(name = "cumulative_loss_limit_pct")
    @Builder.Default
    private Integer cumulativeLossLimitPct = -10;

    @Column(name = "consecutive_stop_loss_limit")
    @Builder.Default
    private Integer consecutiveStopLossLimit = 3;

    @Column(name = "fixed_buy_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fixedBuyAmount = new BigDecimal("100000.00");

    @Column(name = "use_daily_limit_recovery")
    @Builder.Default
    private Boolean useDailyLimitRecovery = false;

    @Column(name = "use_round_robin")
    @Builder.Default
    private Boolean useRoundRobin = true;

    // Phase 2 전용
    @Column(name = "max_holding_days")
    @Builder.Default
    private Integer maxHoldingDays = 20;

    @Column(name = "kis_app_key_encrypted", columnDefinition = "TEXT")
    private String kisAppKeyEncrypted;

    @Column(name = "kis_app_secret_encrypted", columnDefinition = "TEXT")
    private String kisAppSecretEncrypted;

    @Column(name = "kis_account_no_encrypted", columnDefinition = "TEXT")
    private String kisAccountNoEncrypted;

    @Column(name = "kis_mock_mode")
    @Builder.Default
    private Boolean kisMockMode = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}