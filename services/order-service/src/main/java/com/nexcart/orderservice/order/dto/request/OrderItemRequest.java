package com.nexcart.orderservice.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,
        
        @NotBlank(message = "Product name is required")
        String productName,
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be non-negative")
        BigDecimal unitPrice
) {
}
