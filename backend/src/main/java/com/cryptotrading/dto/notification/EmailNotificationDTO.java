package com.cryptotrading.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDTO {
    
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> variables;
    private LocalDateTime sentAt;
    private boolean success;
    private String errorMessage;
    
    // 간단한 텍스트 이메일용
    private String textContent;
    private String htmlContent;
}