package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_symbol_status", columnList = "user_id, coin_symbol, status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "coin_symbol", nullable = false, length = 20)
    private String coinSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 4)
    private TransactionType type;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "fee", precision = 15, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at")
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
    @Column(name = "status", length = 10)
    private TransactionStatus status = TransactionStatus.HOLDING;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", 
                insertable = false, updatable = false)
    private User user;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}