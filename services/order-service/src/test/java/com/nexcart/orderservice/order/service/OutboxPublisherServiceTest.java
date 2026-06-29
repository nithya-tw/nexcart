package com.nexcart.orderservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.common.event.DomainEvent;
import com.nexcart.orderservice.order.entity.OutboxEvent;
import com.nexcart.orderservice.order.event.OrderPlacedEvent;
import com.nexcart.orderservice.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisherService Tests")
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisherService outboxPublisherService;

    private OrderPlacedEvent testEvent;
    private OutboxEvent testOutboxEvent;

    @BeforeEach
    void setUp() {
        testEvent = new OrderPlacedEvent();
        testEvent.setEventId("evt-123");
        testEvent.setEventType("OrderPlaced");
        testEvent.setOrderId(1L);
        testEvent.setTimestamp(Instant.now());

        testOutboxEvent = OutboxEvent.builder()
                .id(1L)
                .eventId("evt-123")
                .eventType("OrderPlaced")
                .aggregateType("Order")
                .aggregateId(1L)
                .payload("{\"eventId\":\"evt-123\"}")
                .processed(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save event to outbox successfully")
    void shouldSaveEventToOutboxSuccessfully() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"evt-123\"}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(testOutboxEvent);

        // Act
        outboxPublisherService.saveEventToOutbox(testEvent, "Order", 1L);

        // Assert
        verify(objectMapper).writeValueAsString(testEvent);
        verify(outboxEventRepository).save(argThat(event ->
                event.getEventId().equals("evt-123") &&
                event.getEventType().equals("OrderPlaced") &&
                event.getAggregateType().equals("Order") &&
                event.getAggregateId().equals(1L) &&
                !event.getProcessed()
        ));
    }

    @Test
    @DisplayName("Should throw exception when JSON serialization fails")
    void shouldThrowExceptionWhenJsonSerializationFails() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            outboxPublisherService.saveEventToOutbox(testEvent, "Order", 1L);
        });

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should publish pending events successfully")
    void shouldPublishPendingEventsSuccessfully() throws Exception {
        // Arrange
        List<OutboxEvent> pendingEvents = List.of(testOutboxEvent);
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(pendingEvents);
        when(objectMapper.readValue(anyString(), eq(OrderPlacedEvent.class)))
                .thenReturn(testEvent);
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, Object>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        // Create a mock SendResult that returns the future
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.BiConsumer<SendResult<String, Object>, Throwable> action = 
                invocation.getArgument(0);
            // Simulate successful send
            action.accept(mock(SendResult.class), null);
            return null;
        }).when(future).whenComplete(any());

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        verify(outboxEventRepository).findTop100ByProcessedFalseOrderByCreatedAtAsc();
        verify(objectMapper).readValue(testOutboxEvent.getPayload(), OrderPlacedEvent.class);
        verify(kafkaTemplate).send(eq("order-events"), eq("evt-123"), any(OrderPlacedEvent.class));
    }

    @Test
    @DisplayName("Should do nothing when no pending events")
    void shouldDoNothingWhenNoPendingEvents() {
        // Arrange
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(Collections.emptyList());

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        verify(outboxEventRepository).findTop100ByProcessedFalseOrderByCreatedAtAsc();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should skip unknown event types")
    void shouldSkipUnknownEventTypes() {
        // Arrange
        testOutboxEvent.setEventType("UnknownEvent");
        List<OutboxEvent> pendingEvents = List.of(testOutboxEvent);
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(pendingEvents);

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        verify(outboxEventRepository).findTop100ByProcessedFalseOrderByCreatedAtAsc();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should mark event as processed")
    void shouldMarkEventAsProcessed() {
        // Arrange
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(testOutboxEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(testOutboxEvent);

        // Act
        outboxPublisherService.markAsProcessed(1L);

        // Assert
        verify(outboxEventRepository).findById(1L);
        verify(outboxEventRepository).save(argThat(event ->
                event.getProcessed() &&
                event.getProcessedAt() != null
        ));
    }

    @Test
    @DisplayName("Should do nothing when event not found for marking processed")
    void shouldDoNothingWhenEventNotFoundForMarkingProcessed() {
        // Arrange
        when(outboxEventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        outboxPublisherService.markAsProcessed(999L);

        // Assert
        verify(outboxEventRepository).findById(999L);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle Kafka publish failure gracefully")
    void shouldHandleKafkaPublishFailureGracefully() throws Exception {
        // Arrange
        List<OutboxEvent> pendingEvents = List.of(testOutboxEvent);
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(pendingEvents);
        when(objectMapper.readValue(anyString(), eq(OrderPlacedEvent.class)))
                .thenReturn(testEvent);
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, Object>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        // Simulate Kafka failure
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.BiConsumer<SendResult<String, Object>, Throwable> action = 
                invocation.getArgument(0);
            action.accept(null, new RuntimeException("Kafka unavailable"));
            return null;
        }).when(future).whenComplete(any());

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        verify(kafkaTemplate).send(anyString(), anyString(), any());
        // Event should NOT be marked as processed on failure
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle JSON deserialization failure gracefully")
    void shouldHandleJsonDeserializationFailureGracefully() throws Exception {
        // Arrange
        List<OutboxEvent> pendingEvents = List.of(testOutboxEvent);
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(pendingEvents);
        when(objectMapper.readValue(anyString(), eq(OrderPlacedEvent.class)))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        verify(objectMapper).readValue(anyString(), eq(OrderPlacedEvent.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
