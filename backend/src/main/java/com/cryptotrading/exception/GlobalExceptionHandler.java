package com.cryptotrading.exception;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.common.ApiResponse.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리 (커스텀 비즈니스 예외)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: code={}, message={}, detail={}", 
                ex.getErrorCode().getCode(), 
                ex.getErrorCode().getMessage(), 
                ex.getDetail());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .detail(ex.getDetail())
                .build();
        
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(error));
    }

    /**
     * EntityNotFoundException 처리 (리소스 미발견)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("EntityNotFoundException: code={}, detail={}", 
                ex.getErrorCode().getCode(), 
                ex.getDetail());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .detail(ex.getDetail())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error));
    }

    /**
     * UpbitApiException 처리 (업비트 API 오류)
     */
    @ExceptionHandler(UpbitApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpbitApiException(UpbitApiException ex) {
        log.error("UpbitApiException: code={}, upbitCode={}, upbitMessage={}", 
                ex.getErrorCode().getCode(),
                ex.getUpbitErrorCode(),
                ex.getUpbitErrorMessage());
        
        String detail = ex.getUpbitErrorCode() != null 
                ? String.format("[%s] %s", ex.getUpbitErrorCode(), ex.getUpbitErrorMessage())
                : ex.getDetail();
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .detail(detail)
                .build();
        
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(error));
    }

    /**
     * Validation 예외 처리 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("ValidationException: fields={}", fieldErrors);
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(ErrorCode.INVALID_INPUT_VALUE.getMessage())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    /**
     * 요청 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        
        log.warn("MissingParameterException: param={}", ex.getParameterName());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message("필수 파라미터가 누락되었습니다.")
                .detail(ex.getParameterName() + " 파라미터가 필요합니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        
        log.warn("TypeMismatchException: param={}, value={}", 
                ex.getName(), ex.getValue());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_TYPE_VALUE.getCode())
                .message(ErrorCode.INVALID_TYPE_VALUE.getMessage())
                .detail(ex.getName() + " 파라미터의 타입이 올바르지 않습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    /**
     * JSON 파싱 오류 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        
        log.warn("MessageNotReadableException: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message("요청 본문을 파싱할 수 없습니다.")
                .detail("JSON 형식을 확인해주세요.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    /**
     * HTTP 메서드 불일치 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        
        log.warn("MethodNotSupportedException: method={}", ex.getMethod());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.METHOD_NOT_ALLOWED.getCode())
                .message(ErrorCode.METHOD_NOT_ALLOWED.getMessage())
                .detail(ex.getMethod() + " 메서드는 지원되지 않습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(error));
    }

    /**
     * 404 Not Found 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex) {
        
        log.warn("NoHandlerFoundException: path={}", ex.getRequestURL());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message(ErrorCode.RESOURCE_NOT_FOUND.getMessage())
                .detail(ex.getRequestURL() + " 경로를 찾을 수 없습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error));
    }

    /**
     * 인증 실패 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex) {
        
        log.warn("AuthenticationException: {}", ex.getMessage());
        
        ErrorCode errorCode = ex instanceof BadCredentialsException 
                ? ErrorCode.LOGIN_FAILED 
                : ErrorCode.UNAUTHORIZED;
        
        ErrorResponse error = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(error));
    }

    /**
     * 접근 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex) {
        
        log.warn("AccessDeniedException: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.ACCESS_DENIED.getCode())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(error));
    }

    /**
     * IllegalArgumentException 처리 (기존 코드 호환)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(ex.getMessage())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    /**
     * RuntimeException 처리 (일반 런타임 예외)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생했습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error));
    }

    /**
     * 최상위 Exception 처리 (예상치 못한 예외)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected Exception: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("서버 오류가 발생했습니다.")
                .detail("잠시 후 다시 시도해주세요.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error));
    }
}