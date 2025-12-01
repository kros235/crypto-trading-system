package com.cryptotrading.scheduler;

import com.cryptotrading.service.TradingBotService;
import com.cryptotrading.service.TradingBotService.BotExecutionResult;
import com.cryptotrading.service.DailyReportService;
import com.cryptotrading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradingScheduler {

    private final TradingBotService tradingBotService;
    private final DailyReportService dailyReportService;
    private final NotificationService notificationService;
    
    // 업비트 점검 시간 (매일 09:00 ~ 09:10 KST)
    private static final LocalTime MAINTENANCE_START = LocalTime.of(9, 0);
    private static final LocalTime MAINTENANCE_END = LocalTime.of(9, 10);

    /**
     * 5분마다 자동매매 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void executeAutoTrading() {
        LocalTime now = LocalTime.now();
        
        // 업비트 점검 시간 체크
        if (isMaintenanceTime(now)) {
            log.info("업비트 점검 시간 - 자동매매 스킵 ({})", now);
            return;
        }
        
        log.info("========================================");
        log.info("스케줄러 자동매매 시작: {}", LocalDateTime.now());
        log.info("========================================");
        
        try {
            List<BotExecutionResult> results = tradingBotService.executeForAllUsers();
            
            // 실행 결과 요약
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            int totalBuys = 0;
            int totalSells = 0;
            
            for (BotExecutionResult result : results) {
                switch (result.getStatus()) {
                    case "SUCCESS" -> {
                        successCount++;
                        totalBuys += result.getBuyCount();
                        totalSells += result.getSellCount();
                    }
                    case "SKIP" -> skipCount++;
                    case "ERROR" -> errorCount++;
                }
            }
            
            log.info("========================================");
            log.info("스케줄러 자동매매 완료");
            log.info("처리: {}명 (성공: {}, 스킵: {}, 오류: {})", 
                    results.size(), successCount, skipCount, errorCount);
            log.info("거래: 매수 {}건, 매도 {}건", totalBuys, totalSells);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("스케줄러 자동매매 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 새벽 4시에 시스템 점검
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void dailySystemCheck() {
        log.info("========================================");
        log.info("일일 시스템 점검 시작: {}", LocalDateTime.now());
        log.info("========================================");
        
        // 추후 구현: 일일 통계 생성, 알림 발송 등
        
        log.info("일일 시스템 점검 완료");
    }

    /**
     * 업비트 점검 시간 여부 확인
     */
    private boolean isMaintenanceTime(LocalTime time) {
        return !time.isBefore(MAINTENANCE_START) && time.isBefore(MAINTENANCE_END);
    }

     /**
     * 매일 23:50에 일일 리포트 발송
     */
    @Scheduled(cron = "0 50 23 * * *")
    public void sendDailyReports() {
        log.info("========================================");
        log.info("일일 리포트 발송 시작: {}", LocalDateTime.now());
        log.info("========================================");
        
        try {
            dailyReportService.sendDailyReportsForAllUsers();
            log.info("일일 리포트 발송 완료");
        } catch (Exception e) {
            log.error("일일 리포트 발송 오류: {}", e.getMessage(), e);
            notificationService.notifySystemError("일일 리포트 발송 실패: " + e.getMessage());
        }
    }
}