package com.example.carteirainvestimento.api.error;

import java.time.Instant;
import java.util.List;

import com.example.carteirainvestimento.exception.ApplicationException;
import com.example.carteirainvestimento.exception.ErrorCode;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        HttpStatus status = exception instanceof ResourceNotFoundException ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        if (exception.getErrorCode() == ErrorCode.EXTERNAL_INTEGRATION_ERROR) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return response(status, exception.getErrorCode(), exception.getMessage(), request, List.of());
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
