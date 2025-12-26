package com.cryptotrading.service;

import com.cryptotrading.dto.admin.MonitoringDTO;
import com.cryptotrading.dto.admin.MonitoringDTO.SlowQueryInfo;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    
    // 슬로우 쿼리 저장 (최근 100개)
    private final Deque<SlowQueryInfo> slowQueries = new ConcurrentLinkedDeque<>();
    private static final int MAX_SLOW_QUERIES = 100;
    
    // API 응답 시간 통계
    private final Map<String, List<Long>> apiResponseTimes = new ConcurrentHashMap<>();
    
    // 최근 에러 카운트
    private final AtomicInteger recentErrorCount = new AtomicInteger(0);
    
    // 슬로우 쿼리 임계값 (밀리초)
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    /**
     * 시스템 모니터링 데이터 수집
     */
    public MonitoringDTO getSystemMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // JVM 메모리
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // 업타임
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMs / 1000;
        String uptimeFormatted = formatUptime(uptimeSeconds);
        
        // DB 커넥션 풀
        int dbActive = 0, dbIdle = 0, dbTotal = 0, dbMax = 10;
        if (dataSource instanceof HikariDataSource hikari) {
            dbActive = hikari.getHikariPoolMXBean() != null ? 
                hikari.getHikariPoolMXBean().getActiveConnections() : 0;
            dbIdle = hikari.getHikariPoolMXBean() != null ? 
                hikari.getHikariPoolMXBean().getIdleConnections() : 0;
            dbTotal = hikari.getHikariPoolMXBean() != null ? 
                hikari.getHikariPoolMXBean().getTotalConnections() : 0;
            dbMax = hikari.getMaximumPoolSize();
        }
        
        // Redis 상태
        boolean redisConnected = false;
        long redisUsedMemory = 0;
        int redisClients = 0;
        try {
            redisConnectionFactory.getConnection().ping();
            redisConnected = true;
            
            Properties info = redisConnectionFactory.getConnection().serverCommands().info("memory");
            if (info != null) {
                String usedMemoryStr = info.getProperty("used_memory");
                if (usedMemoryStr != null) {
                    redisUsedMemory = Long.parseLong(usedMemoryStr);
                }
            }
            
            Properties clientInfo = redisConnectionFactory.getConnection().serverCommands().info("clients");
            if (clientInfo != null) {
                String clientsStr = clientInfo.getProperty("connected_clients");
                if (clientsStr != null) {
                    redisClients = Integer.parseInt(clientsStr);
                }
            }
        } catch (Exception e) {
            log.warn("Redis 상태 확인 실패: {}", e.getMessage());
        }
        
        // API 응답 시간 평균
        Map<String, Double> avgResponseTimes = calculateAverageResponseTimes();
        
        // 최근 슬로우 쿼리 (최근 10개)
        List<SlowQueryInfo> recentSlowQueries = new ArrayList<>();
        Iterator<SlowQueryInfo> iter = slowQueries.descendingIterator();
        int count = 0;
        while (iter.hasNext() && count < 10) {
            recentSlowQueries.add(iter.next());
            count++;
        }
        
        return MonitoringDTO.builder()
                .heapUsed(heapUsed)
                .heapMax(heapMax)
                .heapUsagePercent(Math.round(heapUsagePercent * 100.0) / 100.0)
                .nonHeapUsed(nonHeapUsed)
                .availableProcessors(osBean.getAvailableProcessors())
                .systemLoadAverage(osBean.getSystemLoadAverage())
                .uptimeSeconds(uptimeSeconds)
                .uptimeFormatted(uptimeFormatted)
                .threadCount(threadBean.getThreadCount())
                .peakThreadCount(threadBean.getPeakThreadCount())
                .daemonThreadCount(threadBean.getDaemonThreadCount())
                .dbActiveConnections(dbActive)
                .dbIdleConnections(dbIdle)
                .dbTotalConnections(dbTotal)
                .dbMaxConnections(dbMax)
                .redisConnected(redisConnected)
                .redisUsedMemory(redisUsedMemory)
                .redisConnectedClients(redisClients)
                .recentSlowQueries(recentSlowQueries)
                .apiResponseTimes(avgResponseTimes)
                .recentErrorCount(recentErrorCount.get())
                .collectedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 슬로우 쿼리 기록
     */
    public void recordSlowQuery(String query, long executionTimeMs, String source) {
        if (executionTimeMs >= SLOW_QUERY_THRESHOLD_MS) {
            SlowQueryInfo slowQuery = SlowQueryInfo.builder()
                    .query(truncateQuery(query))
                    .executionTimeMs(executionTimeMs)
                    .executedAt(LocalDateTime.now())
                    .source(source)
                    .build();
            
            slowQueries.addLast(slowQuery);
            
            // 최대 개수 초과 시 오래된 것 제거
            while (slowQueries.size() > MAX_SLOW_QUERIES) {
                slowQueries.pollFirst();
            }
            
            log.warn("[SLOW QUERY] {}ms - {} (source: {})", executionTimeMs, truncateQuery(query), source);
            
            // 3초 이상 쿼리는 Discord 알림
            if (executionTimeMs >= 3000) {
                sendSlowQueryAlert(slowQuery);
            }
        }
    }
    
    /**
     * API 응답 시간 기록
     */
    public void recordApiResponseTime(String endpoint, long responseTimeMs) {
        apiResponseTimes.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(responseTimeMs);
        
        // 각 엔드포인트당 최근 100개만 유지
        List<Long> times = apiResponseTimes.get(endpoint);
        if (times.size() > 100) {
            times.remove(0);
        }
    }
    
    /**
     * 에러 카운트 증가
     */
    public void incrementErrorCount() {
        recentErrorCount.incrementAndGet();
    }
    
    /**
     * 매 시간 에러 카운트 리셋
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void resetHourlyErrorCount() {
        int prevCount = recentErrorCount.getAndSet(0);
        if (prevCount > 0) {
            log.info("지난 1시간 에러 카운트: {}", prevCount);
        }
    }
    
    /**
     * 슬로우 쿼리 알림 발송
     */
    private void sendSlowQueryAlert(SlowQueryInfo slowQuery) {
        try {
            String message = String.format(
                "🐢 **슬로우 쿼리 감지**\n" +
                "⏱️ 실행시간: %dms\n" +
                "📍 Source: %s\n" +
                "📝 Query: ```%s```",
                slowQuery.getExecutionTimeMs(),
                slowQuery.getSource(),
                slowQuery.getQuery()
            );
            // Discord 알림 대신 로그로 기록 (NotificationService 확장 필요 시 별도 구현)
            log.warn("[SLOW QUERY ALERT] {}", message.replace("\n", " | "));
        } catch (Exception e) {
            log.error("슬로우 쿼리 알림 처리 실패: {}", e.getMessage());
        }
    }
    
    /**
     * API 평균 응답 시간 계산
     */
    private Map<String, Double> calculateAverageResponseTimes() {
        Map<String, Double> averages = new LinkedHashMap<>();
        apiResponseTimes.forEach((endpoint, times) -> {
            if (!times.isEmpty()) {
                double avg = times.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                averages.put(endpoint, Math.round(avg * 100.0) / 100.0);
            }
        });
        return averages;
    }
    
    /**
     * 쿼리 문자열 자르기
     */
    private String truncateQuery(String query) {
        if (query == null) return "";
        query = query.replaceAll("\\s+", " ").trim();
        return query.length() > 200 ? query.substring(0, 200) + "..." : query;
    }
    
    /**
     * 업타임 포맷팅
     */
    private String formatUptime(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long secs = seconds % 60;
        
        if (days > 0) {
            return String.format("%d일 %d시간 %d분", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d시간 %d분 %d초", hours, minutes, secs);
        } else {
            return String.format("%d분 %d초", minutes, secs);
        }
    }
}