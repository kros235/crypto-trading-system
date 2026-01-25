package com.cryptotrading.controller;

import com.cryptotrading.dto.backtest.BacktestRequestDTO;
import com.cryptotrading.dto.backtest.BacktestResultDTO;
import com.cryptotrading.service.BacktestService;
import com.cryptotrading.service.UpbitApiService;

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

import com.cryptotrading.dto.upbit.UpbitMarketDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
@Slf4j
public class BacktestController {

    private final BacktestService backtestService;
    private final UpbitApiService upbitApiService;  // ★★★ 신규 추가 ★★★

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
        if (request.getStartDate().plusYears(3).isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("백테스트 기간은 최대 3년입니다.");
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
     * ★★★ 수정: 전체 코인 목록 (시가총액 순 정렬) ★★★
     */
    @GetMapping("/available-coins")
    public ResponseEntity<Map<String, Object>> getAvailableCoins() {
        try {
            // 업비트에서 전체 KRW 마켓 코인 조회
            List<UpbitMarketDTO> allMarkets = upbitApiService.getMarketAll();
        
            // KRW 마켓만 필터링
            List<String> krwMarkets = allMarkets.stream()
                    .filter(m -> m.getMarket().startsWith("KRW-"))
                    .map(UpbitMarketDTO::getMarket)
                    .toList();
        
            // 현재가 조회 (시가총액 정보 포함)
            List<UpbitTickerDTO> tickers = upbitApiService.getTicker(krwMarkets);
        
            // 시가총액 순 정렬 (거래대금 기준 - acc_trade_price_24h)
            tickers.sort((a, b) -> {
                BigDecimal aValue = a.getAccTradePrice24h() != null ? a.getAccTradePrice24h() : BigDecimal.ZERO;
                BigDecimal bValue = b.getAccTradePrice24h() != null ? b.getAccTradePrice24h() : BigDecimal.ZERO;
                return bValue.compareTo(aValue);  // 내림차순
            });
        
            // DTO 변환
            List<Map<String, Object>> coins = new ArrayList<>();
            for (int i = 0; i < tickers.size(); i++) {
                UpbitTickerDTO ticker = tickers.get(i);
                String market = ticker.getMarket();
            
                // 마켓 정보에서 한글명 찾기
                String koreanName = allMarkets.stream()
                        .filter(m -> m.getMarket().equals(market))
                        .findFirst()
                        .map(UpbitMarketDTO::getKoreanName)
                        .orElse(market.replace("KRW-", ""));
            
                Map<String, Object> coin = new HashMap<>();
                coin.put("symbol", market);
                coin.put("name", koreanName);
                coin.put("rank", i + 1);
                coin.put("accTradePrice24h", ticker.getAccTradePrice24h());
                coins.add(coin);
            }
        
            return ResponseEntity.ok(Map.of(
                    "coins", coins,
                    "maxPeriodDays", 365,
                    "minInitialBalance", 100000,
                    "totalCount", coins.size()
            ));
            
        } catch (Exception e) {
            log.error("코인 목록 조회 실패: {}", e.getMessage());
        
            // 실패 시 기본 목록 반환
            List<Map<String, String>> defaultCoins = List.of(
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
                    "coins", defaultCoins,
                    "maxPeriodDays", 365,
                    "minInitialBalance", 100000
            ));
        }
    }

    /**
     * 백테스트 기본 설정값
     */
    @GetMapping("/default-settings")
    public ResponseEntity<BacktestRequestDTO> getDefaultSettings() {
        BacktestRequestDTO defaults = BacktestRequestDTO.builder()
                .coinSymbols(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-SOL"))
  	  .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().minusDays(1))
                .initialBalance(new BigDecimal("1000000"))
                .basePeriod(20)
                .buyThresholdPct(new BigDecimal("-6"))
                .sellTargetPct(new BigDecimal("4"))
                .stopLossPct(new BigDecimal("-8"))
                .maxHoldingsPerCoin(2)
                .useTrailingStop(true)
                .trailingStopPct(new BigDecimal("4"))
                .rsiPeriod(14)
	  .rsiBuyThreshold(32)
                .rsiSellThreshold(68)
                .bbPeriod(20)
                .bbMultiplier(2)
                .volumeThreshold(140)
                .dailyTradeLimitPct(20)
                .maxPositionPct(25)
                .dailyStopLossPct(-5)
                .buyAmountPct(10)
                .build();
        
        return ResponseEntity.ok(defaults);
    }
}