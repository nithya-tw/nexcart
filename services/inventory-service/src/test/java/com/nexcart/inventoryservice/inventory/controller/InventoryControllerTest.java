package com.nexcart.inventoryservice.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.inventoryservice.inventory.dto.request.ReserveStockRequest;
import com.nexcart.inventoryservice.inventory.dto.request.UpdateStockRequest;
import com.nexcart.inventoryservice.inventory.dto.response.InventoryResponse;
import com.nexcart.inventoryservice.inventory.dto.response.ReservationResponse;
import com.nexcart.inventoryservice.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InventoryControllerTest - Unit tests for InventoryController
 *
 * This test class provides comprehensive coverage for the InventoryController endpoints.
 * It uses @WebMvcTest to test only the controller layer, mocking the InventoryService layer.
 *
 * Test Coverage:
 * - GET /api/v1/inventory/product/{productId} - Get inventory for a product
 * - POST /api/v1/inventory/product/{productId} - Create inventory for a product
 * - PUT /api/v1/inventory/product/{productId} - Update inventory/stock
 * - GET /api/v1/inventory/product/{productId}/availability - Check product availability
 * - POST /api/v1/inventory/reservations - Reserve stock
 * - POST /api/v1/inventory/reservations/order/{orderId}/confirm - Confirm reservation
 * - DELETE /api/v1/inventory/reservations/order/{orderId} - Cancel reservation
 * - GET /api/v1/inventory/low-stock - Get low stock items
 * - POST /api/v1/inventory/process-expired - Process expired reservations
 */
