package com.cryptotrading.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.cryptotrading.dto.upbit.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpbitApiService {

    @Value("${upbit.api.url}")
    private String upbitApiUrl;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.upbit.com/v1")
            .build();

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
                .doOnSuccess(order -> log.info("매도 주문 완료: uuid={}", order.getUuid()))
                .doOnError(error -> log.error("매도 주문 실패: {}", error.getMessage()))
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
                .doOnSuccess(order -> log.info("주문 취소 완료: uuid={}", order.getUuid()))
                .doOnError(error -> log.error("주문 취소 실패: {}", error.getMessage()))
                .block();
    }
}