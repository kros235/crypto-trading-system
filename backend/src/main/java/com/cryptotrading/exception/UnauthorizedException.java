package com.cryptotrading.exception;

public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public UnauthorizedException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}