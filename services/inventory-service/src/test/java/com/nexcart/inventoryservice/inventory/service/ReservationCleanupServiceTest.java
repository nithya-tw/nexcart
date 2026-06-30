package com.nexcart.inventoryservice.inventory.service;

import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationCleanupService Tests")
class ReservationCleanupServiceTest {
    
    @Mock
    private StockReservationRepository stockReservationRepository;
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @InjectMocks
    private ReservationCleanupService reservationCleanupService;
    
    private Inventory testInventory;
    private StockReservation expiredReservation;
    
    @BeforeEach
    void setUp() {
        testInventory = Inventory.builder()
            .id(1L)
            .productId(100L)
            .quantity(50)
            .availableQuantity(30)
            .reservedQuantity(20)
            .build();
        
        expiredReservation = StockReservation.builder()
            .id(1L)
            .inventory(testInventory)
            .orderId(999L)
            .quantity(5)
            .status(StockReservation.ReservationStatus.RESERVED)
            .expiresAt(LocalDateTime.now().minusMinutes(10))
            .build();
    }
    
    @Test
    @DisplayName("Should cleanup expired reservations and release stock")
    void shouldCleanupExpiredReservations() {
        when(stockReservationRepository.findByStatusAndExpiresAtBefore(
            eq(StockReservation.ReservationStatus.RESERVED), 
            any(LocalDateTime.class)))
            .thenReturn(List.of(expiredReservation));
        
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(stockReservationRepository.save(any(StockReservation.class)))
            .thenReturn(expiredReservation);
        
        reservationCleanupService.cleanupExpiredReservations();
        
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getReservedQuantity()).isEqualTo(15);
        
        ArgumentCaptor<StockReservation> reservationCaptor = 
            ArgumentCaptor.forClass(StockReservation.class);
        verify(stockReservationRepository).save(reservationCaptor.capture());
        
        StockReservation savedReservation = reservationCaptor.getValue();
        assertThat(savedReservation.getStatus())
            .isEqualTo(StockReservation.ReservationStatus.EXPIRED);
    }
    
    @Test
    @DisplayName("Should handle multiple expired reservations")
    void shouldHandleMultipleExpiredReservations() {
        Inventory inventory2 = Inventory.builder()
            .id(2L)
            .productId(200L)
            .quantity(100)
            .availableQuantity(70)
            .reservedQuantity(30)
            .build();
        
        StockReservation reservation2 = StockReservation.builder()
            .id(2L)
            .inventory(inventory2)
            .orderId(998L)
            .quantity(10)
            .status(StockReservation.ReservationStatus.RESERVED)
            .expiresAt(LocalDateTime.now().minusMinutes(5))
            .build();
        
        when(stockReservationRepository.findByStatusAndExpiresAtBefore(
            eq(StockReservation.ReservationStatus.RESERVED), 
            any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(expiredReservation, reservation2));
        
        when(inventoryRepository.save(any(Inventory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockReservationRepository.save(any(StockReservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        reservationCleanupService.cleanupExpiredReservations();
        
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
        verify(stockReservationRepository, times(2)).save(any(StockReservation.class));
        
        assertThat(testInventory.getReservedQuantity()).isEqualTo(15);
        assertThat(inventory2.getReservedQuantity()).isEqualTo(20);
        assertThat(expiredReservation.getStatus())
            .isEqualTo(StockReservation.ReservationStatus.EXPIRED);
        assertThat(reservation2.getStatus())
            .isEqualTo(StockReservation.ReservationStatus.EXPIRED);
    }
    
    @Test
    @DisplayName("Should do nothing when no expired reservations found")
    void shouldDoNothingWhenNoExpiredReservations() {
        when(stockReservationRepository.findByStatusAndExpiresAtBefore(
            eq(StockReservation.ReservationStatus.RESERVED), 
            any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        reservationCleanupService.cleanupExpiredReservations();
        
        verify(inventoryRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should continue cleanup on individual failure")
    void shouldContinueCleanupOnIndividualFailure() {
        Inventory inventory2 = Inventory.builder()
            .id(2L)
            .productId(200L)
            .quantity(100)
            .reservedQuantity(30)
            .build();
        
        StockReservation reservation2 = StockReservation.builder()
            .id(2L)
            .inventory(inventory2)
            .orderId(998L)
            .quantity(10)
            .status(StockReservation.ReservationStatus.RESERVED)
            .expiresAt(LocalDateTime.now().minusMinutes(5))
            .build();
        
        when(stockReservationRepository.findByStatusAndExpiresAtBefore(
            eq(StockReservation.ReservationStatus.RESERVED), 
            any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(expiredReservation, reservation2));
        
        when(inventoryRepository.save(testInventory))
            .thenThrow(new RuntimeException("Database error"));
        when(inventoryRepository.save(inventory2)).thenReturn(inventory2);
        when(stockReservationRepository.save(reservation2)).thenReturn(reservation2);
        
        reservationCleanupService.cleanupExpiredReservations();
        
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
        verify(stockReservationRepository, times(1)).save(reservation2);
        
        assertThat(inventory2.getReservedQuantity()).isEqualTo(20);
        assertThat(reservation2.getStatus())
            .isEqualTo(StockReservation.ReservationStatus.EXPIRED);
    }
    
    @Test
    @DisplayName("Should correctly calculate reserved quantity after cleanup")
    void shouldCorrectlyCalculateReservedQuantity() {
        testInventory.setReservedQuantity(25);
        expiredReservation.setQuantity(8);
        
        when(stockReservationRepository.findByStatusAndExpiresAtBefore(
            eq(StockReservation.ReservationStatus.RESERVED), 
            any(LocalDateTime.class)))
            .thenReturn(List.of(expiredReservation));
        
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(stockReservationRepository.save(any(StockReservation.class)))
            .thenReturn(expiredReservation);
        
        reservationCleanupService.cleanupExpiredReservations();
        
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        
        assertThat(captor.getValue().getReservedQuantity()).isEqualTo(17);
    }
    
    @Test
    @DisplayName("Should only process RESERVED status reservations")
    void shouldOnlyProcessReservedStatusReservations() {
        reservationCleanupService.cleanupExpiredReservations();
        
        ArgumentCaptor<StockReservation.ReservationStatus> statusCaptor = 
            ArgumentCaptor.forClass(StockReservation.ReservationStatus.class);
        
        verify(stockReservationRepository).findByStatusAndExpiresAtBefore(
            statusCaptor.capture(), 
            any(LocalDateTime.class)
        );
        
        assertThat(statusCaptor.getValue())
            .isEqualTo(StockReservation.ReservationStatus.RESERVED);
    }
    
    @Test
    @DisplayName("Should query reservations expiring before current time")
    void shouldQueryReservationsExpiringBeforeCurrentTime() {
        LocalDateTime beforeCleanup = LocalDateTime.now();
        
        reservationCleanupService.cleanupExpiredReservations();
        
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(stockReservationRepository).findByStatusAndExpiresAtBefore(
            any(StockReservation.ReservationStatus.class),
            timeCaptor.capture()
        );
        
        LocalDateTime afterCleanup = LocalDateTime.now();
        LocalDateTime queriedTime = timeCaptor.getValue();
        
        assertThat(queriedTime).isBetween(beforeCleanup, afterCleanup);
    }
}
