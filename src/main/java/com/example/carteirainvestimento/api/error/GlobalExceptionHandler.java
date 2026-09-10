package com.example.carteirainvestimento.api.error;

import java.time.Instant;
import java.util.List;

import com.example.carteirainvestimento.exception.ApplicationException;
import com.example.carteirainvestimento.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError.FieldViolation> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiError> handleApplication(ApplicationException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception.getErrorCode());
        return response(status, exception.getErrorCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Request validation failed", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred", request, List.of());
    }

    private HttpStatus statusFor(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR, BUSINESS_RULE_VIOLATION -> HttpStatus.BAD_REQUEST;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_RESOURCE, ASSET_IN_USE -> HttpStatus.CONFLICT;
            case ASSET_MARKET_MISMATCH -> HttpStatus.UNPROCESSABLE_ENTITY;
            case EXTERNAL_INTEGRATION_ERROR -> HttpStatus.BAD_GATEWAY;
            case PROVIDER_UNAUTHORIZED -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            List<ApiError.FieldViolation> fieldErrors) {
        ApiError error = new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), code.name(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(error);
    }
}
