package com.ryanm.loan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BaseException(String message, String code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public BaseException(String message, String code, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
} 