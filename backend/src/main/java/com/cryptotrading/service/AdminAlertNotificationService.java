package com.cryptotrading.service;

import com.cryptotrading.entity.User;
import com.cryptotrading.entity.UserRole;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAlertNotificationService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DiscordBotService discordBotService;
    
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Admin 계정들에게 시스템 알림 발송 (비동기)
     */
    @Async
    public void sendAdminAlert(String subject, String message) {
        try {
            List<User> admins = userRepository.findByRoleAndIsActive(UserRole.ADMIN, true);
            
            if (admins.isEmpty()) {
                log.debug("활성화된 관리자 계정이 없습니다.");
                return;
            }
            
            String currentTime = LocalDateTime.now(KST).format(formatter);
            log.info("[{}] Admin 알림 발송 시작 - 대상: {}명", currentTime, admins.size());
            
            for (User admin : admins) {
                sendToAdmin(admin, subject, message);
            }
            
        } catch (Exception e) {
            log.error("Admin 알림 발송 중 오류: {}", e.getMessage());
        }
    }
    
    /**
     * ⭐ 추가: Admin 계정들에게 시스템 알림 발송 (동기 - 서버 종료 시 사용)
     */
    public void sendAdminAlertSync(String subject, String message) {
        try {
            List<User> admins = userRepository.findByRoleAndIsActive(UserRole.ADMIN, true);
            
            if (admins.isEmpty()) {
                log.debug("활성화된 관리자 계정이 없습니다.");
                return;
            }
            
            String currentTime = LocalDateTime.now(KST).format(formatter);
            log.info("[{}] Admin 알림 동기 발송 시작 - 대상: {}명", currentTime, admins.size());
            
            for (User admin : admins) {
                sendToAdmin(admin, subject, message);
            }
            
            log.info("Admin 알림 동기 발송 완료");
            
        } catch (Exception e) {
            log.error("Admin 알림 동기 발송 중 오류: {}", e.getMessage());
        }
    }
    
    private void sendToAdmin(User admin, String subject, String message) {
        sendDiscordDM(admin, subject, message);
        sendEmail(admin, subject, message);
    }
    
    private void sendDiscordDM(User admin, String subject, String message) {
        String discordUserId = admin.getDiscordUserId();
        
        if (discordUserId == null || discordUserId.isBlank()) {
            log.debug("Admin [{}]: Discord User ID 미등록 - DM 발송 스킵", admin.getUserId());
            return;
        }
        
        if (!discordBotService.isEnabled()) {
            log.debug("Discord Bot 비활성화 상태 - DM 발송 스킵");
            return;
        }
        
        try {
            boolean success = discordBotService.sendSystemAlertDM(discordUserId, subject, message);
            
            if (success) {
                log.info("Admin [{}]: Discord DM 발송 성공", admin.getUserId());
            } else {
                log.warn("Admin [{}]: Discord DM 발송 실패", admin.getUserId());
            }
        } catch (Exception e) {
            log.error("Admin [{}]: Discord DM 발송 오류 - {}", admin.getUserId(), e.getMessage());
        }
    }
    
    private void sendEmail(User admin, String subject, String message) {
        String email = admin.getEmail();
        
        if (email == null || email.isBlank()) {
            log.debug("Admin [{}]: Email 미등록 - 이메일 발송 스킵", admin.getUserId());
            return;
        }
        
        try {
            String htmlMessage = convertMarkdownToHtml(message);
            boolean success = emailService.sendSystemAlert(email, subject, htmlMessage);
            
            if (success) {
                log.info("Admin [{}]: Email 발송 성공 - {}", admin.getUserId(), email);
            } else {
                log.warn("Admin [{}]: Email 발송 실패 - {}", admin.getUserId(), email);
            }
        } catch (Exception e) {
            log.error("Admin [{}]: Email 발송 오류 - {}", admin.getUserId(), e.getMessage());
        }
    }
    
    private String convertMarkdownToPlainText(String markdown) {
        if (markdown == null) return "";
        
        return markdown
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("━+", "─────────────────────")
                .trim();
    }
    
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null) return "";
        
        String html = markdown
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("`(.+?)`", "<code>$1</code>")
                .replaceAll("━+", "<hr style='border: 1px solid #ddd;'>")
                .replaceAll("\n", "<br>");
        
        return String.format("""
            <div style="font-family: 'Segoe UI', Arial, sans-serif; padding: 15px; background-color: #f5f5f5; border-radius: 8px;">
                <div style="background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid #ff6b6b;">
                    %s
                </div>
                <div style="margin-top: 10px; font-size: 12px; color: #888;">
                    코인 & 주식 자동매매 시스템 - 관리자 알림
                </div>
            </div>
            """, html);
    }
}