package com.nexcart.orderservice.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.common.event.DomainEvent;
import com.nexcart.orderservice.order.entity.OutboxEvent;
import com.nexcart.orderservice.order.event.OrderPlacedEvent;
import com.nexcart.orderservice.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ORDER_EVENTS_TOPIC = "order-events";
    
    @Transactional
    public void saveEventToOutbox(DomainEvent event, String aggregateType, Long aggregateId) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .processed(false)
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            log.info("Saved event to outbox: eventId={}, type={}", event.getEventId(), event.getEventType());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON", e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
    
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop100ByProcessedFalseOrderByCreatedAtAsc();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Found {} pending events to publish", pendingEvents.size());
        
        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                // Deserialize based on event type
                Object domainEvent;
                switch (outboxEvent.getEventType()) {
                    case "OrderPlaced":
                        domainEvent = objectMapper.readValue(
                                outboxEvent.getPayload(), 
                                OrderPlacedEvent.class
                        );
                        break;
                    default:
                        log.warn("Unknown event type: {}", outboxEvent.getEventType());
                        continue;
                }
                
                kafkaTemplate.send(ORDER_EVENTS_TOPIC, outboxEvent.getEventId(), domainEvent)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                markAsProcessed(outboxEvent.getId());
                                log.info("Published event to Kafka: eventId={}, offset={}", 
                                        outboxEvent.getEventId(), 
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to publish event to Kafka: eventId={}", 
                                        outboxEvent.getEventId(), ex);
                            }
                        });
                
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}", outboxEvent.getId(), e);
            }
        }
    }
    
    @Transactional
    public void markAsProcessed(Long outboxEventId) {
        outboxEventRepository.findById(outboxEventId).ifPresent(event -> {
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
        });
    }
}
