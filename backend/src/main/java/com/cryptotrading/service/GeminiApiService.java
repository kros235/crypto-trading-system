package com.cryptotrading.service;

import com.cryptotrading.entity.CoinNews;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * AI API 서비스 (Groq API 사용)
 * ✅ 변경: Gemini → Groq API로 전환 (2025-12-29)
 * 기존 벌크 분석 로직 유지
 */
@Slf4j
@Service
public class GeminiApiService {
    
    // ✅ 변경: Groq API 설정
    @Value("${groq.api.key:}")
    private String groqApiKey;
    
    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String groqModel;
    
    @Value("${groq.api.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqBaseUrl;
    
    @Value("${groq.api.timeout:60}")
    private int timeoutSeconds;
    
    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            log.warn("⚠️ Groq API 키가 설정되지 않았습니다. AI 뉴스 분석 기능이 비활성화됩니다.");
            return;
        }
        
        // ✅ 변경: Groq API용 WebClient 설정
        this.webClient = WebClient.builder()
                .baseUrl(groqBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .build();
        
        log.info("✅ Groq API 클라이언트 초기화 완료 (모델: {})", groqModel);
    }
    
    /**
     * ✅ 변경: Groq API로 텍스트 생성
     */
    public String generateContent(String prompt) {
        if (webClient == null) {
            log.error("Groq API 클라이언트가 초기화되지 않았습니다.");
            return null;
        }
        
        try {
            // ✅ 변경: Groq API 요청 형식 (OpenAI 호환)
            Map<String, Object> request = new HashMap<>();
            request.put("model", groqModel);
            request.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            request.put("max_tokens", 2048);
            request.put("temperature", 0.3);
            
            log.debug("Groq API 요청 - 모델: {}, 프롬프트 길이: {} chars", groqModel, prompt.length());
            
            String response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                            .maxBackoff(Duration.ofSeconds(30))
                            .jitter(0.3)
                            .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                            .doBeforeRetry(signal -> log.warn("⚠️ Rate Limit 감지 - 재시도 {}회", signal.totalRetries() + 1)))
                    .block();
            
            // ✅ 변경: Groq 응답 파싱 (OpenAI 형식)
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = jsonNode.path("choices").path(0).path("message").path("content").asText();
            
            log.debug("Groq API 응답 수신 - 길이: {} chars", content.length());
            return content;
            
        } catch (WebClientResponseException.TooManyRequests e) {
            log.error("Groq API Rate Limit 초과: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Groq API 호출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 벌크 뉴스 분석 - 여러 뉴스를 한 번에 분석 (기존 로직 유지)
     */
    public Map<Long, NewsAnalysisResult> analyzeBulkNews(List<CoinNews> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return Collections.emptyMap();
        }
        
        log.info("📦 벌크 뉴스 분석 시작 - {} 건", newsList.size());
        
        // 벌크 프롬프트 생성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 암호화폐 뉴스 분석 전문가입니다.\n");
        promptBuilder.append("아래 뉴스들을 각각 분석하여 JSON 배열로 응답해주세요.\n\n");
        promptBuilder.append("각 뉴스에 대해 다음 형식으로 응답:\n");
        promptBuilder.append("- id: 뉴스 ID\n");
        promptBuilder.append("- score: -1.0(매우 악재) ~ 1.0(매우 호재) 사이 점수\n");
        promptBuilder.append("- sentiment: POSITIVE, NEGATIVE, NEUTRAL 중 하나\n");
        promptBuilder.append("- summary: 한줄 요약 (20자 이내)\n\n");
        promptBuilder.append("=== 분석할 뉴스 목록 ===\n\n");
        
        for (CoinNews news : newsList) {
            promptBuilder.append(String.format("[ID: %d] %s\n", news.getId(), news.getTitle()));
            if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                promptBuilder.append(String.format("내용: %s\n", 
                    news.getSummary().length() > 100 ? news.getSummary().substring(0, 100) + "..." : news.getSummary()));
            }
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("\n=== 응답 형식 (JSON만 출력, 다른 텍스트 없이) ===\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\"id\": 1, \"score\": 0.5, \"sentiment\": \"POSITIVE\", \"summary\": \"비트코인 상승 전망\"},\n");
        promptBuilder.append("  {\"id\": 2, \"score\": -0.3, \"sentiment\": \"NEGATIVE\", \"summary\": \"규제 우려 확대\"}\n");
        promptBuilder.append("]\n");
        
        try {
            String response = generateContent(promptBuilder.toString());
            return parseBulkAnalysisResponse(response, newsList);
        } catch (Exception e) {
            log.error("벌크 분석 실패: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * 벌크 분석 응답 파싱 (기존 로직 유지)
     */
    private Map<Long, NewsAnalysisResult> parseBulkAnalysisResponse(String response, List<CoinNews> newsList) {
        Map<Long, NewsAnalysisResult> results = new HashMap<>();
        
        if (response == null || response.isEmpty()) {
            log.warn("벌크 분석 응답이 비어있습니다");
            return results;
        }
        
        try {
            // JSON 배열 추출 (```json ... ``` 형식 처리)
            String jsonContent = response;
            if (response.contains("```json")) {
                jsonContent = response.substring(response.indexOf("```json") + 7);
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            } else if (response.contains("```")) {
                jsonContent = response.substring(response.indexOf("```") + 3);
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            }
            jsonContent = jsonContent.trim();
            
            // JSON 파싱
            JsonNode arrayNode = objectMapper.readTree(jsonContent);
            
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    try {
                        long id = node.get("id").asLong();
                        double score = node.get("score").asDouble();
                        String sentiment = node.get("sentiment").asText();
                        String summary = node.has("summary") ? node.get("summary").asText() : "";
                        
                        NewsAnalysisResult result = new NewsAnalysisResult();
                        result.setScore(BigDecimal.valueOf(score));
                        result.setSentiment(sentiment);
                        result.setSummary(summary);
                        
                        results.put(id, result);
                        log.debug("뉴스 ID {} 분석 완료: score={}, sentiment={}", id, score, sentiment);
                    } catch (Exception e) {
                        log.warn("개별 뉴스 파싱 실패: {}", e.getMessage());
                    }
                }
            }
            
            log.info("✅ 벌크 분석 완료 - {} / {} 건 성공", results.size(), newsList.size());
            
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * API 상태 확인
     */
    public String getStatus() {
        if (webClient == null || groqApiKey == null || groqApiKey.isEmpty()) {
            return "DISABLED";
        }
        return "ENABLED (Groq - " + groqModel + ")";
    }
    
    /**
     * 뉴스 분석 결과 DTO
     */
    @Data
    public static class NewsAnalysisResult {
        private BigDecimal score;
        private String sentiment;
        private String summary;
    }
}