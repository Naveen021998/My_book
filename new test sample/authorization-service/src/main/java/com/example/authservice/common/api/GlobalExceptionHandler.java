package com.example.authservice.common.api;

import com.example.authservice.exception.AuthenticationFailedException;
import com.example.authservice.exception.ResourceConflictException;
import com.example.authservice.exception.TokenValidationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed.",
                details,
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.error("Data integrity violation (correlationId={}): {}", MDC.get("correlationId"), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "DATA_INTEGRITY_VIOLATION",
                "Request conflicts with existing data.",
                List.of(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(JwtEncodingException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtEncoding(JwtEncodingException exception) {
        log.error("JWT encoding error (correlationId={}): {}", MDC.get("correlationId"), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                "JWT_ENCODING_ERROR",
                "Token service is misconfigured. Please contact support.",
                List.of(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error (correlationId={}): {}", MDC.get("correlationId"), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.",
                List.of(),
                OffsetDateTime.now()
        ));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_ARGUMENT",
                exception.getMessage(),
                List.of(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ResourceConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "RESOURCE_CONFLICT",
                exception.getMessage(),
                List.of(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorResponse(
                "AUTHENTICATION_FAILED",
                exception.getMessage(),
                List.of(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenValidation(TokenValidationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorResponse(
                "TOKEN_VALIDATION_FAILED",
                exception.getMessage(),
                List.of(),
                OffsetDateTime.now()
        ));
    }
}
