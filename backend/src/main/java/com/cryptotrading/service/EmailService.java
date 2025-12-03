package com.cryptotrading.service;

import com.cryptotrading.config.EmailConfig;
import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.dto.notification.EmailNotificationDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;
    
    private static final NumberFormat KRW_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 일반 이메일 발송
     */
    @Async
    public void sendEmail(EmailNotificationDTO dto) {
        if (!emailConfig.isEnabled()) {
            log.info("이메일 알림이 비활성화되어 있습니다.");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(dto.getTo());
            helper.setSubject(dto.getSubject());
            
            if (dto.getTemplateName() != null && dto.getVariables() != null) {
                // Thymeleaf 템플릿 사용
                Context context = new Context();
                context.setVariables(dto.getVariables());
                String htmlContent = templateEngine.process(dto.getTemplateName(), context);
                helper.setText(htmlContent, true);
            } else if (dto.getHtmlContent() != null) {
                // HTML 직접 지정
                helper.setText(dto.getHtmlContent(), true);
            } else {
                // 텍스트만
                helper.setText(dto.getTextContent() != null ? dto.getTextContent() : "", false);
            }
            
            mailSender.send(message);
            log.info("이메일 발송 성공: to={}, subject={}", dto.getTo(), dto.getSubject());
            
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, error={}", dto.getTo(), e.getMessage());
        }
    }
    
    /**
     * 거래 체결 알림 이메일
     */
    @Async
    public void sendTradeNotification(String email, String type, String coinSymbol, 
                                       BigDecimal quantity, BigDecimal price, BigDecimal totalAmount) {
        if (!emailConfig.isEnabled() || email == null || email.isEmpty()) {
            return;
        }
        
        String typeKr = "BUY".equals(type) ? "매수" : "매도";
        String subject = String.format("[코인봇] %s %s 체결 알림", coinSymbol, typeKr);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", typeKr);
        variables.put("typeClass", "BUY".equals(type) ? "buy" : "sell");
        variables.put("coinSymbol", coinSymbol);
        variables.put("quantity", quantity.toPlainString());
        variables.put("price", KRW_FORMAT.format(price));
        variables.put("totalAmount", KRW_FORMAT.format(totalAmount));
        variables.put("timestamp", LocalDateTime.now().format(DATETIME_FORMAT));
        
        EmailNotificationDTO dto = EmailNotificationDTO.builder()
                .to(email)
                .subject(subject)
                .templateName("email/trade-notification")
                .variables(variables)
                .build();
        
        sendEmail(dto);
    }
    
    /**
     * 일일 리포트 이메일
     */
    @Async
    public void sendDailyReport(String email, DailyReportDTO report) {
        if (!emailConfig.isEnabled() || email == null || email.isEmpty()) {
            return;
        }
    
        // ★★★ 수정: getDate() → getReportDate() ★★★
        String subject = String.format("[코인봇] %s 일일 리포트", report.getReportDate());
    
        Map<String, Object> variables = new HashMap<>();
        // ★★★ 수정: 필드명 변경 ★★★
        variables.put("date", report.getReportDate());
        variables.put("realizedProfit", KRW_FORMAT.format(report.getRealizedProfit()));
        variables.put("unrealizedProfit", KRW_FORMAT.format(report.getUnrealizedProfit()));
        variables.put("totalProfit", KRW_FORMAT.format(report.getTotalProfit()));
        // ★★★ 수정: getTotalProfitRate() → getProfitRate() ★★★
        variables.put("totalProfitRate", report.getProfitRate());
        variables.put("profitClass", report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "profit" : "loss");
        variables.put("buyCount", report.getBuyCount());
        variables.put("sellCount", report.getSellCount());
        variables.put("totalInvestment", KRW_FORMAT.format(report.getTotalInvestment()));
        // ★★★ 수정: getTotalEvaluation() → getTotalHoldingValue() ★★★
        variables.put("totalEvaluation", KRW_FORMAT.format(report.getTotalHoldingValue()));
        // ★★★ 수정: getHoldings() → getCoinSummaries() ★★★
        variables.put("holdings", report.getCoinSummaries());
        variables.put("timestamp", LocalDateTime.now().format(DATETIME_FORMAT));
    
        EmailNotificationDTO dto = EmailNotificationDTO.builder()
                .to(email)
                .subject(subject)
                .templateName("email/daily-report")
                .variables(variables)
                .build();
    
        sendEmail(dto);
    }
    
    /**
     * 손절매 경고 이메일
     */
    @Async
    public void sendStopLossAlert(String email, String coinSymbol, BigDecimal lossRate, BigDecimal lossAmount) {
        if (!emailConfig.isEnabled() || email == null || email.isEmpty()) {
            return;
        }
        
        String subject = String.format("[코인봇] ⚠️ %s 손절매 발생!", coinSymbol);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("coinSymbol", coinSymbol);
        variables.put("lossRate", lossRate);
        variables.put("lossAmount", KRW_FORMAT.format(lossAmount));
        variables.put("timestamp", LocalDateTime.now().format(DATETIME_FORMAT));
        
        EmailNotificationDTO dto = EmailNotificationDTO.builder()
                .to(email)
                .subject(subject)
                .templateName("email/stop-loss-alert")
                .variables(variables)
                .build();
        
        sendEmail(dto);
    }
    
    /**
     * 테스트 이메일 발송
     */
    public boolean sendTestEmail(String email) {
        if (!emailConfig.isEnabled()) {
            log.warn("이메일 알림이 비활성화되어 있습니다.");
            return false;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(email);
            helper.setSubject("[코인봇] 이메일 테스트");
            
            String htmlContent = """
                <div style="font-family: 'Malgun Gothic', sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #1976d2;">🤖 코인 자동매매 시스템</h2>
                    <p>이메일 알림이 정상적으로 설정되었습니다.</p>
                    <p style="color: #666; font-size: 12px;">발송 시간: %s</p>
                </div>
                """.formatted(LocalDateTime.now().format(DATETIME_FORMAT));
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            log.info("테스트 이메일 발송 성공: {}", email);
            return true;
            
        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패: {}", e.getMessage());
            return false;
        }
    }
}