package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 휴장일 캘린더 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 * 데이터는 API 자동 수집 + 관리자 수동 등록 방식으로 관리
 */
@Entity
@Table(name = "market_holidays",
    uniqueConstraints = @UniqueConstraint(columnNames = {"market", "holiday_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(length = 20)
    @Builder.Default
    private String market = "KRX";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}