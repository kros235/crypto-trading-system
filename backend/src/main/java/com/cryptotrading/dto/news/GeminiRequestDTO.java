package com.cryptotrading.dto.news;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 요청 DTO
 * Day 25: AI 뉴스 분석 (2025-12-29)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequestDTO {
    
    private List<Content> contents;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
        
        @JsonProperty("topP")
        private Double topP;
        
        @JsonProperty("topK")
        private Integer topK;
    }
    
    /**
     * 간편 생성 메서드
     */
    public static GeminiRequestDTO of(String prompt) {
        return GeminiRequestDTO.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(Part.builder().text(prompt).build()))
                                .build()
                ))
                .generationConfig(GenerationConfig.builder()
                        .temperature(0.3)  // 낮은 온도로 일관성 있는 응답
                        .maxOutputTokens(1024)
                        .topP(0.8)
                        .topK(40)
                        .build())
                .build();
    }
}