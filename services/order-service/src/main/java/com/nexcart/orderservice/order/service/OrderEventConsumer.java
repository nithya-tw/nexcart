package com.nexcart.orderservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.exception.ResourceNotFoundException;
import com.nexcart.orderservice.order.entity.FailedEvent;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.entity.ProcessedEvent;
import com.nexcart.orderservice.order.event.InventoryReservationFailedEvent;
import com.nexcart.orderservice.order.event.InventoryReservedEvent;
import com.nexcart.orderservice.order.repository.FailedEventRepository;
import com.nexcart.orderservice.order.repository.OrderRepository;
import com.nexcart.orderservice.order.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    @Transactional
    public void handleInventoryEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        String eventId = null;
        try {
            log.info("Received message from {}: partition={}, offset={}", topic, partition, offset);
            
            Object event = objectMapper.readValue(message, Object.class);
            
            if (message.contains("\"eventType\":\"InventoryReserved\"")) {
                InventoryReservedEvent reservedEvent = objectMapper.readValue(message, 
                        InventoryReservedEvent.class);
                eventId = reservedEvent.getEventId();
                
                if (processedEventRepository.existsByEventId(eventId)) {
                    log.info("Event already processed (idempotency): eventId={}", eventId);
                    if (acknowledgment != null) acknowledgment.acknowledge();
                    return;
                }
                
                handleInventoryReserved(reservedEvent);
                
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                        .eventId(eventId)
                        .eventType("InventoryReserved")
                        .build();
                processedEventRepository.save(processedEvent);
                
            } else if (message.contains("\"eventType\":\"InventoryReservationFailed\"")) {
                InventoryReservationFailedEvent failedEvent = objectMapper.readValue(message, 
                        InventoryReservationFailedEvent.class);
                eventId = failedEvent.getEventId();
                
                if (processedEventRepository.existsByEventId(eventId)) {
                    log.info("Event already processed (idempotency): eventId={}", eventId);
                    if (acknowledgment != null) acknowledgment.acknowledge();
                    return;
                }
                
                handleInventoryReservationFailed(failedEvent);
                
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                        .eventId(eventId)
                        .eventType("InventoryReservationFailed")
                        .build();
                processedEventRepository.save(processedEvent);
                
            } else {
                log.debug("Ignoring unknown event type in message");
            }
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Failed to process inventory event from topic={}, partition={}, offset={}", 
                    topic, partition, offset, e);
            
            saveFailedEvent(eventId, "Unknown", topic, partition, offset, message, e);
            
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
    
    @KafkaListener(topics = "inventory-events.DLT", groupId = "order-service-dlt-group")
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
                        .eventType("Unknown")
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
