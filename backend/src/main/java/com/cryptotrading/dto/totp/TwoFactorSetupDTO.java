package com.cryptotrading.dto.totp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupDTO {
    private String secret;         // Base32 인코딩된 비밀키
    private String qrCodeUrl;   
    private String message;     // 결과 메시지
}