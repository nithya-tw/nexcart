package com.nexcart.inventoryservice.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Inventory entity.
 * Tests business logic for stock management including low stock detection and availability.
 */
@DisplayName("Inventory Entity Tests")
class InventoryEntityTest {

    @Test
    @DisplayName("Should identify low stock when quantity equals reorder level")
    void shouldIdentifyLowStockWhenQuantityEqualsReorderLevel() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(15)
                .reservedQuantity(5)
                .reorderLevel(10)
                .build();
        inventory.setAvailableQuantity(10); // Simulating DB computed column
        
        // ACT
        boolean isLow = inventory.isLowStock();
        
        // ASSERT
        assertThat(isLow).isTrue();
    }

    @Test
    @DisplayName("Should identify low stock when quantity below reorder level")
    void shouldIdentifyLowStockWhenQuantityBelowReorderLevel() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(12)
                .reservedQuantity(5)
                .reorderLevel(10)
                .build();
        inventory.setAvailableQuantity(7); // Below reorder level
        
        // ACT & ASSERT
        assertThat(inventory.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("Should not identify low stock when quantity above reorder level")
    void shouldNotIdentifyLowStockWhenQuantityAboveReorderLevel() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .reorderLevel(10)
                .build();
        inventory.setAvailableQuantity(40);
        
        // ACT & ASSERT
        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("Should return false for low stock when available quantity is null")
    void shouldReturnFalseForLowStockWhenAvailableQuantityIsNull() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .reorderLevel(10)
                .build();
        // availableQuantity not set (null)
        
        // ACT & ASSERT
        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("Should confirm sufficient stock when requested quantity available")
    void shouldConfirmSufficientStockWhenRequestedQuantityAvailable() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        inventory.setAvailableQuantity(40);
        
        // ACT
        boolean hasStock = inventory.hasAvailableStock(30);
        
        // ASSERT
        assertThat(hasStock).isTrue();
    }

    @Test
    @DisplayName("Should confirm sufficient stock when requested exactly equals available")
    void shouldConfirmSufficientStockWhenRequestedExactlyEqualsAvailable() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        inventory.setAvailableQuantity(40);
        
        // ACT & ASSERT
        assertThat(inventory.hasAvailableStock(40)).isTrue();
    }

    @Test
    @DisplayName("Should reject when insufficient stock")
    void shouldRejectWhenInsufficientStock() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        inventory.setAvailableQuantity(40);
        
        // ACT
        boolean hasStock = inventory.hasAvailableStock(50); // More than available
        
        // ASSERT
        assertThat(hasStock).isFalse();
    }

    @Test
    @DisplayName("Should return false when available quantity is null")
    void shouldReturnFalseWhenAvailableQuantityIsNull() {
        // ARRANGE
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        // availableQuantity not set
        
        // ACT & ASSERT
        assertThat(inventory.hasAvailableStock(10)).isFalse();
    }

    @Test
    @DisplayName("Should initialize with default values using builder")
    void shouldInitializeWithDefaultValuesUsingBuilder() {
        // ACT
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .build();
        
        // ASSERT
        assertThat(inventory.getQuantity()).isZero();
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getReorderLevel()).isEqualTo(10);
        assertThat(inventory.getReservations()).isEmpty();
    }

    @Test
    @DisplayName("Should support all args constructor")
    void shouldSupportAllArgsConstructor() {
        // ACT
        Inventory inventory = new Inventory(
                1L,
                100L,
                50,
                10,
                40,
                10,
                null,
                null,
                null
        );
        
        // ASSERT
        assertThat(inventory.getId()).isEqualTo(1L);
        assertThat(inventory.getProductId()).isEqualTo(100L);
        assertThat(inventory.getQuantity()).isEqualTo(50);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
    }
}
