package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ⭐ [수정 Q6] 주식 자산 스냅샷 엔티티
 * 코인의 AssetSnapshot과 동일한 구조
 */
@Entity
@Table(name = "stock_asset_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockAssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "evaluation_amount", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal evaluationAmount = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "profit_amount", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal profitAmount = BigDecimal.ZERO;

    @Column(name = "profit_rate", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal profitRate = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}