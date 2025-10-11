package com.cryptotrading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    
    private String userId;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime joinDate;
    private LocalDateTime lastLogin;
    private Boolean isActive;
    private Boolean hasApiKey; // API 키 등록 여부만 반환
}