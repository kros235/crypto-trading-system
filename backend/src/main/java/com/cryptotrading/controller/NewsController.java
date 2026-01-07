package com.cryptotrading.controller;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.news.CoinNewsDTO;
import com.cryptotrading.service.NewsCollectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.cryptotrading.dto.news.NewsAnalysisResultDTO;
import com.cryptotrading.service.NewsAnalysisService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "News", description = "뉴스 수집 및 조회 API")
public class NewsController {
    
    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;
    
    @Operation(summary = "뉴스 수동 수집", description = "지정된 코인의 뉴스를 수동으로 수집합니다 (관리자 전용)")
    @PostMapping("/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CoinNewsDTO>>> collectNews(
            @RequestParam(defaultValue = "KRW-BTC,KRW-ETH") String coins,
            @AuthenticationPrincipal String userId
    ) {
        log.info("뉴스 수동 수집 요청: {}", coins);
        List<String> targetCoins = Arrays.asList(coins.split(","));
        List<CoinNewsDTO> collected = newsCollectorService.collectAllNews(targetCoins);
        
        // ============================================================
        // ⭐⭐⭐ [추가] 수집 후 즉시 AI 분석 실행 ⭐⭐⭐
        // ============================================================
        if (!collected.isEmpty()) {
            log.info("🤖 수집된 뉴스 AI 분석 시작...");
            int totalAnalyzed = 0;
            for (String coinSymbol : targetCoins) {
                try {
                    NewsAnalysisResultDTO result = newsAnalysisService.analyzeNewsForCoin(userId, coinSymbol);
                    if (result.getNewsCount() > 0) {
                        totalAnalyzed += result.getNewsCount();
                        log.info("✅ {} 분석 완료: {}건, 평균점수: {}", 
                                coinSymbol, result.getNewsCount(), result.getAverageScore());
                    }
                } catch (Exception e) {
                    log.error("❌ {} 분석 실패: {}", coinSymbol, e.getMessage());
                }
            }
            log.info("🤖 AI 분석 완료 - 총 {}건 분석", totalAnalyzed);
        }
        // ============================================================
        
        return ResponseEntity.ok(ApiResponse.success(collected, 
                String.format("%d건의 뉴스가 수집되었습니다.", collected.size())));
    }
    
    @Operation(summary = "당일 뉴스 조회", description = "특정 코인의 당일 뉴스를 조회합니다")
    @GetMapping("/today/{symbol}")
    public ResponseEntity<ApiResponse<List<CoinNewsDTO>>> getTodayNews(
            @PathVariable String symbol
    ) {
        List<CoinNewsDTO> news = newsCollectorService.getTodayNews(symbol);
        return ResponseEntity.ok(ApiResponse.success(news));
    }
    
    @Operation(summary = "오래된 뉴스 정리", description = "7일 초과 뉴스 데이터를 삭제합니다 (관리자 전용)")
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldNews() {
        int deleted = newsCollectorService.cleanupOldNews();
        return ResponseEntity.ok(ApiResponse.success(deleted, 
                String.format("%d건의 뉴스가 삭제되었습니다.", deleted)));
    }

    @PostMapping("/analyze/{symbol}")
    @Operation(summary = "코인 뉴스 분석", description = "특정 코인의 당일 뉴스를 AI로 분석합니다.")
    public ResponseEntity<ApiResponse<NewsAnalysisResultDTO>> analyzeNews(
            @PathVariable String symbol,
            @AuthenticationPrincipal String userId) {
        
        log.info("뉴스 분석 요청 - userId: {}, symbol: {}", userId, symbol);
        
        NewsAnalysisResultDTO result = newsAnalysisService.analyzeNewsForCoin(userId, symbol);
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error(
        	ApiResponse.ErrorResponse.builder()
                .code("AI_SERVICE_DISABLED")
                .message("AI 분석 서비스가 비활성화 상태입니다.")
                .build()
	));
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/analysis/status")
    @Operation(summary = "AI 분석 서비스 상태", description = "Gemini API 연동 상태를 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysisStatus() {
        String status = newsAnalysisService.getApiStatus();
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", status,
                "enabled", status.equals("ENABLED"),
                "message", getStatusMessage(status)
        )));
    }
    
    @PostMapping("/analysis/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "가중치 초기화", description = "관리자 전용: 모든 AI 가중치를 0으로 초기화합니다.")
    public ResponseEntity<ApiResponse<String>> resetWeights() {
        log.info("AI 가중치 초기화 요청");
        newsAnalysisService.resetAllWeights();
        return ResponseEntity.ok(ApiResponse.success("모든 가중치가 초기화되었습니다."));
    }
    
    @GetMapping("/analysis/weight/{symbol}")
    @Operation(summary = "코인 가중치 조회", description = "특정 코인의 현재 AI 가중치를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeight(
            @PathVariable String symbol,
            @AuthenticationPrincipal String userId) {
        
        var weight = newsAnalysisService.getWeightAdjustment(userId, symbol);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "coinSymbol", symbol,
                "weightAdjustment", weight,
                "weightPercent", weight.doubleValue() + "%"
        )));
    }

     /**
     * 뉴스 목록 조회 (페이징, 필터링, 검색)
     */
    @GetMapping("/list")
    @Operation(summary = "뉴스 목록 조회", description = "페이징, 필터링, 검색을 지원하는 뉴스 목록 조회")
    public ResponseEntity<ApiResponse<Page<CoinNewsDTO>>> getNewsList(
            @RequestParam(required = false) String coinSymbol,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("뉴스 목록 조회 - coin: {}, keyword: {}, page: {}, size: {}", 
                coinSymbol, keyword, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CoinNewsDTO> result = newsCollectorService.searchNews(coinSymbol, keyword, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * ⭐ [추가] 뉴스 상세 조회
     */
    @GetMapping("/{newsId}")
    @Operation(summary = "뉴스 상세 조회", description = "특정 뉴스의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CoinNewsDTO>> getNewsDetail(@PathVariable Long newsId) {
        
        CoinNewsDTO news = newsCollectorService.getNewsById(newsId);
        
        if (news == null) {
            return ResponseEntity.ok(ApiResponse.error(
                ApiResponse.ErrorResponse.builder()
                    .code("NEWS_NOT_FOUND")
                    .message("뉴스를 찾을 수 없습니다.")
                    .build()
            ));
        }
        
        return ResponseEntity.ok(ApiResponse.success(news));
    }
    
    /**
     * 상태 메시지 변환
     */
    private String getStatusMessage(String status) {
        return switch (status) {
            case "ENABLED" -> "AI 뉴스 분석 서비스가 활성화되어 있습니다.";
            case "DISABLED" -> "AI 뉴스 분석 서비스가 비활성화되어 있습니다.";
            case "NO_API_KEY" -> "Gemini API 키가 설정되지 않았습니다.";
            default -> "알 수 없는 상태입니다.";
        };
    }
}