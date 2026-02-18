package com.cryptotrading.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * KIS API OAuth 토큰 DTO
 * Phase 2 Day 50: KIS API 토큰 발급/갱신
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisTokenDTO {

    /** 접근 토큰 */
    @JsonProperty("access_token")
    private String accessToken;

    /** 토큰 타입 (Bearer) */
    @JsonProperty("token_type")
    private String tokenType;

    /** 토큰 만료일시 (yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("access_token_token_expired")
    private String accessTokenExpired;

    /** 토큰 유효기간 (초) */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** 토큰 발급 시각 (내부 관리용) */
    private LocalDateTime issuedAt;

    /**
     * 토큰 만료 여부 확인
     * @param refreshHours 갱신 기준 시간 (기본 23시간)
     */
    public boolean isExpired(int refreshHours) {
        if (issuedAt == null) return true;
        return LocalDateTime.now().isAfter(issuedAt.plusHours(refreshHours));
    }
}