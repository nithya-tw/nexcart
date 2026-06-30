package com.nexcart.inventoryservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests API error responses for different exception types.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    void shouldHandleResourceNotFoundExceptionWith404Status() {
        // ARRANGE
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");
        
        // ACT
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleResourceNotFound(exception);
        
        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Product not found");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle InsufficientStockException with 409 CONFLICT status")
    void shouldHandleInsufficientStockExceptionWith409ConflictStatus() {
        // ARRANGE
        InsufficientStockException exception = 
                new InsufficientStockException("Not enough stock available");
        
        // ACT
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInsufficientStock(exception);
        
        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Not enough stock available");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle validation errors with field-level error map")
    void shouldHandleValidationErrorsWithFieldLevelErrorMap() {
        // ARRANGE
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = 
                new MethodArgumentNotValidException(null, bindingResult);
        
        FieldError fieldError1 = new FieldError("updateStockRequest", "quantity", 
                "must be greater than 0");
        FieldError fieldError2 = new FieldError("updateStockRequest", "productId", 
                "must not be null");
        
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        // ACT
        ResponseEntity<Map<String, String>> response = 
                exceptionHandler.handleValidationExceptions(exception);
        
        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("quantity", "must be greater than 0");
        assertThat(response.getBody()).containsEntry("productId", "must not be null");
    }

    @Test
    @DisplayName("Should handle generic exceptions with 500 INTERNAL_SERVER_ERROR")
    void shouldHandleGenericExceptionsWith500InternalServerError() {
        // ARRANGE
        Exception exception = new RuntimeException("Unexpected database error");
        
        // ACT
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleGenericException(exception);
        
        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message())
                .contains("An unexpected error occurred")
                .contains("Unexpected database error");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create ErrorResponse record with all fields")
    void shouldCreateErrorResponseRecordWithAllFields() {
        // ARRANGE
        int status = 404;
        String message = "Test error message";
        
        // ACT
        GlobalExceptionHandler.ErrorResponse errorResponse = 
                new GlobalExceptionHandler.ErrorResponse(status, message, 
                        java.time.LocalDateTime.now());
        
        // ASSERT
        assertThat(errorResponse.status()).isEqualTo(status);
        assertThat(errorResponse.message()).isEqualTo(message);
        assertThat(errorResponse.timestamp()).isNotNull();
    }
}
