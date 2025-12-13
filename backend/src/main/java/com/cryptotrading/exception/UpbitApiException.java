package com.cryptotrading.exception;

import lombok.Getter;

@Getter
public class UpbitApiException extends BusinessException {
    
    private final String upbitErrorCode;
    private final String upbitErrorMessage;
    
    public UpbitApiException(ErrorCode errorCode) {
        super(errorCode);
        this.upbitErrorCode = null;
        this.upbitErrorMessage = null;
    }
    
    public UpbitApiException(ErrorCode errorCode, String upbitErrorCode, String upbitErrorMessage) {
        super(errorCode, upbitErrorMessage);
        this.upbitErrorCode = upbitErrorCode;
        this.upbitErrorMessage = upbitErrorMessage;
    }
    
    public UpbitApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.upbitErrorCode = null;
        this.upbitErrorMessage = cause.getMessage();
    }
}