package com.cryptotrading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;    // ⭐ 변경
import org.springframework.context.annotation.Configuration;

@Component    // ⭐ 변경: @Configuration → @Component
@ConfigurationProperties(prefix = "notification.discord")
@Data
public class NotificationConfig {
    
    private boolean enabled = false;
    private String webhookUrl;
}