package com.nexcart.inventoryservice.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.inventoryservice.inventory.entity.FailedEvent;
import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.ProcessedEvent;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.event.InventoryReservationFailedEvent;
import com.nexcart.inventoryservice.inventory.event.InventoryReservedEvent;
import com.nexcart.inventoryservice.inventory.event.OrderPlacedEvent;
import com.nexcart.inventoryservice.inventory.repository.FailedEventRepository;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.ProcessedEventRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final FailedEventRepository failedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    
    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    @Transactional
    public void handleOrderPlacedEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        String eventId = null;
        try {
            log.info("Received message from {}: partition={}, offset={}", topic, partition, offset);
            
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            eventId = event.getEventId();
            
            if (!"OrderPlaced".equals(event.getEventType())) {
                log.debug("Ignoring event type: {}", event.getEventType());
                if (acknowledgment != null) acknowledgment.acknowledge();
                return;
            }
            
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.info("Event already processed (idempotency): eventId={}", event.getEventId());
                if (acknowledgment != null) acknowledgment.acknowledge();
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
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Failed to process order event from topic={}, partition={}, offset={}", 
                    topic, partition, offset, e);
            
            saveFailedEvent(eventId, "OrderPlaced", topic, partition, offset, message, e);
            
            throw new RuntimeException("Failed to process order event", e);
        }
    }
    
    @KafkaListener(topics = "order-events.DLT", groupId = "inventory-service-dlt-group")
    @Transactional
    public void handleDLTMessage(ConsumerRecord<String, String> record) {
        log.error("Received message in DLT: topic={}, partition={}, offset={}, value={}", 
                record.topic(), record.partition(), record.offset(), record.value());
        
        try {
            String eventId = record.key();
            
            FailedEvent existingEvent = failedEventRepository.findByEventId(eventId).orElse(null);
            
            if (existingEvent == null) {
                FailedEvent failedEvent = FailedEvent.builder()
                        .eventId(eventId != null ? eventId : "unknown")
                        .eventType("OrderPlaced")
                        .topic(record.topic())
                        .partition(record.partition())
                        .offset(record.offset())
                        .payload(record.value())
                        .errorMessage("Message sent to DLT after max retries")
                        .stackTrace("See application logs for details")
                        .retryCount(3)
                        .status(FailedEvent.FailureStatus.PERMANENTLY_FAILED)
                        .build();
                
                failedEventRepository.save(failedEvent);
                log.info("Saved DLT message to failed_events table: eventId={}", eventId);
            } else {
                existingEvent.setStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
                failedEventRepository.save(existingEvent);
                log.info("Updated existing failed event to PERMANENTLY_FAILED: eventId={}", eventId);
            }
            
        } catch (Exception e) {
            log.error("Failed to save DLT message to database", e);
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
    
    private void saveFailedEvent(String eventId, String eventType, String topic, 
                                 int partition, long offset, String payload, Exception exception) {
        try {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            
            FailedEvent failedEvent = FailedEvent.builder()
                    .eventId(eventId != null ? eventId : "unknown")
                    .eventType(eventType)
                    .topic(topic)
                    .partition(partition)
                    .offset(offset)
                    .payload(payload)
                    .errorMessage(exception.getMessage())
                    .stackTrace(sw.toString())
                    .retryCount(0)
                    .status(FailedEvent.FailureStatus.FAILED)
                    .build();
            
            failedEventRepository.save(failedEvent);
            log.info("Saved failed event to database: eventId={}", eventId);
            
        } catch (Exception e) {
            log.error("Failed to save failed event to database", e);
        }
    }
}
