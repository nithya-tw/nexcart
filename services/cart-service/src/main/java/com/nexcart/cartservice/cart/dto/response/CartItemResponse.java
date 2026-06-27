package com.nexcart.cartservice.cart.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemResponse(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}
