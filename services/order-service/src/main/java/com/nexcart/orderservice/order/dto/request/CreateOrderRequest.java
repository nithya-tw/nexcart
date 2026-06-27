package com.nexcart.orderservice.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        Long userId,
        
        @NotBlank(message = "Shipping address is required")
        String shippingAddress,
        
        String paymentMethod,
        
        String notes,
        
        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}
