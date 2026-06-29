package com.nexcart.orderservice.saga;

public interface SagaStep {
    void execute();
    void compensate();
}
