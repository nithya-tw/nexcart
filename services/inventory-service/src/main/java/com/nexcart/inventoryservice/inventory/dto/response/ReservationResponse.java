package com.nexcart.inventoryservice.inventory.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResponse(
        Long id,
        Long inventoryId,
        Long orderId,
        Integer quantity,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
