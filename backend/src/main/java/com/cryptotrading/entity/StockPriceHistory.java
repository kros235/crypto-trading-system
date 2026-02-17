package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 가격 이력 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 */
@Entity
@Table(name = "stock_price_history",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stock_code", "timestamp"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private LocalDate timestamp;

    @Column(name = "open_price", precision = 15, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 15, scale = 2)
    private BigDecimal lowPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal ma7;

    @Column(precision = 15, scale = 2)
    private BigDecimal ma20;

    @Column(precision = 15, scale = 2)
    private BigDecimal ma30;

    @Column(precision = 5, scale = 2)
    private BigDecimal rsi;

    @Column(name = "bb_upper", precision = 15, scale = 2)
    private BigDecimal bbUpper;

    @Column(name = "bb_lower", precision = 15, scale = 2)
    private BigDecimal bbLower;
}