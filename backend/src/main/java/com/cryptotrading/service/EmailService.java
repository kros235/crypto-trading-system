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
import java.util.ArrayList; 
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.ZoneId;

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
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    
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

    // ⭐⭐⭐ 추가: 시스템 알림 이메일 발송 (관리자용) ⭐⭐⭐
    /**
     * 시스템 알림 이메일 발송 (관리자용)
     */
    public boolean sendSystemAlert(String email, String subject, String htmlContent) {
        if (!emailConfig.isEnabled()) {
            log.debug("이메일 알림이 비활성화되어 있습니다.");
            return false;
        }
        
        if (email == null || email.isBlank()) {
            log.debug("이메일 주소가 없습니다.");
            return false;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(email);
            helper.setSubject("[모니터링] " + subject);
            
            String wrappedHtml = wrapSystemAlertHtml(subject, htmlContent);
            helper.setText(wrappedHtml, true);
            
            mailSender.send(message);
            log.info("시스템 알림 이메일 발송 성공: to={}, subject={}", email, subject);
            return true;
            
        } catch (Exception e) {
            log.error("시스템 알림 이메일 발송 실패: to={}, error={}", email, e.getMessage());
            return false;
        }
    }
    
    private String wrapSystemAlertHtml(String subject, String content) {
        String timestamp = LocalDateTime.now(KST).format(DATETIME_FORMAT);
        
        String borderColor = "#1976d2";
        if (subject.contains("긴급") || subject.contains("🚨") || subject.contains("위험")) {
            borderColor = "#f44336";
        } else if (subject.contains("경고") || subject.contains("⚠️")) {
            borderColor = "#ff9800";
        } else if (subject.contains("시작") || subject.contains("✅")) {
            borderColor = "#4caf50";
        } else if (subject.contains("종료") || subject.contains("🛑")) {
            borderColor = "#757575";
        }
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Malgun Gothic', sans-serif; margin: 0; padding: 20px; background: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="background: %s; color: white; padding: 20px;">
                        <h1 style="margin: 0; font-size: 18px;">🤖 코인 & 주식 자동매매 시스템 - 관리자 알림</h1>
                    </div>
                    <div style="padding: 20px;">%s</div>
                    <div style="padding: 15px 20px; background: #fafafa; border-top: 1px solid #eee; font-size: 12px; color: #888;">
                        <p>발송 시간: %s (KST)</p>
                    </div>
                </div>
            </body>
            </html>
            """, borderColor, content, timestamp);
    }
    
    /**
     * 거래 체결 알림 이메일
     */
    @Async
    public void sendTradeNotification(String email, String type, String coinSymbol, 
                                       BigDecimal quantity, BigDecimal price, BigDecimal totalAmount,
                                       String reason) {
        if (!emailConfig.isEnabled() || email == null || email.isEmpty()) {
            return;
        }
        
        String typeKr = "BUY".equals(type) ? "매수" : "매도";
        // ⭐⭐⭐ [개선] 제목을 "[거래]"로 뭉뚱그리지 않고 "[매수]"/"[매도]"로 구분 표시 ⭐⭐⭐
        String subject = String.format("[%s] %s 체결 알림", typeKr, coinSymbol);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", typeKr);
        variables.put("typeClass", "BUY".equals(type) ? "buy" : "sell");
        variables.put("coinSymbol", coinSymbol);
        variables.put("quantity", quantity.toPlainString());
        variables.put("price", formatPriceForEmail(price));
        variables.put("totalAmount", KRW_FORMAT.format(totalAmount));
        variables.put("timestamp", LocalDateTime.now().format(DATETIME_FORMAT));
        // ⭐⭐⭐ [추가] reason 변수 추가 ⭐⭐⭐
        variables.put("reason", reason != null && !reason.isEmpty() ? reason : "조건 충족");
        
        EmailNotificationDTO dto = EmailNotificationDTO.builder()
                .to(email)
                .subject(subject)
                .templateName("email/trade-notification")
                .variables(variables)
                .build();
        
        sendEmail(dto);
    }

    /**
     * ⭐⭐⭐ [Day 63 개선] 레버리지 ETF 보유기간 경고 이메일 ⭐⭐⭐
     * 왜: sendSystemAlert()로 보내던 기존 방식은 줄글(<br> 텍스트)이라 가독성이 떨어짐.
     *     sendTradeNotification()과 동일한 카드+표 포맷(holding-warning.html)으로 교체.
     */
    @Async
    public void sendHoldingWarningEmail(String email, String stockDisplayName,
                                         int holdingDays, int maxHoldingDays,
                                         String buyDate, boolean urgent) {
        if (!emailConfig.isEnabled() || email == null || email.isEmpty()) {
            return;
        }

        String subject = String.format("[%s] 레버리지 ETF 보유기간 %s: %s",
                urgent ? "긴급" : "경고", urgent ? "초과" : "임박", stockDisplayName);

        Map<String, Object> variables = new HashMap<>();
        variables.put("stockDisplayName", stockDisplayName);
        variables.put("holdingDays", holdingDays);
        variables.put("maxHoldingDays", maxHoldingDays);
        variables.put("buyDate", buyDate);
        variables.put("urgent", urgent);
        variables.put("timestamp", LocalDateTime.now(KST).format(DATETIME_FORMAT));

        EmailNotificationDTO dto = EmailNotificationDTO.builder()
                .to(email)
                .subject(subject)
                .templateName("email/holding-warning")
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

        // ⭐⭐⭐ [개선] 코인 활동 여부(hasCoinActivity)와 주식 데이터 여부(stockBuyCount)를 먼저 계산해서
        // 둘 다 있으면 "일일 리포트", 코인만/주식만 있으면 각각 "코인 일일 리포트"/"주식 일일 리포트"로 제목 구분 ⭐⭐⭐
        boolean hasCoinData = report.getHasCoinActivity() == null || report.getHasCoinActivity();
        boolean hasStockData = report.getStockBuyCount() != null;

        String reportTypeLabel = (hasCoinData && hasStockData) ? "" : hasStockData ? "주식 " : "코인 ";
        String subject = String.format("[리포트] %s %s일일 리포트", report.getReportDate(), reportTypeLabel);

        Map<String, Object> variables = new HashMap<>();
        
        variables.put("date", report.getReportDate());
        variables.put("hasCoinData", hasCoinData);
        variables.put("reportTypeLabel", reportTypeLabel);
        variables.put("realizedProfit", KRW_FORMAT.format(report.getRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
        variables.put("unrealizedProfit", KRW_FORMAT.format(report.getUnrealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
        variables.put("totalProfit", KRW_FORMAT.format(report.getTotalProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
        
        // [수정] 수익률 소수점 2자리로 포맷팅
        variables.put("totalProfitRate", report.getProfitRate().setScale(2, java.math.RoundingMode.HALF_UP));
        
        variables.put("profitClass", report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "profit" : "loss");
        variables.put("buyCount", report.getBuyCount());
        variables.put("sellCount", report.getSellCount());
        variables.put("totalInvestment", KRW_FORMAT.format(report.getTotalInvestment().setScale(0, java.math.RoundingMode.HALF_UP)));
        
        variables.put("totalEvaluation", KRW_FORMAT.format(report.getTotalHoldingValue().setScale(0, java.math.RoundingMode.HALF_UP)));
        
        // [수정] 코인별 손익 포맷팅된 리스트로 변환
        List<Map<String, Object>> formattedHoldings = new ArrayList<>();
        if (report.getCoinSummaries() != null) {
            for (DailyReportDTO.CoinSummary coin : report.getCoinSummaries()) {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("coinSymbol", coin.getCoinSymbol());
                formatted.put("holdingCount", coin.getHoldingCount());
                formatted.put("totalQuantity", coin.getTotalQuantity().toPlainString());
                formatted.put("averagePrice", formatPriceForEmail(coin.getAveragePrice()));
	  formatted.put("currentPrice", formatPriceForEmail(coin.getCurrentPrice()));
                formatted.put("profitLoss", KRW_FORMAT.format(coin.getProfitLoss().setScale(0, java.math.RoundingMode.HALF_UP)));
                formatted.put("profitRate", coin.getProfitRate().setScale(2, java.math.RoundingMode.HALF_UP));
                formatted.put("isProfit", coin.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0);
                formattedHoldings.add(formatted);
            }
        }
        variables.put("holdings", formattedHoldings);

        // ⭐⭐⭐ [버그 수정] hasStockData는 메서드 맨 위에서 이미 선언됨 (중복 선언 제거) ⭐⭐⭐
        variables.put("hasStockData", hasStockData);
        if (hasStockData) {
            variables.put("stockBuyCount", report.getStockBuyCount());
            variables.put("stockSellCount", report.getStockSellCount());
            variables.put("stockRealizedProfit", KRW_FORMAT.format(report.getStockRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
            variables.put("stockUnrealizedProfit", KRW_FORMAT.format(report.getStockUnrealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
            variables.put("stockTotalProfit", KRW_FORMAT.format(report.getStockTotalProfit().setScale(0, java.math.RoundingMode.HALF_UP)));
            variables.put("stockTotalProfitRate", report.getStockProfitRate().setScale(2, java.math.RoundingMode.HALF_UP));
            variables.put("stockProfitClass", report.getStockTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "profit" : "loss");
            variables.put("stockTotalInvestment", KRW_FORMAT.format(report.getStockTotalInvestment().setScale(0, java.math.RoundingMode.HALF_UP)));
            variables.put("stockTotalEvaluation", KRW_FORMAT.format(report.getStockTotalHoldingValue().setScale(0, java.math.RoundingMode.HALF_UP)));

            List<Map<String, Object>> formattedStockHoldings = new ArrayList<>();
            if (report.getStockSummaries() != null) {
                for (DailyReportDTO.StockSummary stock : report.getStockSummaries()) {
                    Map<String, Object> formatted = new HashMap<>();
                    // ⭐⭐⭐ [Day 63 개선] 종목코드만 → "종목명 (코드)" 형태로 표시 ⭐⭐⭐
                    String stockDisplayName = (stock.getStockName() != null && !stock.getStockName().equals(stock.getStockCode()))
                            ? stock.getStockName() + " (" + stock.getStockCode() + ")"
                            : stock.getStockCode();
                    formatted.put("stockDisplayName", stockDisplayName);
                    // ⭐⭐⭐ [Day 63 추가] ETF 구분(레버리지/인버스/일반) 라벨 ⭐⭐⭐
                    formatted.put("etfTypeLabel", formatEtfTypeLabel(stock.getEtfType()));
                    formatted.put("holdingCount", stock.getHoldingCount());
                    formatted.put("totalQuantity", stock.getTotalQuantity().toPlainString());
                    formatted.put("profitLoss", KRW_FORMAT.format(stock.getProfitLoss().setScale(0, java.math.RoundingMode.HALF_UP)));
                    formatted.put("profitRate", stock.getProfitRate().setScale(2, java.math.RoundingMode.HALF_UP));
                    formatted.put("isProfit", stock.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0);
                    formattedStockHoldings.add(formatted);
                }
            }
            variables.put("stockHoldings", formattedStockHoldings);
        }
        
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
        
        String subject = String.format("[거래] ⚠️ %s 손절매 발생!", coinSymbol);
        
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
            helper.setSubject("[모니터링] 이메일 테스트");
            
            String htmlContent = """
                <div style="font-family: 'Malgun Gothic', sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #1976d2;">🤖 코인 & 주식 자동매매 시스템</h2>
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

    // ⭐ [추가] 저가코인 가격 포맷팅 유틸리티 메서드
    private String formatPriceForEmail(BigDecimal price) {
        if (price == null) return "0";
        
        // 1원 이상: 기존 KRW 포맷 (예: 130,000,000)
        if (price.compareTo(BigDecimal.ONE) >= 0) {
            return KRW_FORMAT.format(price);
        }
        // 0.01원 이상: 소수점 2자리 (예: 0.35)
        if (price.compareTo(new BigDecimal("0.01")) >= 0) {
            return String.format("%,.2f", price);
        }
        // 0.01원 미만 (SHIB 등): 유효숫자 보존 (예: 0.00824)
        return price.stripTrailingZeros().toPlainString();
    }

    // ⭐⭐⭐ [Day 63 추가] ETF 구분(LEVERAGE/INVERSE/NORMAL) → 한글 라벨 변환 ⭐⭐⭐
    private String formatEtfTypeLabel(String etfType) {
        if (etfType == null) return "-";
        return switch (etfType) {
            case "LEVERAGE" -> "레버리지";
            case "INVERSE" -> "인버스";
            case "NORMAL" -> "일반";
            default -> etfType;
        };
    }

}