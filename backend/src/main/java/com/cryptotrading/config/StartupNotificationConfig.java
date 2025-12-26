package com.cryptotrading.config;

import com.cryptotrading.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StartupNotificationConfig {
    
    private final NotificationService notificationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @PostConstruct
    public void onStartup() {
        // 약간의 지연 후 알림 발송 (서비스 완전 초기화 대기)
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5초 대기
                String message = String.format(
                    "✅ **서버 시작 완료**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🕐 시간: %s\n" +
                    "🖥️ 서비스: 코인 자동매매 시스템\n" +
                    "📊 상태: 정상 운영 중",
                    LocalDateTime.now().format(formatter)
                );
                notificationService.sendSystemNotification(message);
                log.info("서버 시작 알림 발송 완료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("서버 시작 알림 발송 실패: {}", e.getMessage());
            }
        }).start();
    }
    
    @PreDestroy
    public void onShutdown() {
        try {
            String message = String.format(
                "🛑 **서버 종료**\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "🕐 시간: %s\n" +
                "🖥️ 서비스: 코인 자동매매 시스템\n" +
                "⚠️ 서버가 종료되었습니다.",
                LocalDateTime.now().format(formatter)
            );
            notificationService.sendSystemNotification(message);
            log.info("서버 종료 알림 발송 완료");
        } catch (Exception e) {
            log.warn("서버 종료 알림 발송 실패: {}", e.getMessage());
        }
    }
}