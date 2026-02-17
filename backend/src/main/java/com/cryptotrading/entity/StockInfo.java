package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식/ETF 정보 엔티티
 * Phase 2 Day 49: 주식/ETF 자동매매 기반 구축
 * 데이터는 사용자가 KIS API 종목 검색을 통해 직접 추가
 */
@Entity
@Table(name = "stock_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfo {

    @Id
    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(nullable = false, length = 20)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(name = "etf_type", nullable = false)
    private EtfType etfType;

    @Column(name = "underlying_index", length = 100)
    private String underlyingIndex;

    @Column(name = "expense_ratio", precision = 5, scale = 3)
    private BigDecimal expenseRatio;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public enum EtfType {
        LEVERAGE, INVERSE, NORMAL, STOCK
    }
}