package com.cryptotrading.controller;

import com.cryptotrading.dto.backtest.BacktestRequestDTO;
import com.cryptotrading.dto.backtest.BacktestResultDTO;
import com.cryptotrading.service.BacktestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
@Slf4j
public class BacktestController {

    private final BacktestService backtestService;

    /**
     * 백테스트 실행
     */
    @PostMapping("/run")
    public ResponseEntity<BacktestResultDTO> runBacktest(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BacktestRequestDTO request) {
        
        log.info("백테스트 요청: userId={}, coins={}, period={} ~ {}", 
                userId, request.getCoinSymbols(), request.getStartDate(), request.getEndDate());
        
        // 기간 검증
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }
        
        if (request.getEndDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("종료일은 오늘 이전이어야 합니다.");
        }
        
        // 최대 1년으로 제한
        if (request.getStartDate().plusYears(1).isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("백테스트 기간은 최대 1년입니다.");
        }
        
        BacktestResultDTO result = backtestService.runBacktest(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 빠른 백테스트 (기본 설정)
     */
    @GetMapping("/quick")
    public ResponseEntity<BacktestResultDTO> quickBacktest(
            @AuthenticationPrincipal String userId,
            @RequestParam List<String> coins,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "1000000") BigDecimal initialBalance) {
        
        log.info("빠른 백테스트: userId={}, coins={}, days={}", userId, coins, days);
        
        BacktestRequestDTO request = BacktestRequestDTO.builder()
                .coinSymbols(coins)
                .startDate(LocalDate.now().minusDays(days))
                .endDate(LocalDate.now().minusDays(1))
                .initialBalance(initialBalance)
                .build();
        
        BacktestResultDTO result = backtestService.runBacktest(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 백테스트 가능한 코인 목록
     */
    @GetMapping("/available-coins")
    public ResponseEntity<Map<String, Object>> getAvailableCoins() {
        // 주요 코인 목록 반환
        List<Map<String, String>> coins = List.of(
                Map.of("symbol", "KRW-BTC", "name", "비트코인"),
                Map.of("symbol", "KRW-ETH", "name", "이더리움"),
                Map.of("symbol", "KRW-XRP", "name", "리플"),
                Map.of("symbol", "KRW-SOL", "name", "솔라나"),
                Map.of("symbol", "KRW-DOGE", "name", "도지코인"),
                Map.of("symbol", "KRW-ADA", "name", "에이다"),
                Map.of("symbol", "KRW-AVAX", "name", "아발란체"),
                Map.of("symbol", "KRW-DOT", "name", "폴카닷"),
                Map.of("symbol", "KRW-MATIC", "name", "폴리곤"),
                Map.of("symbol", "KRW-LINK", "name", "체인링크")
        );
        
        return ResponseEntity.ok(Map.of(
                "coins", coins,
                "maxPeriodDays", 365,
                "minInitialBalance", 100000
        ));
    }

    /**
     * 백테스트 기본 설정값
     */
    @GetMapping("/default-settings")
    public ResponseEntity<BacktestRequestDTO> getDefaultSettings() {
        BacktestRequestDTO defaults = BacktestRequestDTO.builder()
                .coinSymbols(List.of("KRW-BTC", "KRW-ETH"))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .initialBalance(new BigDecimal("1000000"))
                .basePeriod(20)
                .buyThresholdPct(new BigDecimal("-3"))
                .sellTargetPct(new BigDecimal("3"))
                .stopLossPct(new BigDecimal("-5"))
                .maxHoldingsPerCoin(3)
                .useTrailingStop(false)
                .trailingStopPct(new BigDecimal("5"))
                .build();
        
        return ResponseEntity.ok(defaults);
    }
}