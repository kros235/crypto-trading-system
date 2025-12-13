package com.cryptotrading.exception;

public class TradingException extends BusinessException {
    
    public TradingException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public TradingException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}