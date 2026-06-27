package com.nexcart.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
        UserNotFoundException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                null));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
        EmailAlreadyExistsException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {

        Map<String, String> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                fieldError -> fieldError.getField(),
                fieldError -> fieldError.getDefaultMessage(),
                (existing, replacement) -> existing
            ));

        return ResponseEntity.badRequest()
            .body(buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request) {

        Map<String, String> validationErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));

        return ResponseEntity.badRequest()
            .body(buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint violation",
                request,
                validationErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request) {

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                null));
    }

    private ApiErrorResponse buildErrorResponse(
        HttpStatus status,
        String message,
        HttpServletRequest request,
        Map<String, String> validationErrors) {

        return new ApiErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            validationErrors
        );
    }
}
