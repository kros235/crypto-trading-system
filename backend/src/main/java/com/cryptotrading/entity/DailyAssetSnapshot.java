package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 자산 스냅샷 엔티티
 * 매일 23:59 KST 기준으로 사용자의 평가금액과 불입금액을 기록
 */
@Entity
@Table(name = "daily_asset_snapshot", uniqueConstraints = {
    @UniqueConstraint(name = "unique_user_snapshot_date", columnNames = {"user_id", "snapshot_date"})
}, indexes = {
    @Index(name = "idx_snapshot_user_date", columnList = "user_id, snapshot_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /** 평가금액 (KRW잔고 + 코인평가액) */
    @Column(name = "evaluation_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal evaluationAmount;

    /** 누적 불입금액 (입금 - 출금) */
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    /** KRW 잔고 */
    @Column(name = "krw_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal krwBalance;

    /** 코인 평가액 */
    @Column(name = "coin_evaluation", nullable = false, precision = 15, scale = 2)
    private BigDecimal coinEvaluation;

    /** 수익 금액 (평가금액 - 불입금액) */
    @Column(name = "profit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal profitAmount;

    /** 수익률 (%) */
    @Column(name = "profit_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}