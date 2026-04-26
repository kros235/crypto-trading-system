package com.cryptotrading.config;

import com.cryptotrading.service.AdminAlertNotificationService;
import com.cryptotrading.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StartupNotificationConfig {
    
    private final NotificationService notificationService;
    private final AdminAlertNotificationService adminAlertNotificationService;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    
    @PostConstruct
    public void onStartup() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                
                String subject = "✅ 서버 시작 완료";
                String message = String.format(
                    "✅ **서버 시작 완료**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🕐 시간: %s\n" +
                    "🖥️ 서비스: 코인 & 주식 자동매매 시스템\n" +
                    "📊 상태: 정상 운영 중",
                    LocalDateTime.now(KST).format(formatter)
                );
                
                // Discord Webhook 알림
                notificationService.sendSystemNotification(message);
                log.info("서버 시작 알림 발송 완료 (Webhook)");
                
                // Admin 계정에게 이메일/Discord DM 알림
                adminAlertNotificationService.sendAdminAlert(subject, message);
                log.info("서버 시작 알림 발송 완료 (Admin)");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("서버 시작 알림 발송 실패: {}", e.getMessage());
            }
        }).start();
    }
    
    @PreDestroy
    public void onShutdown() {
        log.info("서버 종료 감지 - 알림 발송 시작...");
        
        try {
            String subject = "🛑 서버 종료";
            String message = String.format(
                "🛑 **서버 종료**\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "🕐 시간: %s\n" +
                "🖥️ 서비스: 코인 & 주식 자동매매 시스템\n" +
                "⚠️ 서버가 종료되었습니다.",
                LocalDateTime.now(KST).format(formatter)
            );
            
            // ⭐ 1. Admin DM 먼저 발송 (Discord Bot 종료 전)
            adminAlertNotificationService.sendAdminAlertSync(subject, message);
            log.info("서버 종료 알림 발송 완료 (Admin)");
            
            // ⭐ 2. Discord Webhook 동기 발송
            notificationService.sendSystemNotificationSync(message);
            log.info("서버 종료 알림 발송 완료 (Webhook)");
            
        } catch (Exception e) {
            log.warn("서버 종료 알림 발송 실패: {}", e.getMessage());
        }
        
        log.info("서버 종료 프로세스 완료");
    }
}