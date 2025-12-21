package com.cryptotrading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 키 접두사
    private static final String TICKER_PREFIX = "ticker:";
    private static final String INDICATOR_PREFIX = "indicator:";
    private static final String ACTIVE_COINS_KEY = "coins:active";

    // 캐시 TTL
    private static final Duration TICKER_TTL = Duration.ofSeconds(30);      // 현재가: 30초
    private static final Duration INDICATOR_TTL = Duration.ofMinutes(5);    // 기술적 지표: 5분
    private static final Duration ACTIVE_COINS_TTL = Duration.ofHours(1);   // 활성 코인: 1시간

    /**
     * 현재가 캐시 저장
     */
    public void cacheTicker(String market, Object ticker) {
        try {
            String key = TICKER_PREFIX + market;
            String value = objectMapper.writeValueAsString(ticker);
            redisTemplate.opsForValue().set(key, value, TICKER_TTL);
            log.debug("캐시 저장: {} (TTL: {}초)", key, TICKER_TTL.getSeconds());
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (ticker): {}", e.getMessage());
        }
    }

    /**
     * 현재가 캐시 조회
     */
    public <T> Optional<T> getTicker(String market, Class<T> clazz) {
        try {
            String key = TICKER_PREFIX + market;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("캐시 히트: {}", key);
                return Optional.of(objectMapper.readValue(value, clazz));
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (ticker): {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 기술적 지표 캐시 저장
     */
    public void cacheIndicator(String market, Object indicator) {
        try {
            String key = INDICATOR_PREFIX + market;
            String value = objectMapper.writeValueAsString(indicator);
            redisTemplate.opsForValue().set(key, value, INDICATOR_TTL);
            log.debug("캐시 저장: {} (TTL: {}분)", key, INDICATOR_TTL.toMinutes());
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (indicator): {}", e.getMessage());
        }
    }

    /**
     * 기술적 지표 캐시 조회
     */
    public <T> Optional<T> getIndicator(String market, Class<T> clazz) {
        try {
            String key = INDICATOR_PREFIX + market;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("캐시 히트: {}", key);
                return Optional.of(objectMapper.readValue(value, clazz));
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (indicator): {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 활성 코인 목록 캐시 저장
     */
    public void cacheActiveCoins(Object coins) {
        try {
            String value = objectMapper.writeValueAsString(coins);
            redisTemplate.opsForValue().set(ACTIVE_COINS_KEY, value, ACTIVE_COINS_TTL);
            log.debug("캐시 저장: {} (TTL: {}시간)", ACTIVE_COINS_KEY, ACTIVE_COINS_TTL.toHours());
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (activeCoins): {}", e.getMessage());
        }
    }

    /**
     * 활성 코인 목록 캐시 조회
     */
    public <T> Optional<T> getActiveCoins(TypeReference<T> typeRef) {
        try {
            String value = redisTemplate.opsForValue().get(ACTIVE_COINS_KEY);
            if (value != null) {
                log.debug("캐시 히트: {}", ACTIVE_COINS_KEY);
                return Optional.of(objectMapper.readValue(value, typeRef));
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (activeCoins): {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 특정 캐시 삭제
     */
    public void evict(String key) {
        redisTemplate.delete(key);
        log.debug("캐시 삭제: {}", key);
    }

    /**
     * 패턴으로 캐시 삭제
     */
    public void evictByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("캐시 삭제 (패턴: {}): {}개", pattern, keys.size());
        }
    }

    /**
     * 모든 현재가 캐시 삭제
     */
    public void evictAllTickers() {
        evictByPattern(TICKER_PREFIX + "*");
    }

    /**
     * 모든 지표 캐시 삭제
     */
    public void evictAllIndicators() {
        evictByPattern(INDICATOR_PREFIX + "*");
    }
}