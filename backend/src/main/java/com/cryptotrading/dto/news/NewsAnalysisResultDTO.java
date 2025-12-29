package com.cryptotrading.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 분석 결과 DTO
 * Day 25: AI 뉴스 분석 (2025-12-29)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsAnalysisResultDTO {
    
    // 코인 정보
    private String coinSymbol;
    private String coinName;
    
    // 분석 결과
    private int newsCount;                    // 분석된 뉴스 건수
    private BigDecimal averageScore;          // 평균 점수 (-100 ~ +100)
    private BigDecimal weightAdjustment;      // 가중치 조정값 (-0.5, 0, +0.5)
    private String sentiment;                 // POSITIVE, NEGATIVE, NEUTRAL
    private String summary;                   // 분석 요약
    
    // 분석된 개별 뉴스
    private List<AnalyzedNews> analyzedNewsList;
    
    // 분석 시간
    private LocalDateTime analyzedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzedNews {
        private Long newsId;
        private String title;
        private String source;
        private int score;                    // 개별 뉴스 점수 (-100 ~ +100)
        private String sentiment;             // POSITIVE, NEGATIVE, NEUTRAL
        private String reason;                // 점수 책정 이유
    }
    
    /**
     * 점수 → 감성 변환
     */
    public static String scoreToSentiment(BigDecimal score) {
        if (score == null) return "NEUTRAL";
        double s = score.doubleValue();
        if (s >= 50) return "POSITIVE";
        if (s <= -50) return "NEGATIVE";
        return "NEUTRAL";
    }
    
    /**
     * 점수 → 가중치 변환
     * - +50 ~ +100: +0.5% (호재)
     * - -49 ~ +49: 0% (중립)
     * - -100 ~ -50: -0.5% (악재)
     */
    public static BigDecimal scoreToWeight(BigDecimal score) {
        if (score == null) return BigDecimal.ZERO;
        double s = score.doubleValue();
        if (s >= 50) return new BigDecimal("0.5");
        if (s <= -50) return new BigDecimal("-0.5");
        return BigDecimal.ZERO;
    }
}