package com.ryanm.loan.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.error("Validation exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            ex.getMessage(),
            "VALIDATION_ERROR",
            HttpStatus.UNPROCESSABLE_ENTITY,
            Map.of("error", ex.getMessage())
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            ex.getMessage(),
            "AUTHENTICATION_ERROR",
            HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            ex.getMessage(),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable: {}", ex.getMessage(), ex);
        
        String message = "Request body is required";
        String code = "MISSING_REQUEST_BODY";
        
        // Check for specific JSON parsing errors
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Required request body is missing")) {
                message = "Request body is required for this endpoint";
            } else if (ex.getMessage().contains("JSON parse error")) {
                message = "Invalid JSON format in request body";
                code = "INVALID_JSON";
            } else if (ex.getMessage().contains("Unexpected character")) {
                message = "Malformed JSON in request body";
                code = "MALFORMED_JSON";
            }
        }
        
        return buildErrorResponse(message, code, HttpStatus.BAD_REQUEST);
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
            HttpStatus.UNPROCESSABLE_ENTITY,
            errors
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        log.error("Missing request parameter: {}", ex.getMessage(), ex);
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return buildErrorResponse(message, "MISSING_PARAMETER", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariable(MissingPathVariableException ex) {
        log.error("Missing path variable: {}", ex.getMessage(), ex);
        String message = String.format("Required path variable '%s' is missing", ex.getVariableName());
        return buildErrorResponse(message, "MISSING_PATH_VARIABLE", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Type mismatch: {}", ex.getMessage(), ex);
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        return buildErrorResponse(message, "TYPE_MISMATCH", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("Method not supported: {}", ex.getMessage(), ex);
        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s", 
            ex.getMethod(), ex.getSupportedHttpMethods());
        return buildErrorResponse(message, "METHOD_NOT_SUPPORTED", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.error("No handler found: {}", ex.getMessage(), ex);
        String message = String.format("Endpoint '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL());
        return buildErrorResponse(message, "ENDPOINT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint violation occurred: {}", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return buildErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST,
            errors
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            "Access denied. You don't have permission to access this resource",
            "ACCESS_DENIED",
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.error("Business rule conflict: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            ex.getMessage(),
            ex.getCode(),
            ex.getStatus(),
            Map.of("error", ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            "An unexpected error occurred. Please try again later",
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