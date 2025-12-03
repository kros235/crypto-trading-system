package com.cryptotrading.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    
    private Long id;
    private String userId;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private boolean hasApiKey;
    private LocalDateTime joinDate;
    private LocalDateTime lastLogin;
    
    // 통계 정보
    private int totalTransactions;
    private int holdingCount;
}