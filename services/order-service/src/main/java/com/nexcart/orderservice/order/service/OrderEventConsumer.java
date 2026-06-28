package com.nexcart.orderservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.exception.ResourceNotFoundException;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.event.InventoryReservationFailedEvent;
import com.nexcart.orderservice.order.event.InventoryReservedEvent;
import com.nexcart.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    @Transactional
    public void handleInventoryEvent(String message) {
        try {
            log.info("Received message from inventory-events: {}", message);
            
            Object event = objectMapper.readValue(message, Object.class);
            String eventType = objectMapper.convertValue(event, 
                    objectMapper.constructType(Object.class))
                    .toString();
            
            if (message.contains("\"eventType\":\"InventoryReserved\"")) {
                InventoryReservedEvent reservedEvent = objectMapper.readValue(message, 
                        InventoryReservedEvent.class);
                handleInventoryReserved(reservedEvent);
            } else if (message.contains("\"eventType\":\"InventoryReservationFailed\"")) {
                InventoryReservationFailedEvent failedEvent = objectMapper.readValue(message, 
                        InventoryReservationFailedEvent.class);
                handleInventoryReservationFailed(failedEvent);
            } else {
                log.debug("Ignoring unknown event type in message");
            }
            
        } catch (Exception e) {
            log.error("Failed to process inventory event", e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
    
    private void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Processing InventoryReservedEvent: orderId={}, eventId={}", 
                event.getOrderId(), event.getEventId());
        
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + event.getOrderId()));
        
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} confirmed: inventory reserved successfully", order.getId());
        } else {
            log.warn("Order {} is in status {} - cannot confirm", order.getId(), order.getStatus());
        }
    }
    
    private void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.info("Processing InventoryReservationFailedEvent: orderId={}, eventId={}, reason={}", 
                event.getOrderId(), event.getEventId(), event.getReason());
        
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + event.getOrderId()));
        
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order {} cancelled: inventory reservation failed - {}", 
                    order.getId(), event.getReason());
        } else {
            log.warn("Order {} is in status {} - cannot cancel", order.getId(), order.getStatus());
        }
    }
}
