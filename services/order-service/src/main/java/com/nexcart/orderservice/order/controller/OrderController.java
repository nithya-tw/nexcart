package com.nexcart.orderservice.order.controller;

import com.nexcart.orderservice.order.dto.request.CreateOrderRequest;
import com.nexcart.orderservice.order.dto.request.UpdateOrderStatusRequest;
import com.nexcart.orderservice.order.dto.response.OrderItemResponse;
import com.nexcart.orderservice.order.dto.response.OrderResponse;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request for user: {}", request.userId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("Fetching order by ID: {}", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Fetching order by number: {}", orderNumber);
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("Fetching orders for user: {}", userId);
        List<OrderResponse> responses = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        log.info("Fetching orders by status: {}", status);
        List<OrderResponse> responses = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(@PathVariable Long id) {
        log.info("Fetching items for order: {}", id);
        List<OrderItemResponse> responses = orderService.getOrderItems(id);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", id, request.status());
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.info("Cancelling order: {}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
