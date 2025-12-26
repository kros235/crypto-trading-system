package com.cryptotrading.config;

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
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");  // ⭐ 추가
    
    @PostConstruct
    public void onStartup() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                String message = String.format(
                    "✅ **서버 시작 완료**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🕐 시간: %s\n" +
                    "🖥️ 서비스: 코인 자동매매 시스템\n" +
                    "📊 상태: 정상 운영 중",
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
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
        log.info("서버 종료 감지 - 알림 발송 시작...");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread notificationThread = new Thread(() -> {
            try {
                String message = String.format(
                    "🛑 **서버 종료**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "🕐 시간: %s\n" +
                    "🖥️ 서비스: 코인 자동매매 시스템\n" +
                    "⚠️ 서버가 종료되었습니다.",
                    LocalDateTime.now(KST).format(formatter)  // ⭐ 수정
                );
                notificationService.sendSystemNotification(message);
                log.info("서버 종료 알림 발송 완료");
            } catch (Exception e) {
                log.warn("서버 종료 알림 발송 실패: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        notificationThread.start();
        
        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("서버 종료 알림 발송 타임아웃 (10초)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("서버 종료 알림 대기 중 인터럽트");
        }
        
        log.info("서버 종료 프로세스 완료");
    }
}