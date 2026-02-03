package com.shotaroi.keygateapi.errors;

import com.shotaroi.keygateapi.trace.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - invalid JSON (example: missing comma)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", "Invalid JSON request body", req, Map.of());
    }

    // 400 - validation errors (from @Valid / @Min / @NotBlank etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        details.put("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", req, details);
    }

    // 404 - not found (optional: your own thrown exception)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), req, Map.of());
    }

    // 400 - generic "you did something wrong" exception
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), req, Map.of());
    }

    // 500 - fallback: any other crash
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest req) {
        // Don't leak internal stack traces to clients
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong", req, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String error,
                                           String message,
                                           HttpServletRequest req,
                                           Map<String, Object> details) {
        Map<String, Object> merged = new HashMap<>(details);
        Object requestId = req.getAttribute(RequestIdFilter.ATTR);
        if (requestId != null) {
            merged.put("requestId", requestId.toString());
        }

        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                error,
                message,
                req.getRequestURI(),
                merged
        );
        return ResponseEntity.status(status).body(body);
    }
}
