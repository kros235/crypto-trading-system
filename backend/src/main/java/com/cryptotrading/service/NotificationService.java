package com.cryptotrading.service;

import com.cryptotrading.config.NotificationConfig;
import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.dto.notification.NotificationDTO;
import com.cryptotrading.dto.notification.NotificationDTO.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationConfig notificationConfig;
    // ⭐⭐⭐ [추가] 코인/주식 섹션 텍스트를 DM과 동일하게 생성하기 위한 공용 포맷터 ⭐⭐⭐
    private final ReportFormatterService reportFormatterService;
    private final WebClient webClient = WebClient.create();
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 매수 체결 알림
     */
    public void notifyBuyExecuted(String userId, String coinSymbol, BigDecimal price, 
                                   BigDecimal quantity, BigDecimal amount, String reason) {
        NotificationDTO notification = NotificationDTO.builder()
                .type(NotificationType.BUY_EXECUTED)
                .userId(userId)
                .coinSymbol(coinSymbol)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .reason(reason)  // ⭐⭐⭐ [추가] reason 설정 ⭐⭐⭐
                .timestamp(LocalDateTime.now())
                .build();
        
        String message = formatBuyMessage(notification);
        sendDiscordNotification(message, 0x00FF00);  // 녹색
        
        log.info("매수 알림 발송: {} - {} {}원 (사유: {})", userId, coinSymbol, amount, reason);
    }

    /**
     * 매도 체결 알림
     */
    public void notifySellExecuted(String userId, String coinSymbol, BigDecimal price,
                                    BigDecimal quantity, BigDecimal amount, 
                                    BigDecimal profitLoss, BigDecimal profitRate, String reason) {
        NotificationDTO notification = NotificationDTO.builder()
                .type(NotificationType.SELL_EXECUTED)
                .userId(userId)
                .coinSymbol(coinSymbol)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .profitLoss(profitLoss)
                .profitRate(profitRate)
                .reason(reason)  // ⭐⭐⭐ [추가] reason 설정 ⭐⭐⭐
                .timestamp(LocalDateTime.now())
                .build();
        
        String message = formatSellMessage(notification);
        int color = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? 0x00FF00 : 0xFF0000;
        sendDiscordNotification(message, color);
        
        log.info("매도 알림 발송: {} - {} {}원 (손익: {}원, 사유: {})", userId, coinSymbol, amount, profitLoss, reason);
    }

    /**
     * 손절매 알림
     */
    public void notifyStopLoss(String userId, String coinSymbol, BigDecimal price,
                                BigDecimal profitLoss, BigDecimal profitRate) {
        NotificationDTO notification = NotificationDTO.builder()
                .type(NotificationType.STOP_LOSS)
                .userId(userId)
                .coinSymbol(coinSymbol)
                .price(price)
                .profitLoss(profitLoss)
                .profitRate(profitRate)
                .timestamp(LocalDateTime.now())
                .build();
        
        String message = formatStopLossMessage(notification);
        sendDiscordNotification(message, 0xFF0000);  // 빨간색
        
        log.warn("손절매 알림 발송: {} - {} (손실: {}원)", userId, coinSymbol, profitLoss);
    }

    /**
     * 일일 리포트 발송
     */
    public void sendDailyReport(DailyReportDTO report) {
        String message = formatDailyReport(report);
        int color = report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? 0x00FF00 : 0xFF0000;
        sendDiscordNotification(message, color);
        
        log.info("일일 리포트 발송: {} - 수익: {}원", report.getUserId(), report.getTotalProfit());
    }

    /**
     * ⭐⭐⭐ [Day 63 추가] 주식 보유기간 경고 메시지 빌더 (public) ⭐⭐⭐
     * 왜: StockTradingScheduler.buildHoldingDaysWarningMessage()와 동일 내용이지만 그 메서드는 private이라
     *     NotificationController의 테스트 엔드포인트에서 재사용할 수 없음. 운영 중인 스케줄러 코드는
     *     그대로 두고(불필요한 리팩토링 회피), 테스트 엔드포인트 전용으로 공개 메서드를 추가함.
     */
    public String buildStockHoldingWarningMessage(
            com.cryptotrading.service.StockRiskManagementService.HoldingDaysWarning warning, int maxHoldingDays) {

        String urgencyText = warning.isUrgent()
                ? "🚨 최대 보유기간에 도달했습니다!"
                : "⚠️ 보유기간 경고 임계값에 도달했습니다.";

        return String.format(
                "%s\n\n" +
                "• 종목: %s\n" +
                "• 현재 보유기간: %d 거래일\n" +
                "• 최대 보유기간: %d 거래일\n" +
                "• 매수일: %s\n\n" +
                "레버리지 ETF 장기 보유 시 변동성 끌림(Volatility Drag)으로\n" +
                "인한 가치 침식이 발생할 수 있습니다.\n" +
                "포지션 정리를 검토해 주세요.",
                urgencyText,
                warning.getStockCode(),
                warning.getHoldingDays(),
                maxHoldingDays,
                warning.getBuyDate());
    }

    /**
     * 봇 상태 알림
     */
    public void notifyBotStatus(String message, boolean isError) {
        int color = isError ? 0xFF0000 : 0x0099FF;
        sendDiscordNotification(message, color);
    }

    /**
     * 시스템 오류 알림
     */
    public void notifySystemError(String errorMessage) {
        String message = String.format("""
            🚨 **시스템 오류 발생**
```
            %s
```
            
            ⏰ 발생 시간: %s
            """,
            errorMessage,
            LocalDateTime.now().format(TIME_FORMATTER)
        );
        
        sendDiscordNotification(message, 0xFF0000);
        log.error("시스템 오류 알림 발송: {}", errorMessage);
    }

    // ============ Private Methods ============

    private String formatBuyMessage(NotificationDTO notification) {
        // ⭐⭐⭐ [추가] 매수 사유 포맷팅 ⭐⭐⭐
        String reasonText = notification.getReason() != null && !notification.getReason().isEmpty()
                ? notification.getReason()
                : "조건 충족";
        
        return String.format("""
            💰 **매수 체결 알림**
            
            📊 **종목**: %s
            💵 **매수가**: %s 원
            📦 **수량**: %s
            💳 **총액**: %s 원
            📌 **매수 사유**: %s
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatPrice(notification.getPrice()),
            formatQuantity(notification.getQuantity()),
            formatNumber(notification.getAmount()),
            reasonText,  // ⭐⭐⭐ [추가] 매수 사유 ⭐⭐⭐
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    private String formatSellMessage(NotificationDTO notification) {
        String profitEmoji = notification.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0 ? "📈" : "📉";
        String profitSign = notification.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        // ⭐⭐⭐ [추가] 매도 사유 포맷팅 ⭐⭐⭐
        String reasonText = notification.getReason() != null && !notification.getReason().isEmpty()
                ? notification.getReason()
                : "조건 충족";
        
        return String.format("""
            💸 **매도 체결 알림**
            
            📊 **종목**: %s
            💵 **매도가**: %s 원
            📦 **수량**: %s
            💳 **총액**: %s 원
            %s **손익**: %s%s 원 (%s%s%%)
            📌 **매도 사유**: %s
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatPrice(notification.getPrice()),
            formatQuantity(notification.getQuantity()),
            formatNumber(notification.getAmount()),
            profitEmoji,
            profitSign, formatNumber(notification.getProfitLoss()),
            profitSign, notification.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            reasonText,  // ⭐⭐⭐ [추가] 매도 사유 ⭐⭐⭐
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    private String formatStopLossMessage(NotificationDTO notification) {
        return String.format("""
            🚨 **손절매 실행 알림**
            
            📊 **종목**: %s
            💵 **매도가**: %s 원
            📉 **손실**: %s 원 (%s%%)
            
            ⚠️ 손절매 기준에 도달하여 자동 매도되었습니다.
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatPrice(notification.getPrice()),
            formatNumber(notification.getProfitLoss()),
            notification.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    /**
     * ⭐⭐⭐ [수정] 코인/주식 섹션 생성을 ReportFormatterService에 위임 ⭐⭐⭐
     * 왜: 웹훅 채널과 Discord DM이 서로 다른 내용/포맷으로 리포트를 만들던 문제를 해소하기 위해,
     *     "[코인]"/"[주식]" 섹션(거래요약+손익현황+보유현황+보유종목상세) 생성 로직을 공용 서비스로
     *     추출함. 이제 웹훅과 DM이 완전히 동일한 본문 텍스트를 사용함.
     */
    private String formatDailyReport(DailyReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            📊 **일일 거래 리포트**
            📅 %s
            
            ━━━━━━━━━━━━━━━━━━━━━━
            
            """,
            report.getReportDate().format(DATE_FORMATTER)
        ));

        sb.append(reportFormatterService.buildCategorySections(report));
        sb.append(String.format("\n👤 사용자: %s", report.getUserId()));

        return sb.toString();
    }

    private void sendDiscordNotification(String message, int color) {
        if (!notificationConfig.isEnabled()) {
            log.debug("Discord 알림 비활성화 상태");
            return;
        }
        
        String webhookUrl = notificationConfig.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Discord Webhook URL이 설정되지 않았습니다");
            return;
        }
        
        try {
            String payload = String.format(
                "{\"embeds\":[{\"description\":\"%s\",\"color\":%d}]}",
                message.replace("\"", "\\\"").replace("\n", "\\n"),
                color
            );
            
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                        response -> log.debug("Discord 알림 발송 성공"),
                        error -> log.error("Discord 알림 발송 실패: {}", error.getMessage())
                    );
                    
        } catch (Exception e) {
            log.error("Discord 알림 발송 오류: {}", e.getMessage());
        }
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return String.format("%,.0f", number);
    }

    // ⭐ [추가] 저가코인 가격 포맷팅 (가격 전용 - SHIB 등 1원 미만 코인 대응)
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        // 1원 이상: 정수 표시 (기존과 동일)
        if (price.compareTo(BigDecimal.ONE) >= 0) {
            return String.format("%,.0f", price);
        }
        // 0.01원 이상: 소수점 2자리
        if (price.compareTo(new BigDecimal("0.01")) >= 0) {
            return String.format("%,.2f", price);
        }
        // 0.01원 미만 (SHIB 등): 유효숫자 보존
        return price.stripTrailingZeros().toPlainString();
    }

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) return "0";
        return quantity.stripTrailingZeros().toPlainString();
    }

    /**
     * 시스템 알림 발송 (모니터링, 서버 시작/종료 등)
     * 사용자 ID가 없는 시스템 레벨 알림용
     */
    public void sendSystemNotification(String message) {
        sendDiscordNotification(message, 0);
    }

    /**
     * ⭐ 추가: 시스템 알림 발송 (동기 - 서버 종료 시 사용)
     * 서버 종료 시 비동기 요청이 완료되기 전에 애플리케이션이 종료되는 것을 방지
     */
    public void sendSystemNotificationSync(String message) {
        if (!notificationConfig.isEnabled()) {
            log.debug("Discord 알림 비활성화 상태");
            return;
        }
        
        String webhookUrl = notificationConfig.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Discord Webhook URL이 설정되지 않았습니다");
            return;
        }
        
        try {
            String payload = String.format(
                "{\"embeds\":[{\"description\":\"%s\",\"color\":%d}]}",
                message.replace("\"", "\\\"").replace("\n", "\\n"),
                0
            );
            
            // ⭐ block()으로 동기 처리
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(10));  // 최대 10초 대기
                    
            log.debug("Discord 알림 동기 발송 성공");
                    
        } catch (Exception e) {
            log.error("Discord 알림 동기 발송 오류: {}", e.getMessage());
        }
    }
}