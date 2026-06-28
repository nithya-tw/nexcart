package com.nexcart.inventoryservice.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.ProcessedEvent;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.event.InventoryReservationFailedEvent;
import com.nexcart.inventoryservice.inventory.event.InventoryReservedEvent;
import com.nexcart.inventoryservice.inventory.event.OrderPlacedEvent;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.ProcessedEventRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    
    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    @Transactional
    public void handleOrderPlacedEvent(String message) {
        try {
            log.info("Received message from order-events: {}", message);
            
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            
            if (!"OrderPlaced".equals(event.getEventType())) {
                log.debug("Ignoring event type: {}", event.getEventType());
                return;
            }
            
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.info("Event already processed (idempotency): eventId={}", event.getEventId());
                return;
            }
            
            log.info("Processing OrderPlacedEvent: orderId={}, eventId={}", 
                    event.getOrderId(), event.getEventId());
            
            boolean success = reserveStockForOrder(event);
            
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .build();
            processedEventRepository.save(processedEvent);
            
            log.info("OrderPlacedEvent processed successfully: orderId={}, success={}", 
                    event.getOrderId(), success);
            
        } catch (Exception e) {
            log.error("Failed to process order event", e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }
    
    private boolean reserveStockForOrder(OrderPlacedEvent event) {
        try {
            List<InventoryReservedEvent.ReservedItemDto> reservedItems = new ArrayList<>();
            
            for (OrderPlacedEvent.OrderItemDto item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElse(null);
                
                if (inventory == null) {
                    String reason = String.format("Product %d not found in inventory", item.getProductId());
                    log.warn("Inventory reservation failed: {}", reason);
                    publishReservationFailedEvent(event.getOrderId(), reason);
                    return false;
                }
                
                if (!inventory.hasAvailableStock(item.getQuantity())) {
                    String reason = String.format(
                            "Insufficient stock for product %d. Available: %d, Requested: %d",
                            item.getProductId(), 
                            inventory.getAvailableQuantity(), 
                            item.getQuantity()
                    );
                    log.warn("Inventory reservation failed: {}", reason);
                    publishReservationFailedEvent(event.getOrderId(), reason);
                    return false;
                }
                
                StockReservation reservation = StockReservation.builder()
                        .inventory(inventory)
                        .orderId(event.getOrderId())
                        .quantity(item.getQuantity())
                        .status(StockReservation.ReservationStatus.RESERVED)
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .build();
                
                stockReservationRepository.save(reservation);
                
                inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
                inventoryRepository.save(inventory);
                
                reservedItems.add(new InventoryReservedEvent.ReservedItemDto(
                        item.getProductId(), 
                        item.getQuantity()
                ));
                
                log.info("Stock reserved: productId={}, quantity={}, reservationId={}", 
                        item.getProductId(), item.getQuantity(), reservation.getId());
            }
            
            publishReservationSuccessEvent(event.getOrderId(), 
                    event.getOrderId(), 
                    reservedItems);
            return true;
            
        } catch (Exception e) {
            log.error("Error during stock reservation for order: {}", event.getOrderId(), e);
            publishReservationFailedEvent(event.getOrderId(), 
                    "Internal error during stock reservation: " + e.getMessage());
            return false;
        }
    }
    
    private void publishReservationSuccessEvent(Long orderId, Long reservationId, 
                                                List<InventoryReservedEvent.ReservedItemDto> items) {
        InventoryReservedEvent event = new InventoryReservedEvent(orderId, reservationId, items);
        
        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published InventoryReservedEvent: orderId={}, eventId={}, offset={}", 
                                orderId, event.getEventId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish InventoryReservedEvent: orderId={}, eventId={}", 
                                orderId, event.getEventId(), ex);
                    }
                });
    }
    
    private void publishReservationFailedEvent(Long orderId, String reason) {
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(orderId, reason);
        
        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published InventoryReservationFailedEvent: orderId={}, eventId={}, offset={}", 
                                orderId, event.getEventId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish InventoryReservationFailedEvent: orderId={}, eventId={}", 
                                orderId, event.getEventId(), ex);
                    }
                });
    }
}
