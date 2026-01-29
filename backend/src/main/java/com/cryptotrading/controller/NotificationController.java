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
            discordBotService.sendDailyReportDM(
                user.getDiscordUserId(),
                report.getReportDate().toString(),
                profitSign + String.format("%,.0f", report.getRealizedProfit()),
                (report.getUnrealizedProfit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "") 
                    + String.format("%,.0f", report.getUnrealizedProfit()),
                profitSign + String.format("%,.0f", report.getTotalProfit()),
                profitSign + report.getProfitRate().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                report.getHoldingCount(),
                String.format("%,.0f", report.getTotalHoldingValue())
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
}