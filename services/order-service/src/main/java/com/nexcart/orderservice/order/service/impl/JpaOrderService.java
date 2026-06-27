package com.nexcart.orderservice.order.service.impl;

import com.nexcart.orderservice.exception.InvalidOrderStateException;
import com.nexcart.orderservice.exception.ResourceNotFoundException;
import com.nexcart.orderservice.order.dto.request.CreateOrderRequest;
import com.nexcart.orderservice.order.dto.request.OrderItemRequest;
import com.nexcart.orderservice.order.dto.request.UpdateOrderStatusRequest;
import com.nexcart.orderservice.order.dto.response.OrderItemResponse;
import com.nexcart.orderservice.order.dto.response.OrderResponse;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderItem;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.repository.OrderItemRepository;
import com.nexcart.orderservice.order.repository.OrderRepository;
import com.nexcart.orderservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaOrderService implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final Random random = new Random();
    
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.debug("Creating order for user: {}", request.userId());
        
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(request.userId())
                .shippingAddress(request.shippingAddress())
                .paymentMethod(request.paymentMethod())
                .notes(request.notes())
                .status(OrderStatus.PENDING)
                .build();
        
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .productName(itemRequest.productName())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build();
            
            item.calculateSubtotal();
            order.addItem(item);
        }
        
        order.calculateTotalAmount();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", savedOrder.getOrderNumber());
        
        return mapToResponse(savedOrder);
    }
    
    @Override
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order by ID: {}", id);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return mapToResponse(order);
    }
    
    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.debug("Fetching order by number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return mapToResponse(order);
    }
    
    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToResponseWithoutItems)
                .toList();
    }
    
    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders by status: {}", status);
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream()
                .map(this::mapToResponseWithoutItems)
                .toList();
    }
    
    @Override
    public List<OrderItemResponse> getOrderItems(Long orderId) {
        log.debug("Fetching order items for order: {}", orderId);
        
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }
        
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return items.stream()
                .map(this::mapToItemResponse)
                .toList();
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.debug("Updating order {} status to {}", id, request.status());
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        
        validateStatusTransition(order.getStatus(), request.status());
        
        order.setStatus(request.status());
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated to {}", id, request.status());
        return mapToResponseWithoutItems(updatedOrder);
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.debug("Cancelling order: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order with status: " + order.getStatus()
            );
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("Order {} cancelled successfully", id);
    }
    
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Cannot update status of a cancelled order");
        }
        
        if (currentStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Delivered orders can only be cancelled");
        }
    }
    
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        return "ORD-" + datePart + "-" + randomPart;
    }
    
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapToItemResponse)
                .toList();
        
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getNotes(),
                order.getItems().size(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
    
    private OrderResponse mapToResponseWithoutItems(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getNotes(),
                0,
                List.of(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
    
    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getCreatedAt()
        );
    }
}
