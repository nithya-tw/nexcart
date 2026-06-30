package com.nexcart.inventoryservice.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StockReservation entity.
 * Tests reservation expiration logic for automatic cart release.
 */
@DisplayName("StockReservation Entity Tests")
class StockReservationEntityTest {

    @Test
    @DisplayName("Should identify expired reservation when time passed and status is RESERVED")
    void shouldIdentifyExpiredReservationWhenTimePassedAndStatusIsReserved() {
        // ARRANGE
        StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .quantity(5)
                .status(StockReservation.ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(10)) // 10 minutes ago
                .build();
        
        // ACT
        boolean expired = reservation.isExpired();
        
        // ASSERT
        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("Should not identify as expired when time passed but status is CONFIRMED")
    void shouldNotIdentifyAsExpiredWhenTimePassedButStatusIsConfirmed() {
        // ARRANGE
        StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .quantity(5)
                .status(StockReservation.ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();
        
        // ACT & ASSERT
        assertThat(reservation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should not identify as expired when time passed but status is CANCELLED")
    void shouldNotIdentifyAsExpiredWhenTimePassedButStatusIsCancelled() {
        // ARRANGE
        StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .quantity(5)
                .status(StockReservation.ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();
        
        // ACT & ASSERT
        assertThat(reservation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should not identify as expired when expiry is in future")
    void shouldNotIdentifyAsExpiredWhenExpiryIsInFuture() {
        // ARRANGE
        StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .quantity(5)
                .status(StockReservation.ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // 10 minutes from now
                .build();
        
        // ACT & ASSERT
        assertThat(reservation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should initialize with RESERVED status by default")
    void shouldInitializeWithReservedStatusByDefault() {
        // ACT
        StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .quantity(5)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        
        // ASSERT
        assertThat(reservation.getStatus())
                .isEqualTo(StockReservation.ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("Should support all reservation status values")
    void shouldSupportAllReservationStatusValues() {
        // ASSERT - Testing enum values exist
        assertThat(StockReservation.ReservationStatus.values())
                .containsExactlyInAnyOrder(
                        StockReservation.ReservationStatus.RESERVED,
                        StockReservation.ReservationStatus.CONFIRMED,
                        StockReservation.ReservationStatus.CANCELLED,
                        StockReservation.ReservationStatus.EXPIRED
                );
    }

    @Test
    @DisplayName("Should support all args constructor")
    void shouldSupportAllArgsConstructor() {
        // ARRANGE
        Inventory inventory = new Inventory();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);
        
        // ACT
        StockReservation reservation = new StockReservation(
                1L,
                inventory,
                100L,
                10,
                StockReservation.ReservationStatus.RESERVED,
                expiry,
                null,
                null
        );
        
        // ASSERT
        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getInventory()).isEqualTo(inventory);
        assertThat(reservation.getOrderId()).isEqualTo(100L);
        assertThat(reservation.getQuantity()).isEqualTo(10);
        assertThat(reservation.getStatus())
                .isEqualTo(StockReservation.ReservationStatus.RESERVED);
        assertThat(reservation.getExpiresAt()).isEqualTo(expiry);
    }
}
