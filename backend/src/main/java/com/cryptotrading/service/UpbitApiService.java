package com.cryptotrading.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.cryptotrading.dto.upbit.*;
import com.cryptotrading.dto.upbit.UpbitCandleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Collections;

import java.time.Duration;  
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpbitApiService {

    @Value("${upbit.api.url}")
    private String upbitApiUrl;

    // 재시도 설정 상수
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(500);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(5);
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.upbit.com/v1")
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8")
            .build();

    // 재시도 스펙 생성 메서드
    /**
     * API 호출 재시도 설정
     * - 최대 3회 재시도
     * - 지수 백오프 (500ms → 1s → 2s)
     * - 최대 대기시간 5초
     * - 5xx 서버 오류 및 연결 오류만 재시도
     */
    private Retry getRetrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, INITIAL_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .filter(throwable -> {
                    // 재시도 대상 오류 판단
                    if (throwable instanceof WebClientResponseException) {
                        int statusCode = ((WebClientResponseException) throwable).getStatusCode().value();
                        // 5xx 서버 오류, 429 Too Many Requests만 재시도
                        boolean shouldRetry = statusCode >= 500 || statusCode == 429;
                        if (shouldRetry) {
                            log.warn("업비트 API 오류 (재시도 예정): HTTP {} - {}", 
                                    statusCode, throwable.getMessage());
                        }
                        return shouldRetry;
                    }
                    // 네트워크 오류는 재시도
                    log.warn("업비트 API 연결 오류 (재시도 예정): {}", throwable.getMessage());
                    return true;
                })
                .doBeforeRetry(retrySignal -> {
                    log.info("업비트 API 재시도 #{} - 대기 후 재요청", 
                            retrySignal.totalRetries() + 1);
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("업비트 API 최대 재시도 횟수({}) 초과", MAX_RETRY_ATTEMPTS);
                    return retrySignal.failure();
                });
    }

    /**
     * JWT 토큰 생성 (업비트 API 인증용) - 사용자 API 키 사용
     */
    private String generateToken(String accessKey, String secretKey) {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new RuntimeException("업비트 API 키가 등록되지 않았습니다. 먼저 API 키를 등록해주세요.");
        }
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);
    }

    /**
     * JWT 토큰 생성 (쿼리 파라미터 포함) - 사용자 API 키 사용
     */
    private String generateToken(String accessKey, String secretKey, Map<String, String> params) {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new RuntimeException("업비트 API 키가 등록되지 않았습니다. 먼저 API 키를 등록해주세요.");
        }
        try {
            String queryString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");

            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(queryString.getBytes("UTF-8"));
            String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            return JWT.create()
                    .withClaim("access_key", accessKey)
                    .withClaim("nonce", UUID.randomUUID().toString())
                    .withClaim("query_hash", queryHash)
                    .withClaim("query_hash_alg", "SHA512")
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("JWT 토큰 생성 실패", e);
        }
    }

    /**
     * 1. 마켓 코드 조회 (공개 API)
     */
    public List<UpbitMarketDTO> getMarketAll() {
        log.info("업비트 마켓 코드 조회 시작");
        
        return webClient.get()
                .uri("/market/all?isDetails=true")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitMarketDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnSuccess(markets -> log.info("마켓 코드 {}개 조회 완료", markets.size()))
                .doOnError(error -> log.error("마켓 코드 조회 실패: {}", error.getMessage()))
                .block();
    }

    /**
     * 2. 현재가 정보 조회 (공개 API)
     */
    public List<UpbitTickerDTO> getTicker(List<String> markets) {
        log.info("현재가 조회: {}", markets);
        
        String marketsParam = String.join(",", markets);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ticker")
                        .queryParam("markets", marketsParam)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitTickerDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnSuccess(tickers -> log.info("현재가 {}개 조회 완료", tickers.size()))
                .doOnError(error -> log.error("현재가 조회 실패: {}", error.getMessage()))
                .block();
    }

    /**
     * 3. 계좌 조회 (인증 필요) - 사용자 API 키 사용
     */
    public List<UpbitAccountDTO> getAccounts(String accessKey, String secretKey) {
        log.info("계좌 조회 시작");
        
        String token = generateToken(accessKey, secretKey);
        
        return webClient.get()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitAccountDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnSuccess(accounts -> log.info("계좌 {}개 조회 완료", accounts.size()))
                .doOnError(error -> log.error("계좌 조회 실패: {}", error.getMessage()))
                .block();
    }

    /**
     * 4. 주문하기 - 매수 (인증 필요) - 사용자 API 키 사용
     */
    public UpbitOrderDTO orderBuy(String accessKey, String secretKey, String market, BigDecimal price) {
        log.info("매수 주문: market={}, price={}", market, price);
        
        Map<String, String> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("price", price.toString());
        params.put("ord_type", "price");
        
        String token = generateToken(accessKey, secretKey, params);
        
        return webClient.post()
                .uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(UpbitOrderDTO.class)
                .retryWhen(getRetrySpec())
                .doOnSuccess(order -> log.info("매수 주문 완료: uuid={}", order.getUuid()))
                .doOnError(error -> log.error("매수 주문 실패: {}", error.getMessage()))
                .block();
    }

    /**
     * 5. 주문하기 - 매도 (인증 필요) - 사용자 API 키 사용
     */
    public UpbitOrderDTO orderSell(String accessKey, String secretKey, String market, BigDecimal volume) {
        log.info("매도 주문: market={}, volume={}", market, volume);
        
        Map<String, String> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "ask");
        params.put("volume", volume.toString());
        params.put("ord_type", "market");
        
        String token = generateToken(accessKey, secretKey, params);
        
        return webClient.post()
                .uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(UpbitOrderDTO.class)
                .retryWhen(getRetrySpec()) 
                .doOnSuccess(order -> log.info("매도 주문 완료: uuid={}", order.getUuid()))
                .doOnError(error -> log.error("매도 주문 실패: {}", error.getMessage()))
                .block();
    }

     /**
     * ⭐⭐⭐ 신규: 주문 조회 (인증 필요) - 체결 수량 확인용 ⭐⭐⭐
     */
    public UpbitOrderDTO getOrder(String accessKey, String secretKey, String uuid) {
        log.info("주문 조회: uuid={}", uuid);
        
        Map<String, String> params = new HashMap<>();
        params.put("uuid", uuid);
        
        String token = generateToken(accessKey, secretKey, params);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/order")
                        .queryParam("uuid", uuid)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(UpbitOrderDTO.class)
                .retryWhen(getRetrySpec())
                .doOnSuccess(order -> log.info("주문 조회 완료: uuid={}, state={}, executedVolume={}", 
                        order.getUuid(), order.getState(), order.getExecutedVolume()))
                .doOnError(error -> log.error("주문 조회 실패: {}", error.getMessage()))
                .block();
    }


    /**
     * 6. 주문 취소 (인증 필요) - 사용자 API 키 사용
     */
    public UpbitOrderDTO cancelOrder(String accessKey, String secretKey, String uuid) {
        log.info("주문 취소: uuid={}", uuid);
        
        Map<String, String> params = new HashMap<>();
        params.put("uuid", uuid);
        
        String token = generateToken(accessKey, secretKey, params);
        
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/order")
                        .queryParam("uuid", uuid)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(UpbitOrderDTO.class)
                .retryWhen(getRetrySpec()) 
                .doOnSuccess(order -> log.info("주문 취소 완료: uuid={}", order.getUuid()))
                .doOnError(error -> log.error("주문 취소 실패: {}", error.getMessage()))
                .block();
    }
   
    /**
     * 7. 일봉 캔들 조회 (공개 API) - 기술적 지표 계산용
     */
    public List<UpbitCandleDTO> getDayCandles(String market, int count) {
        log.info("일봉 조회: market={}, count={}", market, count);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/candles/days")
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitCandleDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnSuccess(candles -> log.info("일봉 {}개 조회 완료: {}", candles.size(), market))
                .doOnError(error -> log.error("일봉 조회 실패: {}", error.getMessage()))
                .block();
    }

    /**
     * ★★★ 신규: 일봉 캔들 조회 (특정 시간 기준) - 페이징용 ★★★
     */
    public List<UpbitCandleDTO> getDayCandlesWithTo(String market, int count, String to) {
        log.debug("일봉 캔들 조회 (to 기준): {} - {}개, to={}", market, count, to);
    
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/candles/days")
                        .queryParam("market", market)
                        .queryParam("count", Math.min(count, 200))
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitCandleDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnError(error -> log.error("일봉 캔들 조회 실패: {} - {}", market, error.getMessage()))
                .block();
    }
    
    /**
     * 8. 분봉 캔들 조회 (공개 API) - 실시간 모니터링용
     */
    public List<UpbitCandleDTO> getMinuteCandles(String market, int unit, int count) {
        log.info("분봉 조회: market={}, unit={}분, count={}", market, unit, count);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/candles/minutes/" + unit)
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpbitCandleDTO>>() {})
                .retryWhen(getRetrySpec())
                .doOnSuccess(candles -> log.info("분봉 {}개 조회 완료: {}", candles.size(), market))
                .doOnError(error -> log.error("분봉 조회 실패: {}", error.getMessage()))
                .block();
    }   
}