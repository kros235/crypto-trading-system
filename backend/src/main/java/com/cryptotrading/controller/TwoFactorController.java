package com.cryptotrading.controller;

import com.cryptotrading.dto.totp.TwoFactorSetupDTO;
import com.cryptotrading.dto.totp.TwoFactorVerifyRequest;
import com.cryptotrading.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "2FA", description = "2단계 인증 API")
public class TwoFactorController {

    private final TotpService totpService;

    /**
     * 2FA 설정 시작 (QR 코드 URL 생성)
     */
    @Operation(summary = "2FA 설정 시작", description = "QR 코드 URL을 생성합니다")
    @PostMapping("/setup")
    public ResponseEntity<?> setup2FA(Authentication authentication) {
        try {
            String userId = authentication.getName();
            TotpService.TwoFactorSetupResult result = totpService.setup2FA(userId);

            TwoFactorSetupDTO response = TwoFactorSetupDTO.builder()
                    .secret(result.secret())
                    .qrCodeUrl(result.qrCodeUrl())
                    .message("Google Authenticator 앱으로 QR 코드를 스캔하거나 비밀키를 직접 입력하세요")
                    .build();

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 설정 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 활성화 (OTP 검증 후)
     */
    @Operation(summary = "2FA 활성화", description = "OTP 코드 검증 후 2FA를 활성화합니다")
    @PostMapping("/enable")
    public ResponseEntity<?> enable2FA(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            totpService.enable2FA(userId, request.getOtpCode());

            Map<String, String> response = new HashMap<>();
            response.put("message", "2FA가 활성화되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 활성화 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 비활성화
     */
    @Operation(summary = "2FA 비활성화", description = "OTP 코드 검증 후 2FA를 비활성화합니다")
    @PostMapping("/disable")
    public ResponseEntity<?> disable2FA(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            totpService.disable2FA(userId, request.getOtpCode());

            Map<String, String> response = new HashMap<>();
            response.put("message", "2FA가 비활성화되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 비활성화 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 상태 확인
     */
    @Operation(summary = "2FA 상태 확인", description = "현재 사용자의 2FA 활성화 상태를 확인합니다")
    @GetMapping("/status")
    public ResponseEntity<?> get2FAStatus(Authentication authentication) {
        String userId = authentication.getName();
        boolean enabled = totpService.is2FAEnabled(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * 2FA 필요 여부 확인 (로그인 전 - 인증 불필요)
     */
    @Operation(summary = "2FA 필요 여부 확인", description = "특정 사용자의 2FA 필요 여부를 확인합니다 (로그인 전 호출)")
    @GetMapping("/required/{userId}")
    public ResponseEntity<?> check2FARequired(@PathVariable String userId) {
        boolean required = totpService.requires2FA(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("required", required);
        return ResponseEntity.ok(response);
    }
}