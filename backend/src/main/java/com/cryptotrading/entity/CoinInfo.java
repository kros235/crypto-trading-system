package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coin_info", indexes = {
    @Index(name = "idx_active_rank", columnList = "is_active, market_cap_rank")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinInfo {

    @Id
    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "name_kr", nullable = false, length = 50)
    private String nameKr;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        lastUpdated = LocalDateTime.now();
    }
}