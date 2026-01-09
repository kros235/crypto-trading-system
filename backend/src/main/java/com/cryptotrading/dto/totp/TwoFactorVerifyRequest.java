package com.cryptotrading.dto.totp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotBlank(message = "OTP 코드를 입력해주세요")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP 코드는 6자리 숫자입니다")
    private String otpCode;
}