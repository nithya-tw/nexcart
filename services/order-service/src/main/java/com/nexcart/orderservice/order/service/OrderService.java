package com.nexcart.orderservice.order.service;

import com.nexcart.orderservice.order.dto.request.CreateOrderRequest;
import com.nexcart.orderservice.order.dto.request.UpdateOrderStatusRequest;
import com.nexcart.orderservice.order.dto.response.OrderItemResponse;
import com.nexcart.orderservice.order.dto.response.OrderResponse;
import com.nexcart.orderservice.order.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    
    OrderResponse createOrder(CreateOrderRequest request);
    
    OrderResponse getOrderById(Long id);
    
    OrderResponse getOrderByNumber(String orderNumber);
    
    List<OrderResponse> getAllOrders();
    
    List<OrderResponse> getOrdersByUserId(Long userId);
    
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    
    List<OrderItemResponse> getOrderItems(Long orderId);
    
    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
    
    void cancelOrder(Long id);
}
