package com.cryptotrading.service;

import com.cryptotrading.dto.admin.MonitoringDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringAlertService {
    
    private final MonitoringService monitoringService;
    private final NotificationService notificationService;
    private final AdminAlertNotificationService adminAlertNotificationService;
    
    @Value("${app.monitoring.alert.admin-email:}")
    private String adminEmail;
    
    // 임계값 설정
    private static final double HEAP_USAGE_WARNING = 80.0;
    private static final double HEAP_USAGE_CRITICAL = 90.0;
    private static final int DB_POOL_WARNING = 8;
    private static final int ERROR_COUNT_WARNING = 10;
    
    // 알림 중복 방지 플래그
    private final AtomicBoolean heapWarningeSent = new AtomicBoolean(false);
    private final AtomicBoolean heapCriticalSent = new AtomicBoolean(false);
    private final AtomicBoolean dbWarningeSent = new AtomicBoolean(false);
    private final AtomicBoolean redisAlertSent = new AtomicBoolean(false);
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");  // ⭐ 추가
    
    /**
     * 5분마다 시스템 상태 점검
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void checkSystemHealth() {
        try {
            MonitoringDTO metrics = monitoringService.getSystemMetrics();
            
            checkHeapMemory(metrics);
            checkDbConnections(metrics);
            checkRedisConnection(metrics);
            checkErrorCount(metrics);
            
        } catch (Exception e) {
            log.error("시스템 상태 점검 중 오류: {}", e.getMessage());
        }
    }
    
    private void checkHeapMemory(MonitoringDTO metrics) {
        double heapUsage = metrics.getHeapUsagePercent();
        
        if (heapUsage >= HEAP_USAGE_CRITICAL) {
            if (!heapCriticalSent.getAndSet(true)) {
                String message = String.format(
                    "🚨 **[긴급] JVM 힙 메모리 위험**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "📊 사용률: **%.1f%%**\n" +
                    "💾 사용량: %s / %s\n" +
                    "🕐 시간: %s\n" +
                    "⚠️ 즉시 확인이 필요합니다!",
                    heapUsage,
                    formatBytes(metrics.getHeapUsed()),
                    formatBytes(metrics.getHeapMax()),
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                sendAlert("🚨 JVM 메모리 위험", message);
            }
        } else if (heapUsage >= HEAP_USAGE_WARNING) {
            if (!heapWarningeSent.getAndSet(true)) {
                String message = String.format(
                    "⚠️ **JVM 힙 메모리 경고**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "📊 사용률: **%.1f%%**\n" +
                    "💾 사용량: %s / %s\n" +
                    "🕐 시간: %s\n" +
                    "📈 메모리 사용량이 높습니다.",
                    heapUsage,
                    formatBytes(metrics.getHeapUsed()),
                    formatBytes(metrics.getHeapMax()),
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                sendAlert("⚠️ JVM 메모리 경고", message);
            }
        } else {
            // 정상으로 돌아오면 플래그 리셋
            heapWarningeSent.set(false);
            heapCriticalSent.set(false);
        }
    }
    
    private void checkDbConnections(MonitoringDTO metrics) {
        int activeConnections = metrics.getDbActiveConnections();
        int maxConnections = metrics.getDbMaxConnections();
        
        if (activeConnections >= maxConnections - 1) {
            if (!dbWarningeSent.getAndSet(true)) {
                String message = String.format(
                    "🚨 **[긴급] DB 커넥션 풀 고갈 위험**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔗 활성: **%d** / 최대: %d\n" +
                    "🕐 시간: %s\n" +
                    "⚠️ 커넥션 풀이 거의 소진되었습니다!",
                    activeConnections, maxConnections,
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                sendAlert("🚨 DB 커넥션 위험", message);
            }
        } else if (activeConnections >= DB_POOL_WARNING) {
            if (!dbWarningeSent.getAndSet(true)) {
                String message = String.format(
                    "⚠️ **DB 커넥션 풀 경고**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔗 활성: **%d** / 최대: %d\n" +
                    "🕐 시간: %s\n" +
                    "📈 커넥션 사용량이 높습니다.",
                    activeConnections, maxConnections,
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                sendAlert("⚠️ DB 커넥션 경고", message);
            }
        } else {
            dbWarningeSent.set(false);
        }
    }
    
    private void checkRedisConnection(MonitoringDTO metrics) {
        if (!metrics.isRedisConnected()) {
            if (!redisAlertSent.getAndSet(true)) {
                String message = String.format(
                    "🚨 **[긴급] Redis 연결 끊김**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "❌ Redis 서버와 연결이 끊어졌습니다.\n" +
                    "🕐 시간: %s\n" +
                    "⚠️ 캐싱 및 세션 관리에 문제가 발생할 수 있습니다!",
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                sendAlert("🚨 Redis 연결 끊김", message);
            }
        } else {
            redisAlertSent.set(false);
        }
    }
    
    private void checkErrorCount(MonitoringDTO metrics) {
        int errorCount = metrics.getRecentErrorCount();
        
        if (errorCount >= ERROR_COUNT_WARNING) {
            String message = String.format(
                "⚠️ **시스템 에러 다수 발생**\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "❌ 최근 1시간 에러: **%d건**\n" +
                "🕐 시간: %s\n" +
                "📋 로그를 확인해 주세요.",
                errorCount,
                LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
            );
            sendAlert("⚠️ 에러 다수 발생", message);
        }
    }
    
    /**
     * Discord Webhook 및 Admin 계정에게 알림 발송
     * Admin 이메일/Discord DM 알림 추가
     */
    private void sendAlert(String subject, String message) {
        try {
            // 1. 기존: Discord Webhook으로 시스템 알림 발송
            notificationService.sendSystemNotification(message);
            log.warn("모니터링 알림 발송 (Webhook): {}", subject);
            
            // 2. ⭐ 추가: Admin 계정들에게 이메일/Discord DM 알림 발송
            adminAlertNotificationService.sendAdminAlert(subject, message);
            
        } catch (Exception e) {
            log.error("모니터링 알림 발송 실패: {}", e.getMessage());
        }
    }

    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}