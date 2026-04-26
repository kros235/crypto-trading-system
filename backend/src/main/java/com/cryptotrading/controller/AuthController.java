package com.cryptotrading.controller;

import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.LoginRequest;
import com.cryptotrading.dto.SignupRequest;
import com.cryptotrading.dto.common.ApiResponse; 
import com.cryptotrading.dto.common.ApiResponse.ErrorResponse;  
import com.cryptotrading.exception.ErrorCode;  
import com.cryptotrading.service.AuthService;
// ⭐ [추가] 비밀번호 재설정 서비스
import com.cryptotrading.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 검증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    // ⭐⭐⭐ [추가] 비밀번호 재설정 서비스 의존성 ⭐⭐⭐
    private final PasswordResetService passwordResetService;

    // === 메서드에 어노테이션 추가 예시 ===
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 사용자")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            AuthResponse response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("회원가입 실패: {}", e.getMessage());
            
            ErrorCode errorCode = e.getMessage().contains("이미 존재") 
                    ? ErrorCode.DUPLICATE_USER_ID 
                    : ErrorCode.INVALID_INPUT_VALUE;
            
            ErrorResponse error = ErrorResponse.builder()
                    .code(errorCode.getCode())
                    .message(e.getMessage())
                    .build();
            
            return ResponseEntity
                    .status(errorCode.getHttpStatus())
                    .body(ApiResponse.error(error));
        }
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "계정 잠금")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest  // IP 추출용 추가
    ) {
        try {
            // 클라이언트 IP 추출
            String clientIp = getClientIp(httpRequest);
            AuthResponse response = authService.login(request, clientIp);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("로그인 실패: {}", e.getMessage());
            
            // 2FA 필요 시 특별 응답
            if ("2FA_REQUIRED".equals(e.getMessage())) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", "2FA_REQUIRED");
                response.put("message", "2단계 인증이 필요합니다");
                response.put("requires2FA", true);
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(response);
            }
           
            ErrorResponse error = ErrorResponse.builder()
                    .code(ErrorCode.LOGIN_FAILED.getCode())
                    .message(ErrorCode.LOGIN_FAILED.getMessage())
                    .detail(e.getMessage())
                    .build();
            
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(error));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            // "Bearer " 제거
            String token = authHeader.substring(7);
            return ResponseEntity.ok(ApiResponse.success("유효한 토큰입니다")); 
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .code(ErrorCode.INVALID_TOKEN.getCode())
                    .message(ErrorCode.INVALID_TOKEN.getMessage())
                    .build();
            
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(error));
        }
    }

    // 클라이언트 IP 추출 메서드 추가
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For는 여러 IP가 쉼표로 구분될 수 있음
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // ====================================================================
    // ⭐⭐⭐ [추가] 비밀번호 재설정 (OTP 인증) 엔드포인트 ⭐⭐⭐
    // ====================================================================

    /**
     * [1단계] OTP 발송 요청
     * - 이메일 입력 → OTP 6자리 코드를 이메일로 발송
     * - 응답에는 마스킹된 이메일만 반환 (보안: 등록 여부 확인 우회 방지)
     */
    @Operation(summary = "비밀번호 재설정 OTP 발송", 
               description = "이메일로 6자리 인증 코드를 발송합니다 (3분 유효)")
    @PostMapping("/password-reset/request-otp")
    public ResponseEntity<Map<String, Object>> requestPasswordResetOtp(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                response.put("success", false);
                response.put("message", "이메일을 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            String maskedEmail = passwordResetService.requestOtp(email);

            response.put("success", true);
            response.put("message", "인증 코드가 이메일로 발송되었습니다.");
            response.put("maskedEmail", maskedEmail);
            response.put("expiryMinutes", 3);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("OTP 발송 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * [2단계] OTP 검증 + 임시 비밀번호 발급
     * - 이메일 + 6자리 OTP 코드로 검증
     * - 검증 성공 시 신규 임시 비밀번호 평문을 1회 응답으로 반환
     * - 응답 후 사용자는 즉시 로그인 후 본인이 원하는 비밀번호로 변경해야 함
     */
    @Operation(summary = "비밀번호 재설정 OTP 검증", 
               description = "OTP 검증 후 임시 비밀번호를 발급합니다")
    @PostMapping("/password-reset/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyPasswordResetOtp(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();
        try {
            String email = request.get("email");
            String otp = request.get("otp");

            if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
                response.put("success", false);
                response.put("message", "이메일과 인증 코드를 모두 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            String tempPassword = passwordResetService.verifyOtpAndReset(email, otp);

            response.put("success", true);
            response.put("message", "임시 비밀번호가 발급되었습니다. 로그인 후 즉시 비밀번호를 변경해주세요.");
            response.put("tempPassword", tempPassword);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("OTP 검증 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}