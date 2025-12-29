package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_news", indexes = {
    @Index(name = "idx_coin_news_symbol", columnList = "coinSymbol"),
    @Index(name = "idx_coin_news_published", columnList = "publishedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinNews {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "coin_symbol", nullable = false, length = 20)
    private String coinSymbol;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(nullable = false, length = 100)
    private String source;
    
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;
    
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
    
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}