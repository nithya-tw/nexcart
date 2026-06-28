package com.nexcart.orderservice.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for inventory availability from Inventory Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAvailabilityResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer reorderLevel;
    private Boolean isLowStock;
}
