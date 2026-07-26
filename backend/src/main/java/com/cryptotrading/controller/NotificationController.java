package com.cryptotrading.controller;

import com.cryptotrading.config.NotificationConfig;
import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.service.DailyReportService;
import com.cryptotrading.service.NotificationService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.exception.EntityNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.cryptotrading.service.DiscordBotService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final DailyReportService dailyReportService;
    private final NotificationConfig notificationConfig;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final DiscordBotService discordBotService;
    // ⭐⭐⭐ [Day 63 백로그 추가] 주식 전용 알림 테스트 엔드포인트용 의존성 ⭐⭐⭐
    private final com.cryptotrading.service.StockDailyReportService stockDailyReportService;
    // ⭐⭐⭐ [Day 63 개선] 종목명 표시용 (409820 → "TIGER 미국나스닥100레버리지(합성) (409820)") ⭐⭐⭐
    private final com.cryptotrading.repository.StockInfoRepository stockInfoRepository;
    /**
     * 알림 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("discordEnabled", notificationConfig.isEnabled());
        status.put("emailEnabled", false);
        status.put("telegramEnabled", false);
        
        String message = notificationConfig.isEnabled() 
                ? "Discord 알림이 활성화되어 있습니다."
                : "Discord 알림이 비활성화되어 있습니다.";
        status.put("message", message);
        
        return ResponseEntity.ok(status);
    }

    /**
     * 테스트 알림 발송
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(Authentication authentication) {
        String userId = authentication.getName();
        
        try {
            notificationService.notifyBotStatus(
                "🔔 **테스트 알림**\n\n이 메시지가 보이면 Discord 알림이 정상 작동 중입니다.\n\n👤 요청자: " + userId, 
                false
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "테스트 알림이 발송되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("테스트 알림 발송 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "알림 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 일일 리포트 미리보기
     */
    @GetMapping("/daily-report/preview")
    public ResponseEntity<DailyReportDTO> previewDailyReport(Authentication authentication) {
        String userId = authentication.getName();
        DailyReportDTO report = dailyReportService.generateDailyReport(userId);
        return ResponseEntity.ok(report);
    }

    /**
     * 일일 리포트 발송
     */
    @PostMapping("/daily-report/send")
    public ResponseEntity<Map<String, Object>> sendDailyReport(Authentication authentication) {
        String userId = authentication.getName();
        
        try {
            // ⭐⭐⭐ [추가] 비활성 사용자 차단 (방어적 코딩) ⭐⭐⭐
            // 사유: JWT 토큰 유효 기간 동안 사용자가 비활성화될 수 있으므로
            //       서비스 레이어 진입 전 명시적으로 차단
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "비활성화된 계정입니다. 관리자에게 문의하세요.");
                return ResponseEntity.status(403).body(response);
            }
            // ⭐⭐⭐ [추가 끝] ⭐⭐⭐
            
            DailyReportDTO report = dailyReportService.generateDailyReport(userId);
            notificationService.sendDailyReport(report);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "일일 리포트가 발송되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("일일 리포트 발송 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리포트 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 테스트 이메일 발송
     */
    @PostMapping("/email/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    
        boolean success = emailService.sendTestEmail(user.getEmail());
        response.put("success", success);
        response.put("message", success ? "테스트 이메일이 발송되었습니다." : "이메일 발송에 실패했습니다.");
        response.put("email", user.getEmail());
    
        return ResponseEntity.ok(response);
    }

    /**
     * 매수 체결 테스트 이메일 발송
     */
    @PostMapping("/email/test-buy")
    public ResponseEntity<Map<String, Object>> sendTestBuyEmail(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    
        Map<String, Object> response = new HashMap<>();
    
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    
        try {
            emailService.sendTradeNotification(
                user.getEmail(),
                "BUY",
                "KRW-BTC",
                new BigDecimal("0.001"),
                new BigDecimal("50000000"),
                new BigDecimal("50000"),
                "테스트: MA 이격 -6.5%, RSI 28.5"
            );
        
            response.put("success", true);
            response.put("message", "매수 체결 테스트 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이메일 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 매도 체결 테스트 이메일 발송
     */
    @PostMapping("/email/test-sell")
    public ResponseEntity<Map<String, Object>> sendTestSellEmail(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    
        Map<String, Object> response = new HashMap<>();
    
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            emailService.sendTradeNotification(
                user.getEmail(),
                "SELL",
                "KRW-BTC",
                new BigDecimal("0.001"),
                new BigDecimal("52000000"),
                new BigDecimal("52000"),
                "테스트: 목표 수익률 도달 (4.00%)"
            );
        
            response.put("success", true);
            response.put("message", "매도 체결 테스트 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이메일 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 일일 리포트 이메일 발송
     */
    @PostMapping("/email/daily-report")
    public ResponseEntity<Map<String, Object>> sendDailyReportEmail(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    
        Map<String, Object> response = new HashMap<>();
    
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    
        DailyReportDTO report = dailyReportService.generateDailyReport(userId);
        emailService.sendDailyReport(user.getEmail(), report);
        
        response.put("success", true);
        response.put("message", "일일 리포트 이메일이 발송되었습니다.");
        response.put("email", user.getEmail());
        
        return ResponseEntity.ok(response);
    }

    // Discord DM 테스트 API
    /**
     * Discord DM 테스트 발송
     */
    @PostMapping("/discord/test-dm")
    public ResponseEntity<Map<String, Object>> sendTestDiscordDM(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        // Discord User ID 확인
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다. 프로필에서 먼저 설정해주세요.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Bot 활성화 확인
        if (!discordBotService.isEnabled()) {
            response.put("success", false);
            response.put("message", "Discord Bot이 비활성화 상태입니다. 관리자에게 문의하세요.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // DM 발송 시도
        boolean success = discordBotService.sendTestDM(user.getDiscordUserId());
        
        response.put("success", success);
        response.put("message", success 
                ? "테스트 DM이 발송되었습니다. Discord를 확인해주세요." 
                : "DM 발송에 실패했습니다. Discord User ID를 확인해주세요.");
        
        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Discord Bot 상태 조회
     */
    @GetMapping("/discord/bot-status")
    public ResponseEntity<Map<String, Object>> getDiscordBotStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("botEnabled", discordBotService.isEnabled());
        status.put("webhookEnabled", notificationConfig.isEnabled());
        return ResponseEntity.ok(status);
    }

    // 일일 리포트 DM 테스트 API
    /**
     * 일일 리포트 DM 테스트 발송
     */
    @PostMapping("/discord/test-daily-report")
    public ResponseEntity<Map<String, Object>> testDailyReportDM(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            // 일일 리포트 생성
            DailyReportDTO report = dailyReportService.generateDailyReport(userId);
            
            // DM 발송
            String profitSign = report.getTotalProfit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "";
            // ⭐⭐⭐ [Day 63 개선] 종목별(코인별) 평가액 내역 추가 ⭐⭐⭐
            var holdingsBreakdown = discordBotService.buildHoldingsBreakdown(report.getCoinSummaries(), null);
            discordBotService.sendDailyReportDM(
                user.getDiscordUserId(),
                "코인 ",
                report.getReportDate().toString(),
                profitSign + String.format("%,.0f", report.getRealizedProfit()),
                (report.getUnrealizedProfit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "") 
                    + String.format("%,.0f", report.getUnrealizedProfit()),
                profitSign + String.format("%,.0f", report.getTotalProfit()),
                profitSign + report.getProfitRate().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                report.getHoldingCount(),
                String.format("%,.0f", report.getTotalHoldingValue()),
                holdingsBreakdown
            );
            
            response.put("success", true);
            response.put("message", "일일 리포트 DM이 발송되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "발송 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 매수 알림 DM 테스트 API
    /**
     * 매수 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-buy")
    public ResponseEntity<Map<String, Object>> testBuyNotificationDM(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 테스트용 매수 알림 발송
        discordBotService.sendBuyNotification(
            user.getDiscordUserId(),
            "KRW-BTC",
            "0.00012345",
            "150,000,000",
            "50,000",
            "테스트: MA 이격 -6.5%, RSI 28.5"  // ⭐⭐⭐ [추가] 테스트용 매수 사유 ⭐⭐⭐
        );
        
        response.put("success", true);
        response.put("message", "매수 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    // 매도 알림 DM 테스트 API
    /**
     * 매도 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-sell")
    public ResponseEntity<Map<String, Object>> testSellNotificationDM(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 테스트용 매도 알림 발송 (수익)
        discordBotService.sendSellNotification(
            user.getDiscordUserId(),
            "KRW-BTC",
            "0.00012345",
            "155,000,000",
            "+2,500",
            "+5.00",
            "테스트: 목표 수익률 도달 (5.00%)"  // ⭐⭐⭐ [추가] 테스트용 매도 사유 ⭐⭐⭐
        );
        
        response.put("success", true);
        response.put("message", "매도 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    // 손절매 알림 DM 테스트 AP
    /**
     * 손절매 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-stoploss")
    public ResponseEntity<Map<String, Object>> testStopLossNotificationDM(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 테스트용 손절매 알림 발송
        discordBotService.sendStopLossNotification(
            user.getDiscordUserId(),
            "KRW-ETH",
            "0.05",
            "4,800,000",
            "-25,000",
            "-10.00"
        );
        
        response.put("success", true);
        response.put("message", "손절매 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

     // 가중치 변경 테스트 이메일 발송
    @PostMapping("/email/test-weight-change")
    @Operation(summary = "가중치 변경 테스트 이메일", description = "AI 뉴스 분석 가중치 변경 테스트 이메일을 발송합니다.")
    public ResponseEntity<Map<String, Object>> sendWeightChangeTestEmail(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 등록되어 있지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        String subject = "📰 AI 뉴스 분석 결과 - KRW-BTC (테스트)";
        String htmlContent = String.format("""
            <h3>🔹 분석 시간: %s KST</h3>
            <p>🔹 분석 뉴스: 5건</p>
            <p>🔹 평균 점수: +0.65 (POSITIVE)</p>
            <p>🔹 가중치 변경: 0.00%% → +0.33%%</p>
            <br>
            <h4>📊 지표 변경</h4>
            <p>- buyThresholdPct 조정: +0.33%%</p>
            <br>
            <h4>📰 주요 뉴스</h4>
            <p>1. [CoinTelegraph] Bitcoin ETF sees record inflows</p>
            <p>2. [Bitcoin_Magazine] MicroStrategy buys more BTC</p>
            <p>3. [Decrypt] Institutional investors increase crypto holdings</p>
            <br>
            <p style="color: #888;">※ 이 메일은 테스트 목적으로 발송되었습니다.</p>
            """, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        
        boolean result = emailService.sendSystemAlert(user.getEmail(), subject, htmlContent);
        
        response.put("success", result);
        response.put("message", result ? "가중치 변경 테스트 이메일이 발송되었습니다." : "이메일 발송에 실패했습니다.");
        return ResponseEntity.ok(response);
    }

    // 가중치 변경 테스트 Discord DM 발송
    @PostMapping("/discord/test-weight-change")
    @Operation(summary = "가중치 변경 테스트 Discord DM", description = "AI 뉴스 분석 가중치 변경 테스트 DM을 발송합니다.")
    public ResponseEntity<Map<String, Object>> sendWeightChangeTestDiscord(
            @AuthenticationPrincipal String userId) {
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> response = new HashMap<>();
        
        if (user.getDiscordUserId() == null || user.getDiscordUserId().isEmpty()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 등록되어 있지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!discordBotService.isEnabled()) {
            response.put("success", false);
            response.put("message", "Discord Bot이 비활성화 상태입니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        String subject = "📰 AI 뉴스 분석 결과 - KRW-BTC (테스트)";
        StringBuilder message = new StringBuilder();
        message.append(String.format("🔹 분석 시간: %s KST\n", 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        message.append("🔹 분석 뉴스: 5건\n");
        message.append("🔹 평균 점수: +0.65 (POSITIVE)\n");
        message.append("🔹 가중치 변경: 0.00% → +0.33%\n");
        message.append("\n📊 지표 변경\n");
        message.append("- buyThresholdPct 조정: +0.33%\n");
        message.append("\n📰 주요 뉴스\n");
        message.append("1. [CoinTelegraph] Bitcoin ETF sees record inflows\n");
        message.append("2. [Bitcoin_Magazine] MicroStrategy buys more BTC\n");
        message.append("3. [Decrypt] Institutional investors increase crypto holdings\n");
        message.append("\n※ 이 메시지는 테스트 목적으로 발송되었습니다.");
        
        boolean result = discordBotService.sendSystemAlertDM(user.getDiscordUserId(), subject, message.toString());
        
        response.put("success", result);
        response.put("message", result ? "가중치 변경 테스트 DM이 발송되었습니다." : "Discord DM 발송에 실패했습니다.");
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // ⭐⭐⭐ [Day 63 백로그] 주식 전용 알림 테스트 엔드포인트 8개 ⭐⭐⭐
    // 왜: StockBotMonitorView.vue가 지금까지 코인용 엔드포인트(test-buy 등)를 임시로 호출하고 있었음.
    //     대부분의 발송 로직(discordBotService.send*Notification, emailService.sendTradeNotification)은
    //     이미 종목명을 파라미터로 받는 범용 구조라 별도 신규 메서드 없이 그대로 재사용 가능함.
    //     보유기간 경고만 코인에 없는 개념이라 NotificationService.buildStockHoldingWarningMessage()를 새로 추가함.
    // =====================================================================

    /**
     * 주식 매수 체결 테스트 이메일 발송
     */
    @PostMapping("/email/test-stock-buy")
    public ResponseEntity<Map<String, Object>> sendTestStockBuyEmail(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            emailService.sendTradeNotification(
                user.getEmail(),
                "BUY",
                resolveStockDisplayName("409820"),
                new BigDecimal("10"),
                new BigDecimal("18500"),
                new BigDecimal("185000"),
                "테스트: MA 이격 -3.2%, RSI 33.5"
            );

            response.put("success", true);
            response.put("message", "주식 매수 체결 테스트 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이메일 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 주식 매도 체결 테스트 이메일 발송
     */
    @PostMapping("/email/test-stock-sell")
    public ResponseEntity<Map<String, Object>> sendTestStockSellEmail(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            emailService.sendTradeNotification(
                user.getEmail(),
                "SELL",
                resolveStockDisplayName("409820"),
                new BigDecimal("10"),
                new BigDecimal("19200"),
                new BigDecimal("192000"),
                "테스트: 목표 수익률 도달 (2.50%)"
            );

            response.put("success", true);
            response.put("message", "주식 매도 체결 테스트 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이메일 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 주식 일일 리포트 테스트 이메일 발송
     * ⭐⭐⭐ [Day 63 개선] 이전엔 stock* 값을 core 필드(coinSummaries 포함)에 복사해 넣어서
     * "보유 현황" 표가 "코인" 라벨로 나오는 문제가 있었음. 이제 core 필드는 상단 요약 카드용으로만 채우고,
     * coinSummaries는 비워서 "코인" 표는 렌더링되지 않게 하며, hasStockData=true + stockSummaries로
     * 전용 "[주식]" 섹션(종목 라벨, 실제 종목명, ETF 구분 포함)만 표시되게 한다.
     */
    @PostMapping("/email/test-stock-daily-report")
    public ResponseEntity<Map<String, Object>> sendTestStockDailyReportEmail(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        DailyReportDTO stockReport = toEmailShapedStockReport(
                stockDailyReportService.generateStockDailyReport(userId));

        emailService.sendDailyReport(user.getEmail(), stockReport);

        response.put("success", true);
        response.put("message", "주식 일일 리포트 테스트 이메일이 발송되었습니다.");
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 레버리지 ETF 보유기간 경고 테스트 이메일 발송
     * ⭐⭐⭐ [Day 63 개선] sendSystemAlert(줄글) → sendHoldingWarningEmail(카드+표 포맷)로 교체 ⭐⭐⭐
     */
    @PostMapping("/email/test-stock-holding-warning")
    public ResponseEntity<Map<String, Object>> sendTestStockHoldingWarningEmail(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("success", false);
            response.put("message", "이메일이 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        emailService.sendHoldingWarningEmail(
                user.getEmail(),
                resolveStockDisplayName("409820"),
                21,
                com.cryptotrading.service.StockRiskManagementService.LEVERAGE_ETF_MAX_DAYS,
                java.time.LocalDate.now().minusDays(21).toString(),
                true
        );

        response.put("success", true);
        response.put("message", "보유기간 경고 테스트 이메일이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 종목코드 → "종목명 (코드)" 표시용 문자열 변환
     * 왜: 매수/매도/손절/보유경고 테스트 알림에서 종목코드만 나오면 무엇인지 바로 알기 어렵다는 피드백 반영.
     *     StockInfo에 등록되지 않은 코드는 조회 실패해도 예외 없이 코드만 반환 (graceful fallback).
     */
    private String resolveStockDisplayName(String stockCode) {
        return stockInfoRepository.findById(stockCode)
                .map(info -> info.getStockName() + " (" + stockCode + ")")
                .orElse(stockCode);
    }

    /**
     * 주식 매수 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-stock-buy")
    public ResponseEntity<Map<String, Object>> testStockBuyNotificationDM(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        discordBotService.sendBuyNotification(
            user.getDiscordUserId(),
            resolveStockDisplayName("409820"),
            "10주",
            "18,500",
            "185,000",
            "테스트: MA 이격 -3.2%, RSI 33.5"
        );

        response.put("success", true);
        response.put("message", "주식 매수 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 매도 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-stock-sell")
    public ResponseEntity<Map<String, Object>> testStockSellNotificationDM(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        discordBotService.sendSellNotification(
            user.getDiscordUserId(),
            resolveStockDisplayName("409820"),
            "10주",
            "19,200",
            "+7,000",
            "+3.78",
            "테스트: 목표 수익률 도달 (2.50%)"
        );

        response.put("success", true);
        response.put("message", "주식 매도 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 손절매 알림 DM 테스트 발송
     */
    @PostMapping("/discord/test-stock-stoploss")
    public ResponseEntity<Map<String, Object>> testStockStopLossNotificationDM(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        discordBotService.sendStopLossNotification(
            user.getDiscordUserId(),
            resolveStockDisplayName("409820"),
            "10주",
            "17,500",
            "-10,000",
            "-5.41"
        );

        response.put("success", true);
        response.put("message", "주식 손절매 알림 테스트 DM이 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 일일 리포트 DM 테스트 발송 (email/test-stock-daily-report와 동일한 재매핑 방식)
     */
    @PostMapping("/discord/test-stock-daily-report")
    public ResponseEntity<Map<String, Object>> testStockDailyReportDM(
            @AuthenticationPrincipal String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (user.getDiscordUserId() == null || user.getDiscordUserId().isBlank()) {
            response.put("success", false);
            response.put("message", "Discord User ID가 설정되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // ⭐⭐⭐ [Day 63 개선] coreShape 매핑 없이 stock* 필드를 그대로 사용 (Discord는 core 필드에 의존하지 않음) ⭐⭐⭐
            DailyReportDTO report = stockDailyReportService.generateStockDailyReport(userId);

            String profitSign = report.getStockTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            var holdingsBreakdown = discordBotService.buildHoldingsBreakdown(null, report.getStockSummaries());
            discordBotService.sendDailyReportDM(
                user.getDiscordUserId(),
                "주식 ",
                report.getReportDate().toString(),
                profitSign + String.format("%,.0f", report.getStockRealizedProfit()),
                (report.getStockUnrealizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    + String.format("%,.0f", report.getStockUnrealizedProfit()),
                profitSign + String.format("%,.0f", report.getStockTotalProfit()),
                profitSign + report.getStockProfitRate().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                report.getStockHoldingCount(),
                String.format("%,.0f", report.getStockTotalHoldingValue()),
                holdingsBreakdown
            );

            response.put("success", true);
            response.put("message", "주식 일일 리포트 DM이 발송되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "발송 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * StockDailyReportService가 반환하는 stock* 전용 필드 DailyReportDTO를 이메일 발송용으로 가공한다.
     *
     * ⭐⭐⭐ [버그 수정] 이전엔 core 필드(realizedProfit 등)에도 주식 데이터를 그대로 복사해 넣어서,
     * 상단 "총 손익/손익 상세/거래 현황" 카드와 하단 "[주식] 손익 상세" 섹션에 같은 숫자가 두 번 표시되는
     * 중복 버그가 있었음. core 필드는 실제 코인 활동이 없는 "단독 주식 테스트 메일"이라는 사실 그대로
     * 0/빈 값으로 채우고(= "[코인]" 섹션은 활동 없음으로 정상 표시), 실제 데이터는 stock* 필드에만 담아
     * "[주식]" 섹션에서만 보이게 한다. (production 23:50 통합 리포트는 이 메서드를 거치지 않고
     * TradingScheduler에서 실제 코인 리포트에 stock* 필드만 병합하므로 이 버그의 영향이 없었음)
     */
    private DailyReportDTO toEmailShapedStockReport(DailyReportDTO stockReport) {
        return DailyReportDTO.builder()
                .userId(stockReport.getUserId())
                .reportDate(stockReport.getReportDate())
                // ⭐⭐⭐ [버그 수정] hasCoinActivity를 명시하지 않으면 EmailService에서 null→true로 기본 처리되어
                // "[코인]" 섹션이 계속 보이는 문제가 있었음. 코인 활동이 없는 단독 주식 테스트이므로 false로 명시.
                .hasCoinActivity(false)
                // ⭐ 코인 활동 없음 (단독 주식 테스트 메일이므로) - "[코인]" 섹션은 0/빈 값으로 정직하게 표시
                .buyCount(0)
                .sellCount(0)
                .totalBuyAmount(BigDecimal.ZERO)
                .totalSellAmount(BigDecimal.ZERO)
                .realizedProfit(BigDecimal.ZERO)
                .unrealizedProfit(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .holdingCount(0)
                .totalHoldingValue(BigDecimal.ZERO)
                .totalInvestment(BigDecimal.ZERO)
                .coinSummaries(List.of())
                // ⭐ 실제 주식 데이터는 여기(stock*)에만 담아 "[주식]" 섹션에서만 표시되게 한다
                .stockBuyCount(stockReport.getStockBuyCount())
                .stockSellCount(stockReport.getStockSellCount())
                .stockTotalBuyAmount(stockReport.getStockTotalBuyAmount())
                .stockTotalSellAmount(stockReport.getStockTotalSellAmount())
                .stockRealizedProfit(stockReport.getStockRealizedProfit())
                .stockUnrealizedProfit(stockReport.getStockUnrealizedProfit())
                .stockTotalProfit(stockReport.getStockTotalProfit())
                .stockProfitRate(stockReport.getStockProfitRate())
                .stockHoldingCount(stockReport.getStockHoldingCount())
                .stockTotalHoldingValue(stockReport.getStockTotalHoldingValue())
                .stockTotalInvestment(stockReport.getStockTotalInvestment())
                .stockSummaries(stockReport.getStockSummaries())
                .build();
    }
}