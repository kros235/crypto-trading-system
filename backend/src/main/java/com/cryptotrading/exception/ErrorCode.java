package com.cryptotrading.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // ===== Common (1000번대) =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C003", "잘못된 타입입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "C006", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    
    // ===== Authentication (2000번대) =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A005", "아이디 또는 비밀번호가 일치하지 않습니다."),
    
    // ===== User (3000번대) =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U003", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U005", "현재 비밀번호가 일치하지 않습니다."),
    
    // ===== API Key (4000번대) =====
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "K001", "API 키가 등록되지 않았습니다."),
    API_KEY_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "K002", "API 키 암호화에 실패했습니다."),
    API_KEY_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "K003", "API 키 복호화에 실패했습니다."),
    INVALID_API_KEY(HttpStatus.BAD_REQUEST, "K004", "유효하지 않은 API 키입니다."),
    
    // ===== Trading (5000번대) =====
    TRADING_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "거래 설정을 찾을 수 없습니다."),
    DUPLICATE_TRADING_SETTING(HttpStatus.CONFLICT, "T002", "이미 거래 설정이 존재합니다."),
    INVALID_TRADING_SETTING(HttpStatus.BAD_REQUEST, "T003", "잘못된 거래 설정입니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T004", "일일 거래 한도를 초과했습니다."),
    MAX_HOLDINGS_EXCEEDED(HttpStatus.BAD_REQUEST, "T005", "종목당 최대 보유 건수를 초과했습니다."),
    
    // ===== Transaction (6000번대) =====
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "X001", "거래 내역을 찾을 수 없습니다."),
    TRANSACTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "X002", "해당 거래에 대한 접근 권한이 없습니다."),
    INVALID_TRANSACTION_STATUS(HttpStatus.BAD_REQUEST, "X003", "잘못된 거래 상태입니다."),
    SELL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "X004", "보유 중인 자산만 매도할 수 있습니다."),
    INVALID_SELL_PRICE(HttpStatus.BAD_REQUEST, "X005", "유효한 매도 가격을 입력해주세요."),
    
    // ===== Upbit API (7000번대) =====
    UPBIT_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "P001", "업비트 API 호출에 실패했습니다."),
    UPBIT_MARKET_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "존재하지 않는 마켓입니다."),
    UPBIT_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "P003", "잔고가 부족합니다."),
    UPBIT_ORDER_FAILED(HttpStatus.BAD_REQUEST, "P004", "주문 처리에 실패했습니다."),
    UPBIT_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "P005", "API 요청 한도를 초과했습니다."),
    UPBIT_MAINTENANCE(HttpStatus.SERVICE_UNAVAILABLE, "P006", "업비트 점검 중입니다."),
    
    // ===== Notification (8000번대) =====
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N001", "알림 발송에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "이메일 발송에 실패했습니다."),
    DISCORD_WEBHOOK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N003", "Discord 웹훅 발송에 실패했습니다."),
    EMAIL_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, "N004", "이메일이 설정되지 않았습니다."),
    
    // ===== Backtest (9000번대) =====
    BACKTEST_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "B001", "백테스트 기간이 유효하지 않습니다."),
    BACKTEST_NO_DATA(HttpStatus.NOT_FOUND, "B002", "백테스트 데이터가 부족합니다."),
    BACKTEST_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B003", "백테스트 실행에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}