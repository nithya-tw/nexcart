package com.nexcart.orderservice.saga;

import com.nexcart.orderservice.order.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoOpPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean processPayment(Order order) {
        log.info("Processing payment for order: {} amount: {}", order.getId(), order.getTotalAmount());
        return true;
    }
}
