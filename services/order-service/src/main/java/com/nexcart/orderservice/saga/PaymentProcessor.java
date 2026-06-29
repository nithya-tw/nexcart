package com.nexcart.orderservice.saga;

import com.nexcart.orderservice.order.entity.Order;

public interface PaymentProcessor {

    boolean processPayment(Order order);
}
