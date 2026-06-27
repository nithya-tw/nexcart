package com.nexcart.inventoryservice.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @Min(value = 0, message = "Reorder level cannot be negative")
        Integer reorderLevel
) {
}
