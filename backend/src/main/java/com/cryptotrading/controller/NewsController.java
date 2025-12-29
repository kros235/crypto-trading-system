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

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "News", description = "뉴스 수집 및 조회 API")
public class NewsController {
    
    private final NewsCollectorService newsCollectorService;
    
    @Operation(summary = "뉴스 수동 수집", description = "지정된 코인의 뉴스를 수동으로 수집합니다 (관리자 전용)")
    @PostMapping("/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CoinNewsDTO>>> collectNews(
            @RequestParam(defaultValue = "KRW-BTC,KRW-ETH") String coins
    ) {
        log.info("뉴스 수동 수집 요청: {}", coins);
        List<String> targetCoins = Arrays.asList(coins.split(","));
        List<CoinNewsDTO> collected = newsCollectorService.collectAllNews(targetCoins);
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
}