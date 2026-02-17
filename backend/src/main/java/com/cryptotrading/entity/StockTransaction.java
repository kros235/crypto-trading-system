package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 거래 이력 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 */
@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "sold_price", precision = 15, scale = 2)
    private BigDecimal soldPrice;

    @Column(name = "profit_loss", precision = 15, scale = 2)
    private BigDecimal profitLoss;

    @Column(name = "profit_loss_pct", precision = 5, scale = 2)
    private BigDecimal profitLossPct;

    @Column(name = "target_sell_price", precision = 15, scale = 2)
    private BigDecimal targetSellPrice;

    @Column(name = "stop_loss_price", precision = 15, scale = 2)
    private BigDecimal stopLossPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.HOLDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "highest_price", precision = 15, scale = 2)
    private BigDecimal highestPrice;

    @Column(name = "holding_days")
    @Builder.Default
    private Integer holdingDays = 0;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}