package com.nexcart.inventoryservice.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.inventoryservice.inventory.entity.FailedEvent;
import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.ProcessedEvent;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.event.OrderPlacedEvent;
import com.nexcart.inventoryservice.inventory.repository.FailedEventRepository;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.ProcessedEventRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Event Consumer Tests")
class InventoryEventConsumerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private FailedEventRepository failedEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    private Inventory testInventory;
    private OrderPlacedEvent testEvent;

    @BeforeEach
    void setUp() {
        testInventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(0)
                .build();

        OrderPlacedEvent.OrderItemDto item = new OrderPlacedEvent.OrderItemDto(
                1L, 10, BigDecimal.valueOf(50.00)
        );
        
        testEvent = new OrderPlacedEvent(
                1L, "ORD-001", 1L,
                List.of(item),
                BigDecimal.valueOf(500.00)
        );
    }

    @Test
    @DisplayName("Should process OrderPlaced event and reserve stock successfully")
    void shouldProcessOrderPlacedEventAndReserveStock() throws Exception {
        String eventJson = "{\"eventId\":\"evt-123\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(new StockReservation());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 100L, acknowledgment);

        verify(inventoryRepository).findByProductId(1L);
        verify(stockReservationRepository).save(any(StockReservation.class));
        
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getReservedQuantity()).isEqualTo(10);
        
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(acknowledgment).acknowledge();
        verify(kafkaTemplate).send(eq("inventory-events"), any(String.class), any());
    }

    @Test
    @DisplayName("Should skip processing if event already processed (idempotency)")
    void shouldSkipProcessingIfEventAlreadyProcessed() throws Exception {
        testEvent.setEventId("evt-duplicate");
        String eventJson = "{\"eventId\":\"evt-duplicate\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId("evt-duplicate")).thenReturn(true);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 101L, acknowledgment);

        verify(processedEventRepository).existsByEventId("evt-duplicate");
        verify(inventoryRepository, never()).findByProductId(any());
        verify(stockReservationRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should publish InventoryReservationFailed when product not found")
    void shouldPublishInventoryReservationFailedWhenProductNotFound() throws Exception {
        String eventJson = "{\"eventId\":\"evt-no-product\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 102L, acknowledgment);

        verify(inventoryRepository).findByProductId(1L);
        verify(stockReservationRepository, never()).save(any());
        verify(kafkaTemplate).send(eq("inventory-events"), any(String.class), any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should publish InventoryReservationFailed when insufficient stock")
    void shouldPublishInventoryReservationFailedWhenInsufficientStock() throws Exception {
        testInventory.setQuantity(5);
        testInventory.setReservedQuantity(0);
        
        String eventJson = "{\"eventId\":\"evt-insufficient\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 103L, acknowledgment);

        verify(inventoryRepository).findByProductId(1L);
        verify(stockReservationRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
        verify(kafkaTemplate).send(eq("inventory-events"), any(String.class), any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should ignore non-OrderPlaced event types")
    void shouldIgnoreNonOrderPlacedEventTypes() throws Exception {
        OrderPlacedEvent wrongTypeEvent = new OrderPlacedEvent(1L, "ORD-001", 1L, List.of(), BigDecimal.ZERO);
        wrongTypeEvent.setEventType("DifferentEvent");
        
        String eventJson = "{\"eventId\":\"evt-wrong\",\"eventType\":\"DifferentEvent\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(wrongTypeEvent);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 104L, acknowledgment);

        verify(inventoryRepository, never()).findByProductId(any());
        verify(processedEventRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should save failed event when processing throws exception")
    void shouldSaveFailedEventWhenProcessingThrowsException() throws Exception {
        String eventJson = "{\"eventId\":\"evt-error\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class)))
                .thenThrow(new RuntimeException("Deserialization error"));
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(new FailedEvent());

        assertThatThrownBy(() ->
                inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 105L, acknowledgment)
        ).isInstanceOf(RuntimeException.class);

        verify(failedEventRepository).save(any(FailedEvent.class));
        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle DLT message and save to failed_events")
    void shouldHandleDltMessageAndSaveToFailedEvents() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-events.DLT", 0, 200L, "evt-dlt-123", 
                "{\"eventId\":\"evt-dlt-123\",\"orderId\":1}"
        );
        
        when(failedEventRepository.findByEventId("evt-dlt-123")).thenReturn(Optional.empty());
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(new FailedEvent());

        inventoryEventConsumer.handleDLTMessage(record);

        ArgumentCaptor<FailedEvent> failedEventCaptor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(failedEventCaptor.capture());
        
        FailedEvent savedEvent = failedEventCaptor.getValue();
        assertThat(savedEvent.getEventId()).isEqualTo("evt-dlt-123");
        assertThat(savedEvent.getEventType()).isEqualTo("OrderPlaced");
        assertThat(savedEvent.getStatus()).isEqualTo(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
        assertThat(savedEvent.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update existing failed event when DLT message already exists")
    void shouldUpdateExistingFailedEventWhenDltMessageAlreadyExists() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-events.DLT", 0, 200L, "evt-dlt-existing", 
                "{\"eventId\":\"evt-dlt-existing\",\"orderId\":1}"
        );
        
        FailedEvent existingEvent = FailedEvent.builder()
                .eventId("evt-dlt-existing")
                .status(FailedEvent.FailureStatus.FAILED)
                .build();
        
        when(failedEventRepository.findByEventId("evt-dlt-existing"))
                .thenReturn(Optional.of(existingEvent));
        when(failedEventRepository.save(any(FailedEvent.class))).thenReturn(existingEvent);

        inventoryEventConsumer.handleDLTMessage(record);

        ArgumentCaptor<FailedEvent> failedEventCaptor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(failedEventCaptor.capture());
        
        FailedEvent updatedEvent = failedEventCaptor.getValue();
        assertThat(updatedEvent.getStatus()).isEqualTo(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
    }

    @Test
    @DisplayName("Should create stock reservation with correct expiry time")
    void shouldCreateStockReservationWithCorrectExpiryTime() throws Exception {
        String eventJson = "{\"eventId\":\"evt-reservation\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(new StockReservation());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 106L, acknowledgment);

        ArgumentCaptor<StockReservation> reservationCaptor = ArgumentCaptor.forClass(StockReservation.class);
        verify(stockReservationRepository).save(reservationCaptor.capture());
        
        StockReservation savedReservation = reservationCaptor.getValue();
        assertThat(savedReservation.getOrderId()).isEqualTo(1L);
        assertThat(savedReservation.getQuantity()).isEqualTo(10);
        assertThat(savedReservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.RESERVED);
        assertThat(savedReservation.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle acknowledgment when null")
    void shouldHandleAcknowledgmentWhenNull() throws Exception {
        String eventJson = "{\"eventId\":\"evt-no-ack\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(new StockReservation());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 107L, null);

        verify(stockReservationRepository).save(any(StockReservation.class));
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should save processed event with correct event ID and type")
    void shouldSaveProcessedEventWithCorrectEventIdAndType() throws Exception {
        String eventJson = "{\"eventId\":\"evt-processed\",\"eventType\":\"OrderPlaced\",\"orderId\":1}";
        
        when(objectMapper.readValue(eq(eventJson), eq(OrderPlacedEvent.class))).thenReturn(testEvent);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(new StockReservation());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(new ProcessedEvent());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

        inventoryEventConsumer.handleOrderPlacedEvent(eventJson, "order-events", 0, 108L, acknowledgment);

        ArgumentCaptor<ProcessedEvent> processedEventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        
        ProcessedEvent savedProcessedEvent = processedEventCaptor.getValue();
        assertThat(savedProcessedEvent.getEventId()).isNotNull();
        assertThat(savedProcessedEvent.getEventType()).isEqualTo("OrderPlaced");
    }
}
