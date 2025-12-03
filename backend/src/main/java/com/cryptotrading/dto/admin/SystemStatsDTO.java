package com.cryptotrading.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDTO {
    
    // 사용자 통계
    private int totalUsers;
    private int activeUsers;
    private int usersWithApiKey;
    
    // 거래 통계
    private int todayBuyCount;
    private int todaySellCount;
    private BigDecimal todayTotalVolume;
    
    // 전체 통계
    private int totalTransactions;
    private BigDecimal totalInvestment;
    private BigDecimal totalProfit;
    
    // 시스템 상태
    private boolean botRunning;
    private LocalDateTime lastBotExecution;
    private boolean discordEnabled;
    private boolean emailEnabled;
    private String systemStatus;
    
    // 서버 정보
    private long uptime;
    private double memoryUsage;
    private double cpuUsage;
}