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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinInfoService {

    private final CoinInfoRepository coinInfoRepository;
    private final UpbitApiService upbitApiService;
    private final CacheService cacheService;

    /**
     * 코인 정보 초기화/업데이트 (수동 실행 또는 스케줄링)
     */
    @Transactional
    public void updateCoinInfo() {
        log.info("코인 정보 업데이트 시작");
        
        List<UpbitMarketDTO> markets = upbitApiService.getMarketAll();
        
        // KRW 마켓만 필터링
        List<UpbitMarketDTO> krwMarkets = markets.stream()
                .filter(market -> market.getMarket().startsWith("KRW-"))
                .collect(Collectors.toList());
        
        for (UpbitMarketDTO market : krwMarkets) {
            CoinInfo coinInfo = coinInfoRepository.findById(market.getMarket())
                    .orElse(CoinInfo.builder()
                            .symbol(market.getMarket())
                            .build());
            
            coinInfo.setNameKr(market.getKoreanName());
            coinInfo.setNameEn(market.getEnglishName());
            coinInfo.setIsActive(!"CAUTION".equals(market.getMarketWarning()));
            
            coinInfoRepository.save(coinInfo);
        }
        cacheService.evict("coins:active");
        
        log.info("코인 정보 {}개 업데이트 완료", krwMarkets.size());
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
     * 매일 새벽 4시에 코인 정보 자동 업데이트
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledUpdate() {
        log.info("스케줄된 코인 정보 업데이트 실행");
        updateCoinInfo();
    }
}