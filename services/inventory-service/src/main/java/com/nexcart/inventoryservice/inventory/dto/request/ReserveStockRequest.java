package com.nexcart.inventoryservice.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveStockRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @Min(value = 1, message = "TTL must be at least 1 minute")
        Integer ttlMinutes
) {
    public ReserveStockRequest {
        if (ttlMinutes == null) {
            ttlMinutes = 15;
        }
    }
}
