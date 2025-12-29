package com.cryptotrading.scheduler;

import com.cryptotrading.service.TradingBotService;
import com.cryptotrading.service.TradingBotService.BotExecutionResult;
import com.cryptotrading.service.DailyReportService;
import com.cryptotrading.service.NotificationService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.DiscordBotService;
import com.cryptotrading.service.NewsCollectorService;      
import com.cryptotrading.service.NewsAnalysisService;       
import com.cryptotrading.entity.User;
import com.cryptotrading.entity.TradingSetting;             
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.repository.TradingSettingRepository; 
import com.cryptotrading.repository.CoinNewsRepository;
import com.cryptotrading.dto.notification.DailyReportDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradingScheduler {

    private final TradingBotService tradingBotService;
    private final DailyReportService dailyReportService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final DiscordBotService discordBotService;

    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;
    private final CoinNewsRepository coinNewsRepository;
    private final TradingSettingRepository tradingSettingRepository;
    
    // 업비트 점검 시간 (매일 09:00 ~ 09:10 KST)
    private static final LocalTime MAINTENANCE_START = LocalTime.of(9, 0);
    private static final LocalTime MAINTENANCE_END = LocalTime.of(9, 10);

    /**
     * 5분마다 자동매매 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")  // 5분마다 (KST 기준)
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
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")  // 매일 04:00 KST
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
     @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")  // 매일 23:50 KST
     public void sendDailyReport() {
         log.info("========== 일일 리포트 발송 시작 ==========");
    
         try {
             List<User> users = userRepository.findAll();
        
             for (User user : users) {
                 try {
                     DailyReportDTO report = dailyReportService.generateDailyReport(user.getUserId());
                
                     // Discord 알림 (기존 로직)
                     notificationService.sendDailyReport(report);
                
                     // 이메일 알림 (신규 추가) ★★★ 추가 부분 ★★★
                     if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                         emailService.sendDailyReport(user.getEmail(), report);
                     }

	       // ★★★ 추가: Discord DM 발송 ★★★
                     if (user.getDiscordUserId() != null && !user.getDiscordUserId().isEmpty()) {
                         String profitSign = report.getTotalProfit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "";
                         discordBotService.sendDailyReportDM(
                             user.getDiscordUserId(),
                             report.getReportDate().toString(),
                             profitSign + String.format("%,.0f", report.getRealizedProfit()),
                             (report.getUnrealizedProfit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "") 
                                 + String.format("%,.0f", report.getUnrealizedProfit()),
                             profitSign + String.format("%,.0f", report.getTotalProfit()),
                             profitSign + report.getProfitRate().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                             report.getHoldingCount(),
                             String.format("%,.0f", report.getTotalHoldingValue())
                         );
                     }
                
                     log.info("사용자 {} 일일 리포트 발송 완료", user.getUserId());
                
                 } catch (Exception e) {
                     log.error("사용자 {} 일일 리포트 발송 실패: {}", user.getUserId(), e.getMessage());
                 }
             }
        
         } catch (Exception e) {
             log.error("일일 리포트 발송 중 오류: {}", e.getMessage());
         }
    
         log.info("========== 일일 리포트 발송 완료 ==========");
     }

    // ======================================================
    // AI 뉴스 분석 스케줄러
    // ======================================================

    /**
     * 매일 00:00 KST - AI 가중치 초기화
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetAiWeights() {
        log.info("====== AI 가중치 초기화 시작 (매일 00:00 KST) ======");
        try {
            newsAnalysisService.resetAllWeights();
            log.info("✅ AI 가중치 초기화 완료");
        } catch (Exception e) {
            log.error("❌ AI 가중치 초기화 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 3시간마다 - 뉴스 수집 및 AI 분석
     * 실행 시간: 00:00, 03:00, 06:00, 09:00, 12:00, 15:00, 18:00, 21:00 KST
     */
    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void collectAndAnalyzeNews() {
        log.info("====== 뉴스 수집 및 AI 분석 시작 (3시간마다) ======");
        
        try {
            // 1. 뉴스 수집
            log.info("📰 뉴스 수집 시작...");
            List<String> targetCoins = getActiveUserCoins();
            
            if (targetCoins.isEmpty()) {
                log.info("활성 사용자의 투자 코인이 없습니다. 스킵.");
                return;
            }
            
            // List를 받아서 size() 호출
            List<?> collected = newsCollectorService.collectAllNews(targetCoins);
            int collectedCount = collected.size();
            log.info("📰 뉴스 수집 완료: {}건", collectedCount);
            
            // 2. AI 분석 실행 (각 사용자별로)
            log.info("🤖 AI 뉴스 분석 시작...");
            analyzeNewsForAllUsers();
            
            log.info("====== 뉴스 수집 및 AI 분석 완료 ======");
            
        } catch (Exception e) {
            log.error("❌ 뉴스 수집/분석 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 04:00 KST - 오래된 뉴스 데이터 정리 (시스템 점검 시간)
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldNews() {
        log.info("====== 뉴스 데이터 정리 시작 (매일 04:00 KST) ======");
        try {
            int deleted = newsCollectorService.cleanupOldNews();
            log.info("✅ 오래된 뉴스 삭제 완료: {}건", deleted);
        } catch (Exception e) {
            log.error("❌ 뉴스 데이터 정리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 활성 사용자들의 투자 코인 목록 조회
     */
    private List<String> getActiveUserCoins() {
        List<TradingSetting> settings = tradingSettingRepository.findAll();
        return settings.stream()
                .filter(s -> s.getCoinSymbols() != null && !s.getCoinSymbols().isEmpty())
                .flatMap(s -> s.getCoinSymbols().stream())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 모든 활성 사용자에 대해 AI 분석 실행
     */
    private void analyzeNewsForAllUsers() {
        List<TradingSetting> settings = tradingSettingRepository.findAll();
        
        for (TradingSetting setting : settings) {
            if (setting.getUseAiAnalysis() == null || !setting.getUseAiAnalysis()) {
                continue; // AI 분석 비활성화된 사용자 스킵
            }
            
            String userId = setting.getUser().getUserId();

            for (String coin : setting.getCoinSymbols()) {
                try {
                    newsAnalysisService.analyzeNewsForCoin(userId, coin.trim());
                } catch (Exception e) {
                    log.error("사용자 {} 코인 {} 분석 실패: {}", userId, coin, e.getMessage());
                }
            }
        }
    }
}