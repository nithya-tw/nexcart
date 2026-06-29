package com.nexcart.cartservice.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.cartservice.cart.dto.request.AddItemRequest;
import com.nexcart.cartservice.cart.dto.response.CartItemResponse;
import com.nexcart.cartservice.cart.dto.response.CartResponse;
import com.nexcart.cartservice.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CartControllerTest - Unit tests for CartController
 *
 * This test class provides comprehensive coverage for the CartController endpoints.
 * It uses @WebMvcTest to test only the controller layer, mocking the CartService layer.
 *
 * Test Coverage:
 * - GET /api/v1/carts/user/{userId} - Get or create cart for user
 * - POST /api/v1/carts/user/{userId}/items - Add item to cart
 * - PUT /api/v1/carts/user/{userId}/items/{productId} - Update item quantity
 * - DELETE /api/v1/carts/user/{userId}/items/{productId} - Remove item from cart
 * - DELETE /api/v1/carts/user/{userId} - Clear entire cart
 */
@WebMvcTest(CartController.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    private CartResponse cartResponse;
    private CartItemResponse cartItemResponse;
    private AddItemRequest addItemRequest;

    /**
     * Setup test data before each test case
     * This method prepares common test fixtures for cart-related tests
     */
    @BeforeEach
    void setUp() {
        // Arrange: Create a sample cart item response
        cartItemResponse = CartItemResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(1)
                .price(new BigDecimal("999.99"))
                .subtotal(new BigDecimal("999.99"))
                .build();

        // Arrange: Create a sample cart response with items
        cartResponse = CartResponse.builder()
                .id(1L)
                .userId(1L)
                .status("ACTIVE")
                .items(new ArrayList<>(Arrays.asList(cartItemResponse)))
                .totalAmount(new BigDecimal("999.99"))
                .totalItems(1)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        // Arrange: Create a sample add item request
        addItemRequest = AddItemRequest.builder()
                .productId(1L)
                .quantity(1)
                .price(new BigDecimal("999.99"))
                .build();
    }

    /**
     * Test Case 1: GET /api/v1/carts/user/{userId} - Get Cart Successfully
     *
     * Scenario: Client requests the cart for a specific user
     * Expected: Should return HTTP 200 (OK) with the cart details
     */
    @Test
    @DisplayName("Should retrieve cart for user successfully and return HTTP 200")
    void testGetCart_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        when(cartService.getOrCreateCart(userId))
                .thenReturn(cartResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/carts/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.totalAmount", is(999.99)))
                .andExpect(jsonPath("$.totalItems", is(1)));

        // Verify
        verify(cartService, times(1)).getOrCreateCart(userId);
    }

    /**
     * Test Case 2: GET /api/v1/carts/user/{userId} - Create Cart When Not Exists
     *
     * Scenario: Client requests cart for a user who doesn't have an existing cart
     * Expected: Should create and return a new empty cart with HTTP 200 (OK)
     */
    @Test
    @DisplayName("Should create and return new cart when it does not exist")
    void testGetCart_CreateNew() throws Exception {
        // Arrange
        Long userId = 99L;
        CartResponse newCartResponse = CartResponse.builder()
                .id(99L)
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("0.00"))
                .totalItems(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.getOrCreateCart(userId))
                .thenReturn(newCartResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/carts/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems", is(0)));

        // Verify
        verify(cartService, times(1)).getOrCreateCart(userId);
    }

    /**
     * Test Case 3: POST /api/v1/carts/user/{userId}/items - Add Item Successfully
     *
     * Scenario: Client adds a product to the cart
     * Expected: Should return HTTP 200 (OK) with updated cart containing the new item
     */
    @Test
    @DisplayName("Should add item to cart successfully and return HTTP 200")
    void testAddItem_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        CartResponse updatedCart = CartResponse.builder()
                .id(1L)
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>(Arrays.asList(cartItemResponse)))
                .totalAmount(new BigDecimal("999.99"))
                .totalItems(1)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.addItem(eq(userId), any(AddItemRequest.class)))
                .thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(post("/api/v1/carts/user/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId", is(1)))
                .andExpect(jsonPath("$.totalAmount", is(999.99)))
                .andExpect(jsonPath("$.totalItems", is(1)));

        // Verify that addItem was called with correct parameters
        verify(cartService, times(1)).addItem(eq(userId), any(AddItemRequest.class));
    }

    /**
     * Test Case 4: POST /api/v1/carts/user/{userId}/items - Add Item With Invalid Request
     *
     * Scenario: Client sends an invalid AddItemRequest (missing required fields)
     * Expected: Should return HTTP 400 (Bad Request) due to validation error
     */
    @Test
    @DisplayName("Should return HTTP 400 when add item request is invalid")
    void testAddItem_ValidationError() throws Exception {
        // Arrange
        Long userId = 1L;
        // Create invalid request with missing required fields
        String invalidRequest = "{ \"quantity\": 1 }"; // Missing productId and price

        // Act & Assert
        mockMvc.perform(post("/api/v1/carts/user/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify that addItem was not called
        verify(cartService, never()).addItem(anyLong(), any(AddItemRequest.class));
    }

    /**
     * Test Case 5: PUT /api/v1/carts/user/{userId}/items/{productId} - Update Item Quantity Successfully
     *
     * Scenario: Client updates the quantity of an item in the cart
     * Expected: Should return HTTP 200 (OK) with updated cart
     */
    @Test
    @DisplayName("Should update item quantity successfully and return HTTP 200")
    void testUpdateItemQuantity_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        Long productId = 1L;
        Integer newQuantity = 3;

        CartItemResponse updatedItem = CartItemResponse.builder()
                .id(1L)
                .productId(productId)
                .quantity(newQuantity)
                .price(new BigDecimal("999.99"))
                .subtotal(new BigDecimal("2999.97")) // 999.99 * 3
                .build();

        CartResponse updatedCart = CartResponse.builder()
                .id(1L)
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>(Arrays.asList(updatedItem)))
                .totalAmount(new BigDecimal("2999.97"))
                .totalItems(3)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.updateItemQuantity(userId, productId, newQuantity))
                .thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(put("/api/v1/carts/user/{userId}/items/{productId}", userId, productId)
                        .param("quantity", String.valueOf(newQuantity))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.items[0].quantity", is(newQuantity)))
                .andExpect(jsonPath("$.totalAmount", is(2999.97)))
                .andExpect(jsonPath("$.totalItems", is(newQuantity)));

        // Verify
        verify(cartService, times(1)).updateItemQuantity(userId, productId, newQuantity);
    }

    /**
     * Test Case 6: DELETE /api/v1/carts/user/{userId}/items/{productId} - Remove Item Successfully
     *
     * Scenario: Client removes an item from the cart
     * Expected: Should return HTTP 200 (OK) with updated cart (without the removed item)
     */
    @Test
    @DisplayName("Should remove item from cart successfully and return HTTP 200")
    void testRemoveItem_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        Long productId = 1L;

        CartResponse emptyCart = CartResponse.builder()
                .id(1L)
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("0.00"))
                .totalItems(0)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.removeItem(userId, productId))
                .thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts/user/{userId}/items/{productId}", userId, productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0.00)))
                .andExpect(jsonPath("$.totalItems", is(0)));

        // Verify
        verify(cartService, times(1)).removeItem(userId, productId);
    }

    /**
     * Test Case 7: DELETE /api/v1/carts/user/{userId}/items/{productId} - Remove Item From Multiple Items
     *
     * Scenario: Client removes one item from a cart containing multiple items
     * Expected: Should return HTTP 200 (OK) with cart containing remaining items
     */
    @Test
    @DisplayName("Should remove one item from cart with multiple items")
    void testRemoveItem_MultipleItems() throws Exception {
        // Arrange
        Long userId = 1L;
        Long productId = 1L;

        CartItemResponse remainingItem = CartItemResponse.builder()
                .id(2L)
                .productId(2L)
                .quantity(1)
                .price(new BigDecimal("29.99"))
                .subtotal(new BigDecimal("29.99"))
                .build();

        CartResponse cartWithRemainingItems = CartResponse.builder()
                .id(1L)
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>(Arrays.asList(remainingItem)))
                .totalAmount(new BigDecimal("29.99"))
                .totalItems(1)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.removeItem(userId, productId))
                .thenReturn(cartWithRemainingItems);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts/user/{userId}/items/{productId}", userId, productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId", is(2)))
                .andExpect(jsonPath("$.totalItems", is(1)));

        // Verify
        verify(cartService, times(1)).removeItem(userId, productId);
    }

    /**
     * Test Case 8: DELETE /api/v1/carts/user/{userId} - Clear Cart Successfully
     *
     * Scenario: Client clears the entire cart (removes all items)
     * Expected: Should return HTTP 204 (No Content) with no response body
     */
    @Test
    @DisplayName("Should clear cart successfully and return HTTP 204")
    void testClearCart_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        doNothing().when(cartService).clearCart(userId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify that clearCart was called with correct user ID
        verify(cartService, times(1)).clearCart(userId);
    }
}
