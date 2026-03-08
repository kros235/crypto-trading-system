package com.cryptotrading.controller;

import com.cryptotrading.dto.backtest.BacktestRequestDTO;
import com.cryptotrading.dto.backtest.BacktestResultDTO;
import com.cryptotrading.service.BacktestService;
import com.cryptotrading.service.UpbitApiService;
import com.cryptotrading.repository.CoinInfoRepository;

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
    private final UpbitApiService upbitApiService;
private final CoinInfoRepository coinInfoRepository;  // ★★★ 추가: DB 시총순위 조회용 ★★★

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

        // ★★★ 수정 후 - getAvailableCoins() 메서드 ★★★
    /**
     * 전체 코인 목록 (DB의 시가총액 순위 기준 정렬 - 코인 목록 페이지와 동일 기준)
     * ★★★ 변경: 24시간 거래대금 기준 → DB market_cap_rank 기준으로 변경 ★★★
     * - 이유: BacktestView의 코인 순위가 CoinListView의 시총순위와 일치하도록 통일
     */
    @GetMapping("/available-coins")
    public ResponseEntity<Map<String, Object>> getAvailableCoins() {
        try {
            // 업비트에서 전체 KRW 마켓 코인 조회 (한글명 확보용)
            List<UpbitMarketDTO> allMarkets = upbitApiService.getMarketAll();

            // ★★★ 변경: DB의 market_cap_rank 기준으로 정렬 (코인 목록 페이지와 동일) ★★★
            // CoinGecko 기준 시총순위가 저장된 DB를 활용
            List<com.cryptotrading.entity.CoinInfo> dbCoins = coinInfoRepository.findAll();

            // market_cap_rank 기준 정렬: 순위 있는 것 먼저(오름차순), null은 마지막
            dbCoins.sort((a, b) -> {
                if (a.getMarketCapRank() == null && b.getMarketCapRank() == null) return 0;
                if (a.getMarketCapRank() == null) return 1;
                if (b.getMarketCapRank() == null) return -1;
                return a.getMarketCapRank().compareTo(b.getMarketCapRank());
            });

            // 한글명 맵 구성 (업비트 마켓 정보 기반)
            Map<String, String> koreanNameMap = allMarkets.stream()
                    .filter(m -> m.getMarket().startsWith("KRW-"))
                    .collect(java.util.stream.Collectors.toMap(
                            UpbitMarketDTO::getMarket,
                            UpbitMarketDTO::getKoreanName,
                            (existing, replacement) -> existing
                    ));

            // DTO 변환 (활성 코인만 포함)
            List<Map<String, Object>> coins = new ArrayList<>();
            for (com.cryptotrading.entity.CoinInfo dbCoin : dbCoins) {
                if (Boolean.FALSE.equals(dbCoin.getIsActive())) continue; // 비활성 코인 제외

                String symbol = dbCoin.getSymbol();
                // DB 한글명 우선, 없으면 업비트 마켓 한글명, 없으면 심볼에서 추출
                String koreanName = dbCoin.getNameKr() != null && !dbCoin.getNameKr().isEmpty()
                        ? dbCoin.getNameKr()
                        : koreanNameMap.getOrDefault(symbol, symbol.replace("KRW-", ""));

                Map<String, Object> coin = new HashMap<>();
                coin.put("symbol", symbol);
                coin.put("name", koreanName);
                // ★★★ 변경: DB의 market_cap_rank를 rank로 사용 (null이면 "-" 표시용 null 유지) ★★★
                coin.put("rank", dbCoin.getMarketCapRank());
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
                // ⭐⭐⭐ 수정: buyAmountPct → fixedBuyAmount, useRoundRobin 추가 ⭐⭐⭐
                .fixedBuyAmount(new BigDecimal("10000"))  // 기본 1회 매수 금액: 10,000원
                .useRoundRobin(true)                      // 기본 매수 방식: 라운드로빈
                .build();
        
        return ResponseEntity.ok(defaults);
    }
}