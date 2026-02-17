package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 일간 집계 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 */
@Entity
@Table(name = "stock_daily_summary",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_profit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @Column(name = "profit_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal profitRate = BigDecimal.ZERO;

    @Column(name = "buy_count")
    @Builder.Default
    private Integer buyCount = 0;

    @Column(name = "sell_count")
    @Builder.Default
    private Integer sellCount = 0;

    @Column(name = "total_investment", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInvestment = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}