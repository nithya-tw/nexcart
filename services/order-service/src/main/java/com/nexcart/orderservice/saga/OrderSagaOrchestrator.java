package com.nexcart.orderservice.saga;

import com.nexcart.orderservice.inventory.client.InventoryClient;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Transactional
    public void executeOrderSaga(Order order) {
        try {
            log.info("Starting saga for order: {}", order.getId());
            
            // Step 1: Reserve inventory
            boolean inventoryReserved = reserveInventory(order);
            if (!inventoryReserved) {
                compensateOrder(order, "Inventory reservation failed");
                return;
            }
            
            // Step 2: Process payment (placeholder)
            boolean paymentProcessed = processPayment(order);
            if (!paymentProcessed) {
                compensateInventory(order);
                compensateOrder(order, "Payment processing failed");
                return;
            }
            
            // Step 3: Confirm order
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Saga completed successfully for order: {}", order.getId());
            
        } catch (Exception e) {
            log.error("Saga failed for order: {}", order.getId(), e);
            compensateAll(order);
        }
    }

    private boolean reserveInventory(Order order) {
        try {
            for (var item : order.getItems()) {
                boolean reserved = inventoryClient.reserveStock(
                    item.getProductId(), 
                    item.getQuantity(), 
                    order.getId()
                );
                if (!reserved) {
                    log.error("Failed to reserve inventory for product: {} in order: {}", 
                            item.getProductId(), order.getId());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to reserve inventory for order: {}", order.getId(), e);
            return false;
        }
    }

    private boolean processPayment(Order order) {
        // Placeholder: Integrate with payment gateway
        log.info("Processing payment for order: {} amount: {}", order.getId(), order.getTotalAmount());
        return true;
    }

    private void compensateInventory(Order order) {
        log.info("Compensating inventory for order: {}", order.getId());
        order.getItems().forEach(item -> {
            try {
                inventoryClient.releaseStock(
                    item.getProductId(), 
                    item.getQuantity(), 
                    order.getId()
                );
            } catch (Exception e) {
                log.error("Failed to release inventory for product: {} in order: {}", 
                        item.getProductId(), order.getId(), e);
            }
        });
    }

    private void compensateOrder(Order order, String reason) {
        log.info("Compensating order: {} reason: {}", order.getId(), reason);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void compensateAll(Order order) {
        compensateInventory(order);
        compensateOrder(order, "Saga execution failed");
    }
}
