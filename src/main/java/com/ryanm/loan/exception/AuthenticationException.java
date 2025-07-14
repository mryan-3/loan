package com.ryanm.loan.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {
    public AuthenticationException(String message) {
        super(message, "AUTH_ERROR", HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, "AUTH_ERROR", HttpStatus.UNAUTHORIZED, cause);
    }
} 