package com.nexcart.cartservice.cart.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CartResponse(
        Long id,
        Long userId,
        String status,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        Integer totalItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
