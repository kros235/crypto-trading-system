package com.cryptotrading.controller;

import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.LoginRequest;
import com.cryptotrading.dto.SignupRequest;
import com.cryptotrading.dto.common.ApiResponse; 
import com.cryptotrading.dto.common.ApiResponse.ErrorResponse;  
import com.cryptotrading.exception.ErrorCode;  
import com.cryptotrading.service.AuthService;
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

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 검증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("로그인 실패: {}", e.getMessage());
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
}