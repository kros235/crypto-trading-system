package com.cryptotrading.config.security;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.common.ApiResponse.ErrorResponse;
import com.cryptotrading.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("인증 실패: {} - {}", request.getRequestURI(), authException.getMessage());

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        
        // JWT 관련 에러 메시지 확인
        String message = (String) request.getAttribute("jwt-error-message");
        if (message != null) {
            if (message.contains("expired")) {
                errorCode = ErrorCode.EXPIRED_TOKEN;
            } else if (message.contains("signature") || message.contains("invalid")) {
                errorCode = ErrorCode.INVALID_TOKEN;
            }
        }

        ErrorResponse error = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(error);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}