@WebMvcTest(InventoryController.class)
@DisplayName("InventoryController Tests")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryResponse inventoryResponse;
    private ReservationResponse reservationResponse;

    /**
     * Setup test data before each test case
     * This method prepares common test fixtures for inventory-related tests
     */
    @BeforeEach
    void setUp() {
        // Arrange: Create a sample inventory response
        inventoryResponse = InventoryResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(10)
                .availableQuantity(90)
                .reorderLevel(20)
                .isLowStock(false)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

        // Arrange: Create a sample reservation response
        reservationResponse = ReservationResponse.builder()
                .id(1L)
                .inventoryId(1L)
                .orderId(1L)
                .quantity(5)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test Case 1: GET /api/v1/inventory/product/{productId} - Get Inventory Successfully
     *
     * Scenario: Client requests inventory details for a specific product
     * Expected: Should return HTTP 200 (OK) with inventory details
     */
    @Test
    @DisplayName("Should retrieve inventory for product successfully and return HTTP 200")
    void testGetInventory_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        when(inventoryService.getInventory(productId))
                .thenReturn(inventoryResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/product/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(100)))
                .andExpect(jsonPath("$.reservedQuantity", is(10)))
                .andExpect(jsonPath("$.availableQuantity", is(90)))
                .andExpect(jsonPath("$.isLowStock", is(false)));

        // Verify
        verify(inventoryService, times(1)).getInventory(productId);
    }

    /**
     * Test Case 2: POST /api/v1/inventory/product/{productId} - Create Inventory Successfully
     *
     * Scenario: Client creates initial inventory for a new product
     * Expected: Should return HTTP 201 (Created) with the created inventory details
     */
    @Test
    @DisplayName("Should create inventory successfully and return HTTP 201")
    void testCreateInventory_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        Integer quantity = 100;
        Integer reorderLevel = 20;

        when(inventoryService.createInventory(productId, quantity, reorderLevel))
                .thenReturn(inventoryResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/product/{productId}", productId)
                        .param("quantity", String.valueOf(quantity))
                        .param("reorderLevel", String.valueOf(reorderLevel))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(100)))
                .andExpect(jsonPath("$.reorderLevel", is(20)));

        // Verify
        verify(inventoryService, times(1)).createInventory(productId, quantity, reorderLevel);
    }

    /**
     * Test Case 3: POST /api/v1/inventory/product/{productId} - Create Inventory Without Optional Parameters
     *
     * Scenario: Client creates inventory without specifying reorderLevel
     * Expected: Should return HTTP 201 (Created) with default reorderLevel
     */
    @Test
    @DisplayName("Should create inventory without optional reorderLevel parameter")
    void testCreateInventory_WithoutReorderLevel() throws Exception {
        // Arrange
        Long productId = 1L;
        Integer quantity = 50;

        InventoryResponse responseWithoutReorderLevel = InventoryResponse.builder()
                .id(1L)
                .productId(productId)
                .quantity(50)
                .reservedQuantity(0)
                .availableQuantity(50)
                .reorderLevel(0)
                .isLowStock(false)
                .build();

        when(inventoryService.createInventory(productId, quantity, null))
                .thenReturn(responseWithoutReorderLevel);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/product/{productId}", productId)
                        .param("quantity", String.valueOf(quantity))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity", is(50)));

        // Verify
        verify(inventoryService, times(1)).createInventory(productId, quantity, null);
    }

    /**
     * Test Case 4: PUT /api/v1/inventory/product/{productId} - Update Inventory Successfully
     *
     * Scenario: Client updates the stock quantity for an existing product
     * Expected: Should return HTTP 200 (OK) with updated inventory details
     */
    @Test
    @DisplayName("Should update inventory successfully and return HTTP 200")
    void testUpdateInventory_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        UpdateStockRequest updateRequest = new UpdateStockRequest(150, 30);

        InventoryResponse updatedResponse = InventoryResponse.builder()
                .id(1L)
                .productId(productId)
                .quantity(150)
                .reservedQuantity(10)
                .availableQuantity(140)
                .reorderLevel(30)
                .isLowStock(false)
                .build();

        when(inventoryService.updateInventory(productId, updateRequest))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/product/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(150)))
                .andExpect(jsonPath("$.reorderLevel", is(30)))
                .andExpect(jsonPath("$.availableQuantity", is(140)));

        // Verify
        verify(inventoryService, times(1)).updateInventory(eq(productId), any(UpdateStockRequest.class));
    }

    /**
     * Test Case 5: GET /api/v1/inventory/product/{productId}/availability - Check Availability (Available)
     *
     * Scenario: Client checks if a product is available in requested quantity
     * Expected: Should return HTTP 200 (OK) with availability status as true
     */
    @Test
    @DisplayName("Should check availability successfully when stock is available")
    void testCheckAvailability_Available() throws Exception {
        // Arrange
        Long productId = 1L;
        Integer requestedQuantity = 50;

        when(inventoryService.checkAvailability(productId, requestedQuantity))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/product/{productId}/availability", productId)
                        .param("quantity", String.valueOf(requestedQuantity))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.requestedQuantity", is(requestedQuantity)))
                .andExpect(jsonPath("$.available", is(true)));

        // Verify
        verify(inventoryService, times(1)).checkAvailability(productId, requestedQuantity);
    }

    /**
     * Test Case 6: GET /api/v1/inventory/product/{productId}/availability - Check Availability (Not Available)
     *
     * Scenario: Client checks availability when requested quantity exceeds available stock
     * Expected: Should return HTTP 200 (OK) with availability status as false
     */
    @Test
    @DisplayName("Should check availability and return false when stock is insufficient")
    void testCheckAvailability_NotAvailable() throws Exception {
        // Arrange
        Long productId = 1L;
        Integer requestedQuantity = 150; // More than available (90)

        when(inventoryService.checkAvailability(productId, requestedQuantity))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/product/{productId}/availability", productId)
                        .param("quantity", String.valueOf(requestedQuantity))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(false)));

        // Verify
        verify(inventoryService, times(1)).checkAvailability(productId, requestedQuantity);
    }

    /**
     * Test Case 7: POST /api/v1/inventory/reservations - Reserve Stock Successfully
     *
     * Scenario: Client reserves stock for an order
     * Expected: Should return HTTP 201 (Created) with reservation details
     */
    @Test
    @DisplayName("Should reserve stock successfully and return HTTP 201")
    void testReserveStock_Success() throws Exception {
        // Arrange
        ReserveStockRequest reserveRequest = new ReserveStockRequest(1L, 100L, 5, 15);

        when(inventoryService.reserveStock(any(ReserveStockRequest.class)))
                .thenReturn(reservationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.orderId", is(1)))
                .andExpect(jsonPath("$.quantity", is(5)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Verify
        verify(inventoryService, times(1)).reserveStock(any(ReserveStockRequest.class));
    }

    /**
     * Test Case 8: POST /api/v1/inventory/reservations - Reserve Stock With Invalid Request
     *
     * Scenario: Client sends invalid ReserveStockRequest (missing required fields)
     * Expected: Should return HTTP 400 (Bad Request) due to validation error
     */
    @Test
    @DisplayName("Should return HTTP 400 when reserve stock request is invalid")
    void testReserveStock_ValidationError() throws Exception {
        // Arrange
        String invalidRequest = "{ \"quantity\": 5 }"; // Missing productId, orderId, and ttlMinutes

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify that reserveStock was not called
        verify(inventoryService, never()).reserveStock(any(ReserveStockRequest.class));
    }

    /**
     * Test Case 9: POST /api/v1/inventory/reservations/order/{orderId}/confirm - Confirm Reservation Successfully
     *
     * Scenario: Client confirms a reservation (marks it as permanent)
     * Expected: Should return HTTP 200 (OK) with confirmed reservation details
     */
    @Test
    @DisplayName("Should confirm reservation successfully and return HTTP 200")
    void testConfirmReservation_Success() throws Exception {
        // Arrange
        Long orderId = 100L;

        ReservationResponse confirmedResponse = ReservationResponse.builder()
                .id(1L)
                .inventoryId(1L)
                .orderId(orderId)
                .quantity(5)
                .status("CONFIRMED")
                .expiresAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(inventoryService.confirmReservation(orderId))
                .thenReturn(confirmedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/reservations/order/{orderId}/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(100)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Verify
        verify(inventoryService, times(1)).confirmReservation(orderId);
    }

    /**
     * Test Case 10: DELETE /api/v1/inventory/reservations/order/{orderId} - Cancel Reservation Successfully
     *
     * Scenario: Client cancels a reservation (releases reserved stock)
     * Expected: Should return HTTP 204 (No Content) with no response body
     */
    @Test
    @DisplayName("Should cancel reservation successfully and return HTTP 204")
    void testCancelReservation_Success() throws Exception {
        // Arrange
        Long orderId = 100L;
        doNothing().when(inventoryService).cancelReservation(orderId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/inventory/reservations/order/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify
        verify(inventoryService, times(1)).cancelReservation(orderId);
    }

    /**
     * Test Case 11: GET /api/v1/inventory/low-stock - Get Low Stock Inventories Successfully
     *
     * Scenario: Client requests all products with low stock levels
     * Expected: Should return HTTP 200 (OK) with list of low stock items
     */
    @Test
    @DisplayName("Should retrieve low stock inventories successfully")
    void testGetLowStockInventories_Success() throws Exception {
        // Arrange
        InventoryResponse lowStockItem = InventoryResponse.builder()
                .id(2L)
                .productId(2L)
                .quantity(15)
                .reservedQuantity(5)
                .availableQuantity(10)
                .reorderLevel(20)
                .isLowStock(true)
                .build();

        List<InventoryResponse> lowStockItems = new ArrayList<>(Arrays.asList(lowStockItem));
        when(inventoryService.getLowStockInventories()).thenReturn(lowStockItems);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory/low-stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isLowStock", is(true)))
                .andExpect(jsonPath("$[0].quantity", is(15)));

        // Verify
        verify(inventoryService, times(1)).getLowStockInventories();
    }

    /**
     * Test Case 12: POST /api/v1/inventory/process-expired - Process Expired Reservations Successfully
     *
     * Scenario: Client triggers processing of expired reservations
     * Expected: Should return HTTP 200 (OK)
     */
    @Test
    @DisplayName("Should process expired reservations successfully")
    void testProcessExpiredReservations_Success() throws Exception {
        // Arrange
        doNothing().when(inventoryService).processExpiredReservations();

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/process-expired")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify
        verify(inventoryService, times(1)).processExpiredReservations();
    }
}
