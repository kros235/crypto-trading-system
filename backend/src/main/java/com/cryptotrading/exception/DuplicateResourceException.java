package com.cryptotrading.exception;

public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public DuplicateResourceException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}