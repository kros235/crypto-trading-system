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
    private BigDecimal buyThresholdPct = new BigDecimal("-5.00");

    @Column(name = "sell_target_pct", precision = 5, scale = 2)
    private BigDecimal sellTargetPct = new BigDecimal("3.00");

    @Column(name = "stop_loss_pct", precision = 5, scale = 2)
    private BigDecimal stopLossPct = new BigDecimal("-10.00");

    @Column(name = "max_holdings_per_coin")
    private Integer maxHoldingsPerCoin = 3;

    @Column(name = "daily_limit_amount", precision = 15, scale = 2)
    private BigDecimal dailyLimitAmount = new BigDecimal("1000000.00");

    @Column(name = "use_ai_analysis")
    private Boolean useAiAnalysis = false;

    @Column(name = "use_trailing_stop")
    private Boolean useTrailingStop = false;

    @Column(name = "trailing_stop_pct", precision = 5, scale = 2)
    private BigDecimal trailingStopPct = new BigDecimal("-5.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", 
                insertable = false, updatable = false)
    private User user;
}