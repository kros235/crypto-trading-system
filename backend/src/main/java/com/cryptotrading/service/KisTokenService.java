package com.cryptotrading.service;

import com.cryptotrading.dto.kis.KisTokenDTO;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.repository.StockTradingSettingRepository;
import com.cryptotrading.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KIS API 토큰 관리 서비스
 * Phase 2 Day 50: OAuth 토큰 발급/갱신/캐싱
 * 
 * 토큰 관리 방식:
 * 1. 사용자별 토큰을 ConcurrentHashMap에 캐싱
 * 2. 토큰 유효시간(24h) 내에서 재사용
 * 3. 만료 시 자동 갱신
 */
@Service
@Slf4j
public class KisTokenService {

    private final WebClient kisWebClient;
    private final StockTradingSettingRepository stockTradingSettingRepository;
    private final EncryptionUtil encryptionUtil;

    @Value("${kis.api.base-url}")
    private String kisBaseUrl;

    @Value("${kis.token.refresh-hours:23}")
    private int tokenRefreshHours;

    /** 사용자별 토큰 캐시 (userId -> KisTokenDTO) */
    private final ConcurrentHashMap<String, KisTokenDTO> tokenCache = new ConcurrentHashMap<>();

    public KisTokenService(
            @Qualifier("kisWebClient") WebClient kisWebClient,
            StockTradingSettingRepository stockTradingSettingRepository,
            EncryptionUtil encryptionUtil) {
        this.kisWebClient = kisWebClient;
        this.stockTradingSettingRepository = stockTradingSettingRepository;
        this.encryptionUtil = encryptionUtil;
    }

    /**
     * 사용자의 유효한 접근 토큰 반환
     * - 캐시에 유효한 토큰이 있으면 반환
     * - 없거나 만료되었으면 새로 발급
     * 
     * @param userId 사용자 ID
     * @return 접근 토큰 문자열
     */
    public String getAccessToken(String userId) {
        KisTokenDTO cached = tokenCache.get(userId);

        if (cached != null && !cached.isExpired(tokenRefreshHours)) {
            log.debug("[KIS 토큰] 캐시 사용 - userId: {}", userId);
            return cached.getAccessToken();
        }

        log.info("[KIS 토큰] 새 토큰 발급 - userId: {}", userId);
        KisTokenDTO newToken = issueToken(userId);
        tokenCache.put(userId, newToken);
        return newToken.getAccessToken();
    }

    /**
     * OAuth 토큰 발급
     * - KIS API POST /oauth2/tokenP 호출
     * - 사용자의 암호화된 API 키를 복호화하여 사용
     */
    private KisTokenDTO issueToken(String userId) {
        StockTradingSetting setting = stockTradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정이 없습니다. userId: " + userId));

        String appKey = decryptSafe(setting.getKisAppKeyEncrypted(), "APP KEY");
        String appSecret = decryptSafe(setting.getKisAppSecretEncrypted(), "APP SECRET");

        if (appKey.isEmpty() || appSecret.isEmpty()) {
            throw new RuntimeException("KIS API 키가 등록되지 않았습니다. userId: " + userId);
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", appKey);
        requestBody.put("appsecret", appSecret);

        try {
            KisTokenDTO token = kisWebClient.post()
                    .uri("/oauth2/tokenP")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(KisTokenDTO.class)
                    .block();

            if (token == null || token.getAccessToken() == null) {
                throw new RuntimeException("KIS 토큰 발급 실패: 응답이 null입니다.");
            }

            token.setIssuedAt(LocalDateTime.now());
            log.info("[KIS 토큰] 발급 성공 - userId: {}, 만료: {}",
                    userId, token.getAccessTokenExpired());
            return token;

        } catch (WebClientResponseException e) {
            log.error("[KIS 토큰] 발급 실패 - userId: {}, 상태코드: {}, 응답: {}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("KIS 토큰 발급 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("[KIS 토큰] 발급 중 오류 - userId: {}, 오류: {}", userId, e.getMessage());
            throw new RuntimeException("KIS 토큰 발급 오류: " + e.getMessage());
        }
    }

    /**
     * 안전한 복호화 (null/빈값 처리)
     */
    private String decryptSafe(String encrypted, String fieldName) {
        if (encrypted == null || encrypted.trim().isEmpty()) {
            log.warn("[KIS 토큰] {} 값이 비어있습니다.", fieldName);
            return "";
        }
        try {
            return encryptionUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.error("[KIS 토큰] {} 복호화 실패: {}", fieldName, e.getMessage());
            return "";
        }
    }

    /**
     * 사용자의 AppKey 조회 (API 헤더에 필요)
     */
    public String getAppKey(String userId) {
        StockTradingSetting setting = stockTradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정이 없습니다. userId: " + userId));
        return decryptSafe(setting.getKisAppKeyEncrypted(), "APP KEY");
    }

    /**
     * 사용자의 AppSecret 조회 (API 헤더에 필요)
     */
    public String getAppSecret(String userId) {
        StockTradingSetting setting = stockTradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정이 없습니다. userId: " + userId));
        return decryptSafe(setting.getKisAppSecretEncrypted(), "APP SECRET");
    }

    /**
     * 사용자의 계좌번호 조회
     */
    public String getAccountNo(String userId) {
        StockTradingSetting setting = stockTradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정이 없습니다. userId: " + userId));
        return decryptSafe(setting.getKisAccountNoEncrypted(), "계좌번호");
    }

    /**
     * 사용자의 모의투자 모드 여부
     */
    public boolean isMockMode(String userId) {
        return stockTradingSettingRepository.findByUserId(userId)
                .map(StockTradingSetting::getKisMockMode)
                .orElse(true); // 기본값: 모의투자
    }

    /**
     * 특정 사용자 토큰 캐시 제거 (API 키 변경 시 호출)
     */
    public void invalidateToken(String userId) {
        tokenCache.remove(userId);
        log.info("[KIS 토큰] 캐시 제거 - userId: {}", userId);
    }

    /**
     * 전체 토큰 캐시 초기화
     */
    public void invalidateAllTokens() {
        tokenCache.clear();
        log.info("[KIS 토큰] 전체 캐시 초기화");
    }
}