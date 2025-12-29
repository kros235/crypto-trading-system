package com.cryptotrading.dto.news;

import com.cryptotrading.entity.CoinNewsAnalysis;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinNewsAnalysisDTO {
    
    private Long id;
    private String userId;
    private String coinSymbol;
    private LocalDate analysisDate;
    private Integer newsCount;
    private BigDecimal averageScore;
    private BigDecimal weightAdjustment;
    private String sentiment;
    private String summary;
    private LocalDateTime analyzedAt;
    
    // 추가 정보 (프론트엔드용)
    private String coinNameKr;
    private String weightDescription;
    
    public static CoinNewsAnalysisDTO fromEntity(CoinNewsAnalysis entity) {
        CoinNewsAnalysisDTO dto = CoinNewsAnalysisDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .coinSymbol(entity.getCoinSymbol())
                .analysisDate(entity.getAnalysisDate())
                .newsCount(entity.getNewsCount())
                .averageScore(entity.getAverageScore())
                .weightAdjustment(entity.getWeightAdjustment())
                .sentiment(entity.getSentiment().name())
                .summary(entity.getSummary())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
        
        // 가중치 설명 생성
        dto.setWeightDescription(getWeightDescription(entity.getWeightAdjustment()));
        
        return dto;
    }
    
    private static String getWeightDescription(BigDecimal weight) {
        if (weight == null) return "중립 (변동 없음)";
        
        double value = weight.doubleValue();
        if (value > 0) {
            return String.format("호재 (+%.1f%% 완화)", value);
        } else if (value < 0) {
            return String.format("악재 (%.1f%% 강화)", value);
        }
        return "중립 (변동 없음)";
    }
}