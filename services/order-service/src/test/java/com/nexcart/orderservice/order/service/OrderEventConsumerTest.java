package com.nexcart.orderservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.order.entity.FailedEvent;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.entity.ProcessedEvent;
import com.nexcart.orderservice.order.event.InventoryReservationFailedEvent;
import com.nexcart.orderservice.order.event.InventoryReservedEvent;
import com.nexcart.orderservice.order.repository.FailedEventRepository;
import com.nexcart.orderservice.order.repository.OrderRepository;
import com.nexcart.orderservice.order.repository.ProcessedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Event Consumer Tests")
class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private FailedEventRepository failedEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-001");
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should process InventoryReserved event and update order to CONFIRMED")
    void shouldProcessInventoryReservedEvent() throws Exception {
        String eventJson = "{\"eventId\":\"evt-123\",\"eventType\":\"InventoryReserved\",\"orderId\":1}";
        InventoryReservedEvent event = new InventoryReservedEvent(1L, 1L, List.of());
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 100L, acknowledgment);

        verify(orderRepository).findById(1L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process InventoryReservationFailed event and update order to CANCELLED")
    void shouldProcessInventoryReservationFailedEvent() throws Exception {
        String eventJson = "{\"eventId\":\"evt-456\",\"eventType\":\"InventoryReservationFailed\",\"orderId\":1,\"reason\":\"Insufficient stock\"}";
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(1L, "Insufficient stock");
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservationFailedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 101L, acknowledgment);

        verify(orderRepository).findById(1L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should skip processing if event already processed (idempotency)")
    void shouldSkipProcessingIfEventAlreadyProcessed() throws Exception {
        String eventJson = "{\"eventId\":\"evt-123\",\"eventType\":\"InventoryReserved\",\"orderId\":1}";
        InventoryReservedEvent event = new InventoryReservedEvent(1L, 1L, List.of());
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt-123")).thenReturn(true);

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 100L, acknowledgment);

        verify(processedEventRepository).existsByEventId("evt-123");
        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not update order if status is not PENDING")
    void shouldNotUpdateOrderIfStatusIsNotPending() throws Exception {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        
        String eventJson = "{\"eventId\":\"evt-789\",\"eventType\":\"InventoryReserved\",\"orderId\":1}";
        InventoryReservedEvent event = new InventoryReservedEvent(1L, 1L, List.of());
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 102L, acknowledgment);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should save failed event when processing throws exception")
    void shouldSaveFailedEventWhenProcessingThrowsException() throws Exception {
        String eventJson = "{\"eventId\":\"evt-error\",\"eventType\":\"InventoryReserved\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class)))
                .thenThrow(new RuntimeException("Deserialization error"));
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(new FailedEvent());

        assertThatThrownBy(() ->
                orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 103L, acknowledgment)
        ).isInstanceOf(RuntimeException.class);

        verify(failedEventRepository).save(any(FailedEvent.class));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle DLT message and save to failed_events")
    void shouldHandleDltMessageAndSaveToFailedEvents() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "inventory-events.DLT", 0, 200L, "evt-dlt-123", 
                "{\"eventId\":\"evt-dlt-123\",\"orderId\":1}"
        );
        
        when(failedEventRepository.findByEventId("evt-dlt-123")).thenReturn(Optional.empty());
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(new FailedEvent());

        orderEventConsumer.handleDLTMessage(record);

        ArgumentCaptor<FailedEvent> failedEventCaptor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(failedEventCaptor.capture());
        
        FailedEvent savedEvent = failedEventCaptor.getValue();
        assertThat(savedEvent.getEventId()).isEqualTo("evt-dlt-123");
        assertThat(savedEvent.getStatus()).isEqualTo(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
        assertThat(savedEvent.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update existing failed event when DLT message already exists")
    void shouldUpdateExistingFailedEventWhenDltMessageAlreadyExists() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "inventory-events.DLT", 0, 200L, "evt-dlt-existing", 
                "{\"eventId\":\"evt-dlt-existing\",\"orderId\":1}"
        );
        
        FailedEvent existingEvent = FailedEvent.builder()
                .eventId("evt-dlt-existing")
                .status(FailedEvent.FailureStatus.FAILED)
                .build();
        
        when(failedEventRepository.findByEventId("evt-dlt-existing"))
                .thenReturn(Optional.of(existingEvent));
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(existingEvent);

        orderEventConsumer.handleDLTMessage(record);

        ArgumentCaptor<FailedEvent> failedEventCaptor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(failedEventCaptor.capture());
        
        FailedEvent updatedEvent = failedEventCaptor.getValue();
        assertThat(updatedEvent.getStatus()).isEqualTo(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
    }

    @Test
    @DisplayName("Should ignore unknown event types")
    void shouldIgnoreUnknownEventTypes() throws Exception {
        String eventJson = "{\"eventId\":\"evt-unknown\",\"eventType\":\"UnknownEvent\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 104L, acknowledgment);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle acknowledgment when null")
    void shouldHandleAcknowledgmentWhenNull() throws Exception {
        String eventJson = "{\"eventId\":\"evt-no-ack\",\"eventType\":\"InventoryReserved\",\"orderId\":1}";
        InventoryReservedEvent event = new InventoryReservedEvent(1L, 1L, List.of());
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 105L, null);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should save processed event with correct event type")
    void shouldSaveProcessedEventWithCorrectEventType() throws Exception {
        String eventJson = "{\"eventId\":\"evt-type-check\",\"eventType\":\"InventoryReservationFailed\",\"orderId\":1,\"reason\":\"Test\"}";
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(1L, "Test");
        
        when(objectMapper.readValue(eq(eventJson), eq(Object.class))).thenReturn(new Object());
        when(objectMapper.readValue(eq(eventJson), eq(InventoryReservationFailedEvent.class))).thenReturn(event);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());

        orderEventConsumer.handleInventoryEvent(eventJson, "inventory-events", 0, 106L, acknowledgment);

        ArgumentCaptor<ProcessedEvent> processedEventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        
        ProcessedEvent savedProcessedEvent = processedEventCaptor.getValue();
        assertThat(savedProcessedEvent.getEventId()).isNotNull();
        assertThat(savedProcessedEvent.getEventType()).isEqualTo("InventoryReservationFailed");
    }
}
