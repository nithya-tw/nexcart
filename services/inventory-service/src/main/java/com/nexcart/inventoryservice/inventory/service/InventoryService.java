package com.nexcart.inventoryservice.inventory.service;

import com.nexcart.inventoryservice.inventory.dto.request.ReserveStockRequest;
import com.nexcart.inventoryservice.inventory.dto.request.UpdateStockRequest;
import com.nexcart.inventoryservice.inventory.dto.response.InventoryResponse;
import com.nexcart.inventoryservice.inventory.dto.response.ReservationResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse getInventory(Long productId);

    InventoryResponse createInventory(Long productId, Integer quantity, Integer reorderLevel);

    InventoryResponse updateInventory(Long productId, UpdateStockRequest request);

    Boolean checkAvailability(Long productId, Integer quantity);

    ReservationResponse reserveStock(ReserveStockRequest request);

    ReservationResponse confirmReservation(Long orderId);

    void cancelReservation(Long orderId);

    List<InventoryResponse> getLowStockInventories();

    void processExpiredReservations();
}
