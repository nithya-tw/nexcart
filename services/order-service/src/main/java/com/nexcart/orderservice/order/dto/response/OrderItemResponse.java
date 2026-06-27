package com.nexcart.orderservice.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        LocalDateTime createdAt
) {
}
