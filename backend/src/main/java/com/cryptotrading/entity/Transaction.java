package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
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
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(precision = 20, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "sold_price", precision = 20, scale = 8)
    private BigDecimal soldPrice;

    @Column(name = "profit_loss", precision = 20, scale = 8)
    private BigDecimal profitLoss;

    @Column(name = "profit_loss_pct", precision = 10, scale = 4)
    private BigDecimal profitLossPct;

    @Column(name = "target_sell_price", precision = 20, scale = 8)
    private BigDecimal targetSellPrice;

    @Column(name = "stop_loss_price", precision = 20, scale = 8)
    private BigDecimal stopLossPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.HOLDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "highest_price", precision = 20, scale = 8)
    private BigDecimal highestPrice;  // 보유 기간 중 최고가 (트레일링 스톱용)

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // 거래 유형 Enum
    public enum TransactionType {
        BUY, SELL
    }

    // 거래 상태 Enum
    public enum TransactionStatus {
        HOLDING,    // 보유 중
        SOLD,       // 매도 완료
        CANCELLED   // 취소됨
    }
}