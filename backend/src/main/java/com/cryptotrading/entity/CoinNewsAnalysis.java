package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_news_analysis", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_coin_date", 
        columnNames = {"user_id", "coin_symbol", "analysis_date"}
    ),
    indexes = {
        @Index(name = "idx_analysis_user", columnList = "user_id"),
        @Index(name = "idx_analysis_date", columnList = "analysis_date"),
        @Index(name = "idx_analysis_coin", columnList = "coin_symbol")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinNewsAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "coin_symbol", nullable = false, length = 20)
    private String coinSymbol;
    
    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;
    
    @Column(name = "news_count")
    @Builder.Default
    private Integer newsCount = 0;
    
    @Column(name = "average_score", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal averageScore = BigDecimal.ZERO;
    
    @Column(name = "weight_adjustment", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal weightAdjustment = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Sentiment sentiment = Sentiment.NEUTRAL;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    public enum Sentiment {
        POSITIVE,   // 호재 (+50 ~ +100)
        NEGATIVE,   // 악재 (-100 ~ -50)
        NEUTRAL     // 중립 (-49 ~ +49)
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 평균 점수를 기반으로 감성과 가중치 계산
     */
    public void calculateSentimentAndWeight() {
        double score = averageScore.doubleValue();
        
        if (score >= 50) {
            sentiment = Sentiment.POSITIVE;
            weightAdjustment = new BigDecimal("0.5");
        } else if (score <= -50) {
            sentiment = Sentiment.NEGATIVE;
            weightAdjustment = new BigDecimal("-0.5");
        } else {
            sentiment = Sentiment.NEUTRAL;
            weightAdjustment = BigDecimal.ZERO;
        }
    }
}