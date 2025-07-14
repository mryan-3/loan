package com.ryanm.loan.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        log.error("Base exception occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getMessage(), ex.getCode(), ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error occurred: {}", errors);
        return buildErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST,
            errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint violation occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            "Access denied",
            "ACCESS_DENIED",
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            String message,
            String code,
            HttpStatus status
    ) {
        return buildErrorResponse(message, code, status, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            String message,
            String code,
            HttpStatus status,
            Map<String, String> errors
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .errors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
} 