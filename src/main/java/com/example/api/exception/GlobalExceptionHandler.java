package com.example.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        request.getDescription(false),
                        LocalDateTime.now()
                ),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.error("Authentication error: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        ex.getMessage(),
                        request.getDescription(false),
                        LocalDateTime.now()
                ),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        log.error("User already exists: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        ex.getMessage(),
                        request.getDescription(false),
                        LocalDateTime.now()
                ),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        "You don't have permission to access this resource",
                        request.getDescription(false),
                        LocalDateTime.now()
                ),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);
        return new ResponseEntity<>(
                new ValidationErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed",
                        request.getDescription(false),
                        LocalDateTime.now(),
                        errors
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred",
                        request.getDescription(false),
                        LocalDateTime.now()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private String path;
        private LocalDateTime timestamp;
    }

    @Data
    @AllArgsConstructor
    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> errors;

        public ValidationErrorResponse(int status, String message, String path, LocalDateTime timestamp, Map<String, String> errors) {
            super(status, message, path, timestamp);
            this.errors = errors;
        }
    }
}
