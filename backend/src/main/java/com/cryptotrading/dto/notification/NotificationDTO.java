package com.cryptotrading.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    public enum NotificationType {
        BUY_EXECUTED,      // 매수 체결
        SELL_EXECUTED,     // 매도 체결
        STOP_LOSS,         // 손절매
        TRAILING_STOP,     // 트레일링 스톱
        DAILY_REPORT,      // 일일 리포트
        SYSTEM_ERROR,      // 시스템 오류
        BOT_STATUS         // 봇 상태
    }
    
    private NotificationType type;
    private String userId;
    private String coinSymbol;
    private String message;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal amount;
    private BigDecimal profitLoss;
    private BigDecimal profitRate;
    private LocalDateTime timestamp;
    private String additionalInfo;
}