package com.ryanm.loan.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message, "BUSINESS_CONFLICT", HttpStatus.CONFLICT);
    }
} 