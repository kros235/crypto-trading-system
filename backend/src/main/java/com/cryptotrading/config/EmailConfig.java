package com.cryptotrading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {
    
    @Value("${app.email.enabled:false}")
    private boolean enabled;
    
    @Value("${app.email.from:noreply@cryptobot.com}")
    private String fromAddress;
    
    @Value("${app.email.from-name:코인 & 주식 자동매매 시스템}")
    private String fromName;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public String getFromName() {
        return fromName;
    }
}