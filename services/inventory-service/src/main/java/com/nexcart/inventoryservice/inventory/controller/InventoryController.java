package com.nexcart.inventoryservice.inventory.controller;

import com.nexcart.inventoryservice.inventory.dto.request.ReserveStockRequest;
import com.nexcart.inventoryservice.inventory.dto.request.UpdateStockRequest;
import com.nexcart.inventoryservice.inventory.dto.response.InventoryResponse;
import com.nexcart.inventoryservice.inventory.dto.response.ReservationResponse;
import com.nexcart.inventoryservice.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        log.info("REST request to get inventory for product: {}", productId);
        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> createInventory(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Integer reorderLevel) {
        log.info("REST request to create inventory for product {}: quantity={}", productId, quantity);
        InventoryResponse response = inventoryService.createInventory(productId, quantity, reorderLevel);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockRequest request) {
        log.info("REST request to update inventory for product: {}", productId);
        InventoryResponse response = inventoryService.updateInventory(productId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("REST request to check availability for product {}: quantity={}", productId, quantity);
        Boolean available = inventoryService.checkAvailability(productId, quantity);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "requestedQuantity", quantity,
                "available", available
        ));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        log.info("REST request to reserve stock: {}", request);
        ReservationResponse response = inventoryService.reserveStock(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/reservations/order/{orderId}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long orderId) {
        log.info("REST request to confirm reservation for order: {}", orderId);
        ReservationResponse response = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservations/order/{orderId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long orderId) {
        log.info("REST request to cancel reservation for order: {}", orderId);
        inventoryService.cancelReservation(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventories() {
        log.info("REST request to get low stock inventories");
        List<InventoryResponse> response = inventoryService.getLowStockInventories();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-expired")
    public ResponseEntity<Void> processExpiredReservations() {
        log.info("REST request to process expired reservations");
        inventoryService.processExpiredReservations();
        return ResponseEntity.ok().build();
    }
}
