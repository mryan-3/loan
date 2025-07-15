package com.ryanm.loan.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
} 