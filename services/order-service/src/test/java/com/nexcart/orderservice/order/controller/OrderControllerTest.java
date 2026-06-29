package com.nexcart.orderservice.order.controller;

import com.nexcart.orderservice.exception.InvalidOrderStateException;
import com.nexcart.orderservice.exception.ResourceNotFoundException;
import com.nexcart.orderservice.order.dto.response.OrderResponse;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private String createOrderPayload;
    private String updateStatusPayload;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        createOrderPayload = """
                {
                  "userId": 10,
                  "shippingAddress": "221B Baker Street",
                  "paymentMethod": "CARD",
                  "notes": "Leave at reception",
                  "items": [
                    {
                      "productId": 101,
                      "productName": "Keyboard",
                      "quantity": 2,
                      "unitPrice": 49.99
                    }
                  ]
                }
                """;

        updateStatusPayload = """
                {
                  "status": "SHIPPED"
                }
                """;

        orderResponse = new OrderResponse(
                1L,
                "ORD-20240101-0001",
                10L,
                new BigDecimal("99.98"),
                OrderStatus.PENDING,
                "221B Baker Street",
                "CARD",
                "Leave at reception",
                1,
                List.of(),
                LocalDateTime.of(2024, 1, 1, 12, 0),
                LocalDateTime.of(2024, 1, 1, 12, 30)
        );
    }

    @Test
    @DisplayName("POST /api/v1/orders - should create order successfully")
    void shouldCreateOrderSuccessfully() throws Exception {
        when(orderService.createOrder(any())).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20240101-0001"))
                .andExpect(jsonPath("$.totalAmount").value(99.98));
    }

    @Test
    @DisplayName("POST /api/v1/orders - should return validation errors")
    void shouldReturnValidationErrorsWhenCreateRequestIsInvalid() throws Exception {
        String invalidPayload = """
                {
                  "userId": null,
                  "shippingAddress": "",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userId").value("User ID is required"))
                .andExpect(jsonPath("$.shippingAddress").value("Shipping address is required"))
                .andExpect(jsonPath("$.items").value("Order must have at least one item"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - should return order successfully")
    void shouldGetOrderByIdSuccessfully() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(10L));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - should return not found")
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new ResourceNotFoundException("Order not found with ID: 999"));

        mockMvc.perform(get("/api/v1/orders/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/orders - should return all orders")
    void shouldGetAllOrdersSuccessfully() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/orders - should return empty list")
    void shouldGetEmptyOrderList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/orders/user/{userId} - should return user orders")
    void shouldGetOrdersByUserId() throws Exception {
        when(orderService.getOrdersByUserId(10L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/user/{userId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10L));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{id}/status - should update order status")
    void shouldUpdateOrderStatus() throws Exception {
        OrderResponse shippedOrder = new OrderResponse(
                1L,
                "ORD-20240101-0001",
                10L,
                new BigDecimal("99.98"),
                OrderStatus.SHIPPED,
                "221B Baker Street",
                "CARD",
                "Leave at reception",
                1,
                List.of(),
                LocalDateTime.of(2024, 1, 1, 12, 0),
                LocalDateTime.of(2024, 1, 1, 12, 45)
        );
        when(orderService.updateOrderStatus(eq(1L), any())).thenReturn(shippedOrder);

        mockMvc.perform(put("/api/v1/orders/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateStatusPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id}/cancel - should cancel order")
    void shouldCancelOrder() throws Exception {
        doNothing().when(orderService).cancelOrder(1L);

        mockMvc.perform(delete("/api/v1/orders/{id}/cancel", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id}/cancel - should reject invalid state")
    void shouldRejectCancelWhenOrderStateIsInvalid() throws Exception {
        doThrow(new InvalidOrderStateException("Cannot cancel order with status: DELIVERED"))
                .when(orderService).cancelOrder(1L);

        mockMvc.perform(delete("/api/v1/orders/{id}/cancel", 1L))
                .andExpect(status().isBadRequest());
    }
}
