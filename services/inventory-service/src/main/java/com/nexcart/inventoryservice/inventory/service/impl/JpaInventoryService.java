package com.nexcart.inventoryservice.inventory.service.impl;

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
import com.nexcart.inventoryservice.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaInventoryService implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        log.info("Getting inventory for product: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse createInventory(Long productId, Integer quantity, Integer reorderLevel) {
        log.info("Creating inventory for product {}: quantity={}, reorderLevel={}", productId, quantity, reorderLevel);

        if (inventoryRepository.existsByProductId(productId)) {
            throw new IllegalStateException("Inventory already exists for product: " + productId);
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .quantity(quantity)
                .reservedQuantity(0)
                .reorderLevel(reorderLevel != null ? reorderLevel : 10)
                .build();

        inventory = inventoryRepository.save(inventory);
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(Long productId, UpdateStockRequest request) {
        log.info("Updating inventory for product {}: {}", productId, request);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        if (request.quantity() != null) {
            if (request.quantity() < inventory.getReservedQuantity()) {
                throw new IllegalStateException(
                        String.format("Cannot set quantity to %d. Reserved quantity is %d",
                                request.quantity(), inventory.getReservedQuantity()));
            }
            inventory.setQuantity(request.quantity());
        }

        if (request.reorderLevel() != null) {
            inventory.setReorderLevel(request.reorderLevel());
        }

        inventory = inventoryRepository.save(inventory);
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkAvailability(Long productId, Integer quantity) {
        log.debug("Checking availability for product {}: quantity={}", productId, quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(null);

        if (inventory == null) {
            return false;
        }

        return inventory.hasAvailableStock(quantity);
    }

    @Override
    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request) {
        log.info("Reserving stock for order {}: product={}, quantity={}", 
                request.orderId(), request.productId(), request.quantity());

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + request.productId()));

        if (!inventory.hasAvailableStock(request.quantity())) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %d. Available: %d, Requested: %d",
                            request.productId(), inventory.getAvailableQuantity(), request.quantity()));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventoryRepository.save(inventory);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.ttlMinutes());
        StockReservation reservation = StockReservation.builder()
                .inventory(inventory)
                .orderId(request.orderId())
                .quantity(request.quantity())
                .status(StockReservation.ReservationStatus.RESERVED)
                .expiresAt(expiresAt)
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("Stock reserved successfully. Reservation ID: {}, Expires at: {}", reservation.getId(), expiresAt);

        return mapToReservationResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse confirmReservation(Long orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        StockReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for order: " + orderId));

        if (reservation.getStatus() != StockReservation.ReservationStatus.RESERVED) {
            throw new IllegalStateException("Cannot confirm reservation. Current status: " + reservation.getStatus());
        }

        reservation.setStatus(StockReservation.ReservationStatus.CONFIRMED);
        Inventory inventory = reservation.getInventory();
        inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());

        reservationRepository.save(reservation);
        inventoryRepository.save(inventory);

        log.info("Reservation confirmed. Stock deducted from inventory.");
        return mapToReservationResponse(reservation);
    }

    @Override
    @Transactional
    public void cancelReservation(Long orderId) {
        log.info("Cancelling reservation for order: {}", orderId);

        StockReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for order: " + orderId));

        if (reservation.getStatus() == StockReservation.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed reservation");
        }

        Inventory inventory = reservation.getInventory();
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);

        reservation.setStatus(StockReservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation cancelled. Stock released back to inventory.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventories() {
        log.info("Fetching low stock inventories");
        return inventoryRepository.findLowStockInventories().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processExpiredReservations() {
        log.info("Processing expired reservations");
        List<StockReservation> expiredReservations = reservationRepository.findExpiredReservations(LocalDateTime.now());

        for (StockReservation reservation : expiredReservations) {
            Inventory inventory = reservation.getInventory();
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(StockReservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            log.info("Expired reservation {} for order {}. Stock released.", reservation.getId(), reservation.getOrderId());
        }

        log.info("Processed {} expired reservations", expiredReservations.size());
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reorderLevel(inventory.getReorderLevel())
                .isLowStock(inventory.isLowStock())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private ReservationResponse mapToReservationResponse(StockReservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .inventoryId(reservation.getInventory().getId())
                .orderId(reservation.getOrderId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus().name())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
