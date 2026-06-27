package com.nexcart.inventoryservice.inventory.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record InventoryResponse(
        Long id,
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer reorderLevel,
        Boolean isLowStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
