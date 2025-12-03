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

import java.util.HashMap;
import java.util.Map;

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
}