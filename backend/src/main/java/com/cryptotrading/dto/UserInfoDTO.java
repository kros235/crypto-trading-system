package com.cryptotrading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    
    private String userId;
    private String email;
    private String discordUserId;
    private String phone;
    private String role;
    private LocalDateTime joinDate;
    private LocalDateTime lastLogin;
    private Boolean isActive;
    private Boolean hasApiKey;	                  // API 키 등록 여부만 반환
    private List<String> allowedIps;	    // IP 화이트리스트 필드 추가
    private Boolean ipWhitelistEnabled;
    private Boolean twoFactorEnabled;
}