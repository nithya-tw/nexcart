package com.nexcart.orderservice.order.dto.response;

import com.nexcart.orderservice.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        OrderStatus status,
        String shippingAddress,
        String paymentMethod,
        String notes,
        Integer itemCount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
