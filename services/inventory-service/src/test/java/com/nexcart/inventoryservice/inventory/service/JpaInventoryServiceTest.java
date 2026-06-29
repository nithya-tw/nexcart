package com.nexcart.inventoryservice.inventory.service;

import com.nexcart.inventoryservice.exception.InsufficientStockException;
import com.nexcart.inventoryservice.exception.ResourceNotFoundException;
import com.nexcart.inventoryservice.inventory.dto.request.ReserveStockRequest;
import com.nexcart.inventoryservice.inventory.dto.request.UpdateStockRequest;
import com.nexcart.inventoryservice.inventory.dto.response.InventoryResponse;
import com.nexcart.inventoryservice.inventory.dto.response.ReservationResponse;
import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import com.nexcart.inventoryservice.inventory.service.impl.JpaInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaInventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @InjectMocks
    private JpaInventoryService inventoryService;

    private Inventory inventory;
    private StockReservation reservation;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2025, 1, 10, 12, 0);
        inventory = buildInventory(1L, 1001L, 20, 5, 15, 10);
        reservation = buildReservation(11L, inventory, 5001L, 4,
                StockReservation.ReservationStatus.RESERVED, now.plusMinutes(15));
    }

    @Test
    void getInventoryShouldReturnMappedResponseWhenInventoryExists() {
        // This test teaches the happy path: the service should translate a database entity into an API-safe DTO.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventory(1001L);

        assertThat(response.productId()).isEqualTo(1001L);
        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.reservedQuantity()).isEqualTo(5);
        assertThat(response.availableQuantity()).isEqualTo(15);
        assertThat(response.reorderLevel()).isEqualTo(10);
        assertThat(response.isLowStock()).isFalse();
    }

    @Test
    void getInventoryShouldThrowWhenInventoryDoesNotExist() {
        // When data is missing, the service should fail loudly instead of returning misleading empty data.
        when(inventoryRepository.findByProductId(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventory(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inventory not found for product: 9999");
    }

    @Test
    void createInventoryShouldPersistNewInventoryWhenProductDoesNotExist() {
        // Creating stock is like opening a new shelf in a warehouse: start reserved stock at zero and save it once.
        when(inventoryRepository.existsByProductId(2002L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory savedInventory = invocation.getArgument(0);
            savedInventory.setId(2L);
            savedInventory.setAvailableQuantity(savedInventory.getQuantity() - savedInventory.getReservedQuantity());
            savedInventory.setCreatedAt(now);
            savedInventory.setUpdatedAt(now);
            return savedInventory;
        });

        InventoryResponse response = inventoryService.createInventory(2002L, 30, 8);

        assertThat(response.productId()).isEqualTo(2002L);
        assertThat(response.quantity()).isEqualTo(30);
        assertThat(response.reservedQuantity()).isZero();
        assertThat(response.availableQuantity()).isEqualTo(30);
        assertThat(response.reorderLevel()).isEqualTo(8);
    }

    @Test
    void createInventoryShouldThrowWhenInventoryAlreadyExists() {
        // A unique product should have one inventory record; duplicates would break stock truth.
        when(inventoryRepository.existsByProductId(1001L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(1001L, 10, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Inventory already exists for product: 1001");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void createInventoryShouldUseDefaultReorderLevelWhenValueIsNull() {
        // Default values protect the domain when callers omit optional settings.
        when(inventoryRepository.existsByProductId(3003L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory savedInventory = invocation.getArgument(0);
            savedInventory.setId(3L);
            savedInventory.setAvailableQuantity(savedInventory.getQuantity());
            savedInventory.setCreatedAt(now);
            savedInventory.setUpdatedAt(now);
            return savedInventory;
        });

        InventoryResponse response = inventoryService.createInventory(3003L, 12, null);

        assertThat(response.reorderLevel()).isEqualTo(10);
    }

    @Test
    void updateInventoryShouldChangeQuantityWhenRequestContainsNewQuantity() {
        // Updating quantity should behave like recounting physical units on the shelf.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.updateInventory(1001L, new UpdateStockRequest(25, null));

        assertThat(response.quantity()).isEqualTo(25);
        assertThat(inventory.getQuantity()).isEqualTo(25);
    }

    @Test
    void updateInventoryShouldChangeReorderLevelWhenRequestContainsNewThreshold() {
        // Reorder level is the alarm threshold that tells the business when to buy more stock.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.updateInventory(1001L, new UpdateStockRequest(null, 6));

        assertThat(response.reorderLevel()).isEqualTo(6);
        assertThat(inventory.getReorderLevel()).isEqualTo(6);
        assertThat(inventory.getQuantity()).isEqualTo(20);
    }

    @Test
    void updateInventoryShouldThrowWhenNewQuantityIsLowerThanReservedQuantity() {
        // Reserved units already belong to customer orders, so total stock cannot drop below that promise.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.updateInventory(1001L, new UpdateStockRequest(4, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot set quantity to 4. Reserved quantity is 5");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void updateInventoryShouldThrowWhenInventoryDoesNotExist() {
        // We cannot update a warehouse bin that does not exist in the catalog.
        when(inventoryRepository.findByProductId(7777L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateInventory(7777L, new UpdateStockRequest(5, 2)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inventory not found for product: 7777");
    }

    @Test
    void checkAvailabilityShouldReturnTrueWhenEnoughStockExists() {
        // Availability checks are lightweight read operations used before creating reservations.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));

        Boolean available = inventoryService.checkAvailability(1001L, 10);

        assertThat(available).isTrue();
    }

    @Test
    void checkAvailabilityShouldReturnFalseWhenStockIsInsufficient() {
        // The service should protect against overselling by reporting false when demand exceeds supply.
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));

        Boolean available = inventoryService.checkAvailability(1001L, 99);

        assertThat(available).isFalse();
    }

    @Test
    void checkAvailabilityShouldReturnFalseWhenInventoryDoesNotExist() {
        // Missing inventory is treated as unavailable, which is safer than assuming stock exists.
        when(inventoryRepository.findByProductId(4040L)).thenReturn(Optional.empty());

        Boolean available = inventoryService.checkAvailability(4040L, 1);

        assertThat(available).isFalse();
    }

    @Test
    void reserveStockShouldIncreaseReservedQuantityAndCreateReservationWhenStockIsAvailable() {
        // Reserving stock is like putting items in a hold bin: available stock drops, but quantity is not deducted yet.
        ReserveStockRequest request = new ReserveStockRequest(1001L, 6001L, 3, 20);
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> {
            StockReservation savedReservation = invocation.getArgument(0);
            savedReservation.setId(21L);
            savedReservation.setCreatedAt(now);
            return savedReservation;
        });

        ReservationResponse response = inventoryService.reserveStock(request);

        assertThat(inventory.getReservedQuantity()).isEqualTo(8);
        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.orderId()).isEqualTo(6001L);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.expiresAt()).isAfter(now.plusMinutes(19));
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStockShouldThrowWhenStockIsInsufficient() {
        // If available stock is too low, the reservation must be rejected immediately.
        ReserveStockRequest request = new ReserveStockRequest(1001L, 6002L, 30, 15);
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for product 1001. Available: 15, Requested: 30");

        verify(reservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void reserveStockShouldThrowWhenInventoryDoesNotExist() {
        // No inventory record means there is nothing to reserve from.
        ReserveStockRequest request = new ReserveStockRequest(9999L, 6003L, 2, 15);
        when(inventoryRepository.findByProductId(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inventory not found for product: 9999");
    }

    @Test
    void reserveStockShouldUseDefaultTtlWhenRequestOmitsIt() {
        // TTL is the countdown timer for how long the hold stays valid before stock is released.
        ReserveStockRequest request = new ReserveStockRequest(1001L, 6004L, 2, null);
        when(inventoryRepository.findByProductId(1001L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> {
            StockReservation savedReservation = invocation.getArgument(0);
            savedReservation.setId(22L);
            return savedReservation;
        });

        LocalDateTime beforeCall = LocalDateTime.now();
        ReservationResponse response = inventoryService.reserveStock(request);
        LocalDateTime afterCall = LocalDateTime.now();

        assertThat(request.ttlMinutes()).isEqualTo(15);
        assertThat(response.expiresAt())
                .isAfterOrEqualTo(beforeCall.plusMinutes(15).minusSeconds(1))
                .isBeforeOrEqualTo(afterCall.plusMinutes(15).plusSeconds(1));
    }

    @Test
    void confirmReservationShouldMarkReservationConfirmedAndDeductInventory() {
        // Confirmation finalizes the sale: reserved stock leaves the shelf and becomes purchased stock.
        when(reservationRepository.findByOrderId(5001L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = inventoryService.confirmReservation(5001L);

        assertThat(reservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.CONFIRMED);
        assertThat(inventory.getQuantity()).isEqualTo(16);
        assertThat(inventory.getReservedQuantity()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(reservationRepository).save(reservation);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void confirmReservationShouldThrowWhenReservationStatusIsNotReserved() {
        // A reservation can only move forward from RESERVED; other states are terminal or already handled.
        reservation.setStatus(StockReservation.ReservationStatus.CANCELLED);
        when(reservationRepository.findByOrderId(5001L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> inventoryService.confirmReservation(5001L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot confirm reservation. Current status: CANCELLED");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void confirmReservationShouldThrowWhenReservationDoesNotExist() {
        // Confirming a missing hold should fail so the caller can investigate the bad order flow.
        when(reservationRepository.findByOrderId(9001L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.confirmReservation(9001L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reservation not found for order: 9001");
    }

    @Test
    void cancelReservationShouldReleaseStockAndMarkReservationCancelled() {
        // Cancellation returns held units back to the pool so another shopper can buy them.
        when(reservationRepository.findByOrderId(5001L)).thenReturn(Optional.of(reservation));

        inventoryService.cancelReservation(5001L);

        assertThat(inventory.getReservedQuantity()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.CANCELLED);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservationShouldThrowWhenReservationIsAlreadyConfirmed() {
        // Confirmed reservations represent completed sales, so cancellation must be blocked here.
        reservation.setStatus(StockReservation.ReservationStatus.CONFIRMED);
        when(reservationRepository.findByOrderId(5001L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> inventoryService.cancelReservation(5001L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel confirmed reservation");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void cancelReservationShouldThrowWhenReservationDoesNotExist() {
        // The service should signal missing reservations instead of silently ignoring bad order ids.
        when(reservationRepository.findByOrderId(8888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.cancelReservation(8888L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reservation not found for order: 8888");
    }

    @Test
    void getLowStockInventoriesShouldReturnMappedResponses() {
        // This query acts like a dashboard alert, transforming risky inventory rows into response DTOs.
        Inventory lowStock = buildInventory(2L, 2002L, 6, 2, 4, 5);
        when(inventoryRepository.findLowStockInventories()).thenReturn(List.of(inventory, lowStock));

        List<InventoryResponse> responses = inventoryService.getLowStockInventories();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(InventoryResponse::productId)
                .containsExactly(1001L, 2002L);
        assertThat(responses)
                .extracting(InventoryResponse::isLowStock)
                .containsExactly(false, true);
    }

    @Test
    void processExpiredReservationsShouldReleaseStockAndMarkEachReservationExpired() {
        // Expiration is housekeeping: held stock that timed out must be returned automatically.
        Inventory secondInventory = buildInventory(3L, 3003L, 12, 4, 8, 3);
        StockReservation secondReservation = buildReservation(12L, secondInventory, 7002L, 3,
                StockReservation.ReservationStatus.RESERVED, now.minusMinutes(1));
        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(List.of(reservation, secondReservation));

        inventoryService.processExpiredReservations();

        assertThat(inventory.getReservedQuantity()).isEqualTo(1);
        assertThat(secondInventory.getReservedQuantity()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.EXPIRED);
        assertThat(secondReservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.EXPIRED);
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
        verify(reservationRepository, times(2)).save(any(StockReservation.class));
    }

    private Inventory buildInventory(Long id, Long productId, int quantity, int reservedQuantity,
                                     int availableQuantity, int reorderLevel) {
        Inventory item = Inventory.builder()
                .id(id)
                .productId(productId)
                .quantity(quantity)
                .reservedQuantity(reservedQuantity)
                .reorderLevel(reorderLevel)
                .build();
        item.setAvailableQuantity(availableQuantity);
        item.setCreatedAt(now.minusDays(1));
        item.setUpdatedAt(now);
        return item;
    }

    private StockReservation buildReservation(Long id, Inventory inventory, Long orderId, int quantity,
                                              StockReservation.ReservationStatus status, LocalDateTime expiresAt) {
        StockReservation stockReservation = StockReservation.builder()
                .id(id)
                .inventory(inventory)
                .orderId(orderId)
                .quantity(quantity)
                .status(status)
                .expiresAt(expiresAt)
                .build();
        stockReservation.setCreatedAt(now.minusHours(2));
        stockReservation.setUpdatedAt(now.minusHours(1));
        return stockReservation;
    }
}
