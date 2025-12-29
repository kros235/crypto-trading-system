package com.cryptotrading.service;

import com.cryptotrading.dto.news.CoinNewsDTO;
import com.cryptotrading.dto.news.NewsAnalysisResultDTO;
import com.cryptotrading.entity.CoinNews;
import com.cryptotrading.entity.CoinNewsAnalysis;
import com.cryptotrading.repository.CoinNewsAnalysisRepository;
import com.cryptotrading.repository.CoinNewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 뉴스 분석 서비스
 * Day 25: AI 뉴스 분석 - Gemini API 연동 (2025-12-29)
 * 최적화: 벌크 분석 + 분석 완료 플래그
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {
    
    private final GeminiApiService geminiApiService;
    private final CoinNewsRepository coinNewsRepository;
    private final CoinNewsAnalysisRepository coinNewsAnalysisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    
    /**
     * 특정 코인의 당일 뉴스 분석 (최적화 버전)
     */
    @Transactional
    public NewsAnalysisResultDTO analyzeNewsForCoin(String userId, String coinSymbol) {
        LocalDate today = LocalDate.now(KST);
        
        // 1. 오늘 이미 분석된 결과가 있는지 확인
        Optional<CoinNewsAnalysis> existingAnalysis = coinNewsAnalysisRepository
                .findByUserIdAndCoinSymbolAndAnalysisDate(userId, coinSymbol, today);
        
        // 2. 뉴스 검색 범위 설정 (KST 기준 오늘)
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now(KST);
        
        log.info("뉴스 검색 범위 (KST): {} ~ {}", startOfDay, endOfDay);
        
        // 3. ✅ 분석되지 않은 뉴스만 조회 (최적화 핵심!)
        List<CoinNews> unanalyzedNews = coinNewsRepository
                .findByCoinSymbolAndAnalyzedFalseAndPublishedAtBetweenOrderByPublishedAtDesc(
                        coinSymbol, startOfDay, endOfDay);
        
        log.info("📰 미분석 뉴스 건수: {} (코인: {})", unanalyzedNews.size(), coinSymbol);
        
        // 4. 미분석 뉴스가 없으면 기존 분석 결과 반환
        if (unanalyzedNews.isEmpty()) {
            if (existingAnalysis.isPresent()) {
                log.info("✅ 새로운 뉴스 없음 - 기존 분석 결과 반환");
                return convertToDTO(existingAnalysis.get());
            } else {
                // 분석할 뉴스도 없고 기존 결과도 없음
                return createEmptyResult(coinSymbol);
            }
        }
        
        // 5. ✅ 벌크 분석 실행 (1회 API 호출로 모든 뉴스 분석!)
        Map<Long, GeminiApiService.NewsAnalysisResult> analysisResults = 
                geminiApiService.analyzeBulkNews(unanalyzedNews);
        
        // 6. 분석 결과를 각 뉴스에 저장 + 플래그 업데이트
        List<NewsAnalysisResultDTO.AnalyzedNews> analyzedNewsList = new ArrayList<>();
        int analyzedCount = 0;
        
        for (CoinNews news : unanalyzedNews) {
            GeminiApiService.NewsAnalysisResult result = analysisResults.get(news.getId());
            if (result != null) {
                news.setAnalyzed(true);
                news.setAnalyzedAt(LocalDateTime.now());
                news.setSentimentScore(result.getScore());
                coinNewsRepository.save(news);
                
                // DTO 생성
                analyzedNewsList.add(NewsAnalysisResultDTO.AnalyzedNews.builder()
                        .newsId(news.getId())
                        .title(news.getTitle())
                        .source(news.getSource())
                        .score((int)(result.getScore().doubleValue() * 100))
                        .sentiment(result.getSentiment())
                        .reason(result.getSummary())
                        .build());
                
                analyzedCount++;
            }
        }
        
        // 7. 기존 분석된 뉴스도 포함하여 전체 점수 계산
        List<CoinNews> allTodayNews = coinNewsRepository
                .findByCoinSymbolAndPublishedAtBetweenOrderByPublishedAtDesc(
                        coinSymbol, startOfDay, endOfDay);
        
        double allTotalScore = 0;
        int allCount = 0;
        for (CoinNews news : allTodayNews) {
            if (news.getSentimentScore() != null) {
                allTotalScore += news.getSentimentScore().doubleValue();
                allCount++;
            }
        }
        
        // 8. 종합 분석 결과 저장
        double averageScore = allCount > 0 ? allTotalScore / allCount : 0;
        CoinNewsAnalysis.Sentiment sentimentEnum = averageScore > 0.2 ? CoinNewsAnalysis.Sentiment.POSITIVE : 
                          (averageScore < -0.2 ? CoinNewsAnalysis.Sentiment.NEGATIVE : CoinNewsAnalysis.Sentiment.NEUTRAL);
        double weightAdjustment = averageScore * 0.5; // -0.5% ~ +0.5%
        
        CoinNewsAnalysis analysis = existingAnalysis.orElse(new CoinNewsAnalysis());
        analysis.setUserId(userId);
        analysis.setCoinSymbol(coinSymbol);
        analysis.setAnalysisDate(today);
        analysis.setNewsCount(allCount);
        analysis.setAverageScore(BigDecimal.valueOf(averageScore).setScale(2, RoundingMode.HALF_UP));
        analysis.setSentiment(sentimentEnum);
        analysis.setWeightAdjustment(BigDecimal.valueOf(weightAdjustment).setScale(4, RoundingMode.HALF_UP));
        analysis.setSummary(String.format("%d건 분석 완료 (신규 %d건)", allCount, analyzedCount));
        analysis.setAnalyzedAt(LocalDateTime.now());
        
        coinNewsAnalysisRepository.save(analysis);
        
        log.info("✅ 뉴스 분석 완료 - 신규 {}건 분석, 전체 {}건, 평균점수: {}, 감성: {}", 
                analyzedCount, allCount, String.format("%.2f", averageScore), sentimentEnum);
        
        // 9. DTO 반환
        return NewsAnalysisResultDTO.builder()
                .coinSymbol(coinSymbol)
                .newsCount(allCount)
                .averageScore(BigDecimal.valueOf(averageScore).setScale(2, RoundingMode.HALF_UP))
                .weightAdjustment(BigDecimal.valueOf(weightAdjustment).setScale(4, RoundingMode.HALF_UP))
                .sentiment(sentimentEnum.name())
                .summary(analysis.getSummary())
                .analyzedNewsList(analyzedNewsList)
                .analyzedAt(LocalDateTime.now(KST))
                .build();
    }
    
    /**
     * 개별 뉴스 분석 (레거시 - 필요시 사용)
     */
    private NewsAnalysisResultDTO.AnalyzedNews analyzeIndividualNews(CoinNews news) {
        String prompt = buildAnalysisPrompt(news);
        String response = geminiApiService.generateContent(prompt);
        
        if (response == null || response.isEmpty()) {
            log.warn("Gemini API 응답이 비어있습니다. newsId: {}", news.getId());
            return null;
        }
        
        try {
            return parseAnalysisResponse(news, response);
        } catch (Exception e) {
            log.error("분석 응답 파싱 실패 - newsId: {}, error: {}", news.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 분석 프롬프트 생성
     */
    private String buildAnalysisPrompt(CoinNews news) {
        return String.format("""
            당신은 암호화폐 뉴스 분석 전문가입니다.
            아래 뉴스가 해당 암호화폐의 가격에 미칠 영향을 분석해주세요.
            
            [코인]: %s
            [뉴스 제목]: %s
            [뉴스 내용]: %s
            [출처]: %s
            
            다음 JSON 형식으로만 응답해주세요 (다른 텍스트 없이):
            {
                "score": <-100에서 +100 사이의 정수>,
                "sentiment": "<POSITIVE 또는 NEGATIVE 또는 NEUTRAL>",
                "reason": "<점수 책정 이유를 한 문장으로>"
            }
            
            점수 기준:
            - +70 ~ +100: 매우 호재 (ETF 승인, 대규모 기관 투자 등)
            - +30 ~ +69: 호재 (긍정적 규제, 파트너십 등)
            - -29 ~ +29: 중립 (기술 업데이트, 일반 뉴스 등)
            - -69 ~ -30: 악재 (규제 강화, 해킹 등)
            - -100 ~ -70: 매우 악재 (거래소 파산, 전면 금지 등)
            
            응답은 반드시 유효한 JSON 형식이어야 합니다.
            """,
                news.getCoinSymbol(),
                news.getTitle(),
                news.getSummary() != null ? news.getSummary() : "(내용 없음)",
                news.getSource()
        );
    }
    
    /**
     * 분석 응답 파싱
     */
    private NewsAnalysisResultDTO.AnalyzedNews parseAnalysisResponse(CoinNews news, String response) throws Exception {
        // JSON 부분만 추출 (```json ... ``` 형식 처리)
        String jsonStr = response;
        if (response.contains("```json")) {
            jsonStr = response.substring(response.indexOf("```json") + 7);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
        } else if (response.contains("```")) {
            jsonStr = response.substring(response.indexOf("```") + 3);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
        }
        
        // JSON 파싱
        JsonNode jsonNode = objectMapper.readTree(jsonStr.trim());
        
        int score = jsonNode.has("score") ? jsonNode.get("score").asInt() : 0;
        String sentiment = jsonNode.has("sentiment") ? jsonNode.get("sentiment").asText() : "NEUTRAL";
        String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "";
        
        // 점수 범위 제한
        score = Math.max(-100, Math.min(100, score));
        
        return NewsAnalysisResultDTO.AnalyzedNews.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .source(news.getSource())
                .score(score)
                .sentiment(sentiment)
                .reason(reason)
                .build();
    }
    
    /**
     * 분석 요약 생성
     */
    private String generateSummary(List<NewsAnalysisResultDTO.AnalyzedNews> analyzedList, 
                                   BigDecimal averageScore, String sentiment) {
        if (analyzedList.isEmpty()) {
            return "분석된 뉴스 없음";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("총 %d건의 뉴스 분석 완료. ", analyzedList.size()));
        sb.append(String.format("종합 판단: %s (평균 점수: %.1f). ", 
                sentimentToKorean(sentiment), averageScore.doubleValue()));
        
        // 주요 뉴스 요약 (상위 3건)
        List<NewsAnalysisResultDTO.AnalyzedNews> topNews = analyzedList.stream()
                .sorted((a, b) -> Integer.compare(Math.abs(b.getScore()), Math.abs(a.getScore())))
                .limit(3)
                .collect(Collectors.toList());
        
        if (!topNews.isEmpty()) {
            sb.append("주요 뉴스: ");
            for (int i = 0; i < topNews.size(); i++) {
                NewsAnalysisResultDTO.AnalyzedNews n = topNews.get(i);
                sb.append(String.format("[%s] %s", n.getSource(), truncate(n.getTitle(), 30)));
                if (i < topNews.size() - 1) sb.append(" / ");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 감성 한글 변환
     */
    private String sentimentToKorean(String sentiment) {
        return switch (sentiment) {
            case "POSITIVE" -> "호재";
            case "NEGATIVE" -> "악재";
            default -> "중립";
        };
    }
    
    /**
     * 문자열 자르기
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
    
    /**
     * 빈 결과 생성
     */
    private NewsAnalysisResultDTO createEmptyResult(String coinSymbol) {
        return NewsAnalysisResultDTO.builder()
                .coinSymbol(coinSymbol)
                .newsCount(0)
                .averageScore(BigDecimal.ZERO)
                .weightAdjustment(BigDecimal.ZERO)
                .sentiment("NEUTRAL")
                .summary("분석할 뉴스 없음")
                .analyzedNewsList(new ArrayList<>())
                .analyzedAt(LocalDateTime.now(KST))
                .build();
    }
    
    /**
     * Entity → DTO 변환
     */
    private NewsAnalysisResultDTO convertToDTO(CoinNewsAnalysis entity) {
        return NewsAnalysisResultDTO.builder()
                .coinSymbol(entity.getCoinSymbol())
                .newsCount(entity.getNewsCount())
                .averageScore(entity.getAverageScore())
                .weightAdjustment(entity.getWeightAdjustment())
                .sentiment(entity.getSentiment().name())
                .summary(entity.getSummary())
                .analyzedNewsList(new ArrayList<>())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }
    
    /**
     * 사용자의 특정 코인 가중치 조회
     */
    public BigDecimal getWeightAdjustment(String userId, String coinSymbol) {
        LocalDate today = LocalDate.now(KST);
        return coinNewsAnalysisRepository.findByUserIdAndCoinSymbolAndAnalysisDate(userId, coinSymbol, today)
                .map(CoinNewsAnalysis::getWeightAdjustment)
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * 모든 가중치 초기화 (일일 초기화용)
     */
    @Transactional
    public void resetAllWeights() {
        LocalDate today = LocalDate.now(KST);
        int count = coinNewsAnalysisRepository.resetWeightsByDate(today);
        log.info("가중치 초기화 완료 - 초기화 건수: {}", count);
    }
    
    /**
     * Gemini API 상태 조회
     */
    public String getApiStatus() {
        return geminiApiService.getStatus();
    }
}