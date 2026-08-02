package com.cryptotrading.service;

import com.cryptotrading.dto.upbit.UpbitMarketDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.CoinInfo;
import com.cryptotrading.repository.CoinInfoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

// ★★★ 추가: EventListener import ★★★
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

// ★★★ [추가] 시가총액 순위 갱신용 import ★★★
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinInfoService {

    private final CoinInfoRepository coinInfoRepository;
    private final UpbitApiService upbitApiService;
    private final CacheService cacheService;

    // ⭐⭐⭐ [신규] Top10 자동 리밸런싱 서비스 ⭐⭐⭐
    private final Top10RebalanceService top10RebalanceService;

    /**
     * 코인 정보 초기화/업데이트 (수동 실행 또는 스케줄링)
     * ★★★ 수정: 업비트에 없는 코인 비활성화 로직 추가 ★★★
     */
    @Transactional
    public void updateCoinInfo() {
        log.info("코인 정보 업데이트 시작");
        
        List<UpbitMarketDTO> markets = upbitApiService.getMarketAll();
        
        // KRW 마켓만 필터링
        List<UpbitMarketDTO> krwMarkets = markets.stream()
                .filter(market -> market.getMarket().startsWith("KRW-"))
                .collect(Collectors.toList());
        
        // ★★★ 신규: 업비트에서 받은 심볼 Set 생성 ★★★
        Set<String> upbitSymbols = new HashSet<>();
        
        for (UpbitMarketDTO market : krwMarkets) {
            upbitSymbols.add(market.getMarket());
            
            CoinInfo coinInfo = coinInfoRepository.findById(market.getMarket())
                    .orElse(CoinInfo.builder()
                            .symbol(market.getMarket())
                            .build());
            
            coinInfo.setNameKr(market.getKoreanName());
            coinInfo.setNameEn(market.getEnglishName());
            coinInfo.setIsActive(!"CAUTION".equals(market.getMarketWarning()));
            
            coinInfoRepository.save(coinInfo);
        }
        
        // ★★★ 신규: 업비트에 없는 코인 비활성화 (MATIC → POL 등 심볼 변경 대응) ★★★
        List<CoinInfo> allCoins = coinInfoRepository.findAll();
        int deactivatedCount = 0;
        for (CoinInfo coin : allCoins) {
            if (!upbitSymbols.contains(coin.getSymbol()) && coin.getIsActive()) {
                coin.setIsActive(false);
                coinInfoRepository.save(coin);
                log.info("업비트에 없는 코인 비활성화: {} ({})", coin.getSymbol(), coin.getNameKr());
                deactivatedCount++;
            }
        }
        
        cacheService.evict("coins:active");
        
        log.info("코인 정보 {}개 업데이트, {}개 비활성화 완료", krwMarkets.size(), deactivatedCount);
    }

    /**
     * 활성화된 코인 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CoinInfo> getActiveCoins() {
        // ⭐⭐⭐ [추가] 캐시 우선 조회 시작 ⭐⭐⭐
        Optional<List<CoinInfo>> cached = cacheService.getActiveCoins(
                new TypeReference<List<CoinInfo>>() {}
        );
        if (cached.isPresent()) {
            log.debug("활성 코인 목록 캐시 히트");
            return cached.get();
        }
        // ⭐⭐⭐ [추가] 캐시 우선 조회 끝 ⭐⭐⭐
        
        List<CoinInfo> coins = coinInfoRepository.findByIsActiveOrderByMarketCapRank(true);
        
        // ⭐⭐⭐ [추가] 캐시 저장 ⭐⭐⭐
        cacheService.cacheActiveCoins(coins);
        
        return coins;
    }

    /**
     * 특정 코인의 현재가 조회
     */
    public UpbitTickerDTO getCurrentPrice(String symbol) {
        // ⭐⭐⭐ [추가] 캐시 우선 조회 시작 ⭐⭐⭐
        Optional<UpbitTickerDTO> cached = cacheService.getTicker(symbol, UpbitTickerDTO.class);
        if (cached.isPresent()) {
            log.debug("현재가 캐시 히트: {}", symbol);
            return cached.get();
        }
        // ⭐⭐⭐ [추가] 캐시 우선 조회 끝 ⭐⭐⭐
        
        List<UpbitTickerDTO> tickers = upbitApiService.getTicker(List.of(symbol));
        
        if (tickers.isEmpty()) {
            throw new RuntimeException("코인 정보를 찾을 수 없습니다: " + symbol);
        }
        
        UpbitTickerDTO ticker = tickers.get(0);
        
        // ⭐⭐⭐ [추가] 캐시 저장 ⭐⭐⭐
        cacheService.cacheTicker(symbol, ticker);
        
        return ticker;
    }

    /**
     * ★★★ 신규: 애플리케이션 시작 시 자동으로 코인 정보 업데이트 ★★★
     * - Spring 컨텍스트 완전 초기화 후 실행
     * - MATIC → POL 등 심볼 변경 자동 반영
     * - 실패해도 애플리케이션은 정상 작동
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCoinInfo() {
        log.info("========== 애플리케이션 시작 완료: 코인 정보 자동 업데이트 시작 ==========");
        try {
            // 3초 대기 (추가 안정화)
            Thread.sleep(3000);
            updateCoinInfo();
            log.info("========== 코인 정보 자동 업데이트 완료 ==========");
        } catch (Exception e) {
            log.warn("시작 시 코인 정보 업데이트 실패 (무시하고 계속 진행): {}", e.getMessage());
            // 실패해도 애플리케이션은 정상 작동 - 기존 DB 데이터 사용
        }
    }

    /**
     * 매일 새벽 4시에 코인 정보 자동 업데이트
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledUpdate() {
        log.info("스케줄된 코인 정보 업데이트 실행");
        updateCoinInfo();
         // ★ 추가: 코인 정보 갱신 후 시가총액 순위도 갱신
         try {
              updateMarketCapRanks();
         } catch (Exception e) {
                 log.warn("스케줄된 시가총액 순위 갱신 실패 (무시): {}", e.getMessage());
         }
         // ⭐⭐⭐ [신규] 시가총액 순위 갱신 직후 Top10 자동 운영 사용자 리밸런싱 ⭐⭐⭐
         try {
             top10RebalanceService.rebalanceAllUsers();
         } catch (Exception e) {
             log.warn("Top10 자동 리밸런싱 실패 (무시): {}", e.getMessage());
         }
    }

    /**
     * ★★★ 신규: CoinGecko API로 시가총액 순위 갱신 ★★★
     * - CoinGecko 무료 API로 글로벌 시가총액 순위 조회
     * - 업비트 상장 코인만 필터링하여 재순위 매김
     * - 예: 글로벌 1위 BTC, 3위 USDT(업비트 미상장), 2위 ETH → 업비트 내 순위: BTC=1, ETH=2
     * - 조회 불가 코인은 market_cap_rank = null
     */
    @Transactional
    public Map<String, Object> updateMarketCapRanks() {
        log.info("========== 시가총액 순위 갱신 시작 (CoinGecko API) ==========");
        
        try {
            // 1. CoinGecko API로 글로벌 시가총액 Top 250 조회 (무료, API키 불필요)
            WebClient coinGeckoClient = WebClient.builder()
                    .baseUrl("https://api.coingecko.com/api/v3")
                    .build();
            
            List<Map<String, Object>> globalMarkets = coinGeckoClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/coins/markets")
                            .queryParam("vs_currency", "usd")
                            .queryParam("order", "market_cap_desc")
                            .queryParam("per_page", 250)
                            .queryParam("page", 1)
                            .queryParam("sparkline", false)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
            
            if (globalMarkets == null || globalMarkets.isEmpty()) {
                log.warn("CoinGecko API 응답이 비어있습니다");
                return Map.of("success", false, "message", "CoinGecko API 응답 없음");
            }
            
            log.info("CoinGecko에서 {}개 코인 시가총액 데이터 조회 완료", globalMarkets.size());
            
            // 2. 글로벌 심볼 → market_cap_rank 매핑 (CoinGecko symbol은 소문자)
            // CoinGecko symbol(소문자) → 글로벌 순위 매핑
            // 예: {"btc": 1, "eth": 2, "usdt": 3, "xrp": 4, ...}
            Map<String, Integer> globalRankMap = new java.util.LinkedHashMap<>();
            for (Map<String, Object> coin : globalMarkets) {
                String symbol = ((String) coin.get("symbol")).toLowerCase();
                Object rankObj = coin.get("market_cap_rank");
                if (rankObj != null) {
                    int rank = rankObj instanceof Integer ? (Integer) rankObj : ((Number) rankObj).intValue();
                    // 동일 심볼이 여러 개면 순위가 높은 것(먼저 나온 것)만 유지
                    globalRankMap.putIfAbsent(symbol, rank);
                }
            }
            
            // 3. 업비트 활성 코인 목록 조회
            List<CoinInfo> activeCoins = coinInfoRepository.findAll();
            
            // 4. 업비트 코인 중 CoinGecko에 매칭되는 것만 골라서, 글로벌 순위 순으로 정렬
            // 그 후 업비트 내부 순위를 1,2,3... 으로 재매핑
            List<CoinInfo> matchedCoins = new java.util.ArrayList<>();
            List<CoinInfo> unmatchedCoins = new java.util.ArrayList<>();
            
            for (CoinInfo coin : activeCoins) {
                // KRW-BTC → btc (소문자)
                String upbitSymbol = coin.getSymbol().replace("KRW-", "").toLowerCase();
                
                if (globalRankMap.containsKey(upbitSymbol)) {
                    // 임시로 글로벌 순위를 저장 (나중에 재매핑)
                    coin.setMarketCapRank(globalRankMap.get(upbitSymbol));
                    matchedCoins.add(coin);
                } else {
                    // CoinGecko에서 찾을 수 없는 코인 → null
                    coin.setMarketCapRank(null);
                    unmatchedCoins.add(coin);
                }
            }
            
            // 5. 매칭된 코인을 글로벌 순위 기준으로 정렬한 후, 업비트 내 순위 재매핑 (1,2,3...)
            matchedCoins.sort((a, b) -> {
                if (a.getMarketCapRank() == null && b.getMarketCapRank() == null) return 0;
                if (a.getMarketCapRank() == null) return 1;
                if (b.getMarketCapRank() == null) return -1;
                return a.getMarketCapRank().compareTo(b.getMarketCapRank());
            });
            
            AtomicInteger upbitRank = new AtomicInteger(1);
            for (CoinInfo coin : matchedCoins) {
                coin.setMarketCapRank(upbitRank.getAndIncrement());
                coinInfoRepository.save(coin);
            }
            
            // 6. 매칭 안 된 코인은 null로 저장
            for (CoinInfo coin : unmatchedCoins) {
                coin.setMarketCapRank(null);
                coinInfoRepository.save(coin);
            }
            
            // 7. 캐시 초기화 (갱신된 순위가 반영되도록)
            cacheService.evict("coins:active");
            
            log.info("========== 시가총액 순위 갱신 완료: 매칭 {}개, 미매칭 {}개 ==========", 
                    matchedCoins.size(), unmatchedCoins.size());
            
            // 상위 10개 로그 출력
            matchedCoins.stream().limit(10).forEach(coin -> 
                log.info("  순위 {}: {} ({})", coin.getMarketCapRank(), coin.getSymbol(), coin.getNameKr()));
            
            return Map.of(
                    "success", true,
                    "message", "시가총액 순위 갱신 완료",
                    "matchedCount", matchedCoins.size(),
                    "unmatchedCount", unmatchedCoins.size(),
                    "top10", matchedCoins.stream().limit(10)
                            .map(c -> Map.of("rank", c.getMarketCapRank(), "symbol", c.getSymbol(), "name", c.getNameKr()))
                            .collect(Collectors.toList())
            );
            
        } catch (Exception e) {
            log.error("시가총액 순위 갱신 실패: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "순위 갱신 실패: " + e.getMessage());
        }
    }
}