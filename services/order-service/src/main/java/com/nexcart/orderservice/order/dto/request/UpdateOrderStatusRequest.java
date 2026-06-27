package com.nexcart.orderservice.order.dto.request;

import com.nexcart.orderservice.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
