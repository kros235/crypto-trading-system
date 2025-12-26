package com.cryptotrading.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDTO {
    
    // JVM 메트릭
    private long heapUsed;
    private long heapMax;
    private double heapUsagePercent;
    private long nonHeapUsed;
    
    // 시스템 메트릭
    private int availableProcessors;
    private double systemLoadAverage;
    private long uptimeSeconds;
    private String uptimeFormatted;
    
    // 스레드 정보
    private int threadCount;
    private int peakThreadCount;
    private int daemonThreadCount;
    
    // 데이터베이스 풀 상태
    private int dbActiveConnections;
    private int dbIdleConnections;
    private int dbTotalConnections;
    private int dbMaxConnections;
    
    // Redis 상태
    private boolean redisConnected;
    private long redisUsedMemory;
    private int redisConnectedClients;
    
    // 최근 슬로우 쿼리
    private List<SlowQueryInfo> recentSlowQueries;
    
    // API 응답 시간 통계
    private Map<String, Double> apiResponseTimes;
    
    // 거래 통계 (최근 1시간)
    private int recentBuyCount;
    private int recentSellCount;
    private int recentErrorCount;
    
    // 타임스탬프
    private LocalDateTime collectedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowQueryInfo {
        private String query;
        private long executionTimeMs;
        private LocalDateTime executedAt;
        private String source;
    }
}