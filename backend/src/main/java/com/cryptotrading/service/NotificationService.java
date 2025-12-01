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
    private final WebClient webClient = WebClient.create();
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 매수 체결 알림
     */
    public void notifyBuyExecuted(String userId, String coinSymbol, BigDecimal price, 
                                   BigDecimal quantity, BigDecimal amount) {
        NotificationDTO notification = NotificationDTO.builder()
                .type(NotificationType.BUY_EXECUTED)
                .userId(userId)
                .coinSymbol(coinSymbol)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .build();
        
        String message = formatBuyMessage(notification);
        sendDiscordNotification(message, 0x00FF00);  // 녹색
        
        log.info("매수 알림 발송: {} - {} {}원", userId, coinSymbol, amount);
    }

    /**
     * 매도 체결 알림
     */
    public void notifySellExecuted(String userId, String coinSymbol, BigDecimal price,
                                    BigDecimal quantity, BigDecimal amount, 
                                    BigDecimal profitLoss, BigDecimal profitRate) {
        NotificationDTO notification = NotificationDTO.builder()
                .type(NotificationType.SELL_EXECUTED)
                .userId(userId)
                .coinSymbol(coinSymbol)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .profitLoss(profitLoss)
                .profitRate(profitRate)
                .timestamp(LocalDateTime.now())
                .build();
        
        String message = formatSellMessage(notification);
        int color = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? 0x00FF00 : 0xFF0000;
        sendDiscordNotification(message, color);
        
        log.info("매도 알림 발송: {} - {} {}원 (손익: {}원)", userId, coinSymbol, amount, profitLoss);
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
        return String.format("""
            💰 **매수 체결 알림**
            
            📊 **코인**: %s
            💵 **매수가**: %s 원
            📦 **수량**: %s
            💳 **총액**: %s 원
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatNumber(notification.getPrice()),
            formatQuantity(notification.getQuantity()),
            formatNumber(notification.getAmount()),
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    private String formatSellMessage(NotificationDTO notification) {
        String profitEmoji = notification.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0 ? "📈" : "📉";
        String profitSign = notification.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        return String.format("""
            💸 **매도 체결 알림**
            
            📊 **코인**: %s
            💵 **매도가**: %s 원
            📦 **수량**: %s
            💳 **총액**: %s 원
            
            %s **손익**: %s%s 원 (%s%s%%)
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatNumber(notification.getPrice()),
            formatQuantity(notification.getQuantity()),
            formatNumber(notification.getAmount()),
            profitEmoji,
            profitSign, formatNumber(notification.getProfitLoss()),
            profitSign, notification.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    private String formatStopLossMessage(NotificationDTO notification) {
        return String.format("""
            🚨 **손절매 실행 알림**
            
            📊 **코인**: %s
            💵 **매도가**: %s 원
            📉 **손실**: %s 원 (%s%%)
            
            ⚠️ 손절매 기준에 도달하여 자동 매도되었습니다.
            
            👤 사용자: %s
            ⏰ 체결 시간: %s
            """,
            notification.getCoinSymbol(),
            formatNumber(notification.getPrice()),
            formatNumber(notification.getProfitLoss()),
            notification.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            notification.getUserId(),
            notification.getTimestamp().format(TIME_FORMATTER)
        );
    }

    private String formatDailyReport(DailyReportDTO report) {
        String profitEmoji = report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "📈" : "📉";
        String profitSign = report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            📊 **일일 거래 리포트**
            📅 %s
            
            ━━━━━━━━━━━━━━━━━━━━━━
            
            💼 **거래 요약**
            • 매수: %d건 (%s원)
            • 매도: %d건 (%s원)
            
            %s **손익 현황**
            • 실현 손익: %s%s원
            • 평가 손익: %s%s원
            • **총 손익**: %s%s원 (%s%s%%)
            
            📦 **보유 현황**
            • 보유 코인: %d종목
            • 총 평가액: %s원
            • 투자 원금: %s원
            
            """,
            report.getReportDate().format(DATE_FORMATTER),
            report.getBuyCount(), formatNumber(report.getTotalBuyAmount()),
            report.getSellCount(), formatNumber(report.getTotalSellAmount()),
            profitEmoji,
            profitSign, formatNumber(report.getRealizedProfit()),
            report.getUnrealizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", 
            formatNumber(report.getUnrealizedProfit()),
            profitSign, formatNumber(report.getTotalProfit()),
            profitSign, report.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            report.getHoldingCount(),
            formatNumber(report.getTotalHoldingValue()),
            formatNumber(report.getTotalInvestment())
        ));
        
        // 코인별 상세 (있는 경우)
        if (report.getCoinSummaries() != null && !report.getCoinSummaries().isEmpty()) {
            sb.append("📋 **코인별 현황**\n");
            for (DailyReportDTO.CoinSummary coin : report.getCoinSummaries()) {
                String coinProfitSign = coin.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                sb.append(String.format("• %s: %s%s원 (%s%s%%)\n",
                    coin.getCoinSymbol(),
                    coinProfitSign, formatNumber(coin.getProfitLoss()),
                    coinProfitSign, coin.getProfitRate().setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }
        
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

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) return "0";
        return quantity.stripTrailingZeros().toPlainString();
    }
}