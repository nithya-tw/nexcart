package com.nexcart.inventoryservice.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.inventoryservice.inventory.entity.FailedEvent;
import com.nexcart.inventoryservice.inventory.repository.FailedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DLQController.class)
@DisplayName("DLQ Controller Tests")
class DLQControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FailedEventRepository failedEventRepository;

    @Test
    @DisplayName("Should return all failed events")
    void shouldReturnAllFailedEvents() throws Exception {
        FailedEvent event1 = createFailedEvent("event-1", FailedEvent.FailureStatus.FAILED);
        FailedEvent event2 = createFailedEvent("event-2", FailedEvent.FailureStatus.PERMANENTLY_FAILED);
        
        when(failedEventRepository.findAll()).thenReturn(List.of(event1, event2));

        mockMvc.perform(get("/api/v1/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventId", is("event-1")))
                .andExpect(jsonPath("$[1].eventId", is("event-2")));

        verify(failedEventRepository).findAll();
    }

    @Test
    @DisplayName("Should filter failed events by status")
    void shouldFilterFailedEventsByStatus() throws Exception {
        FailedEvent event = createFailedEvent("event-1", FailedEvent.FailureStatus.PERMANENTLY_FAILED);
        
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/dlq/status/PERMANENTLY_FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PERMANENTLY_FAILED")));

        verify(failedEventRepository).findByStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED);
    }

    @Test
    @DisplayName("Should return failed event by event ID")
    void shouldReturnFailedEventByEventId() throws Exception {
        FailedEvent event = createFailedEvent("event-123", FailedEvent.FailureStatus.FAILED);
        
        when(failedEventRepository.findByEventId("event-123"))
                .thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/v1/dlq/event-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is("event-123")))
                .andExpect(jsonPath("$.eventType", is("InventoryReserved")));

        verify(failedEventRepository).findByEventId("event-123");
    }

    @Test
    @DisplayName("Should return 404 when event ID not found")
    void shouldReturn404WhenEventIdNotFound() throws Exception {
        when(failedEventRepository.findByEventId("non-existent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/dlq/non-existent"))
                .andExpect(status().isNotFound());

        verify(failedEventRepository).findByEventId("non-existent");
    }

    @Test
    @DisplayName("Should return DLQ statistics")
    void shouldReturnDlqStatistics() throws Exception {
        when(failedEventRepository.count()).thenReturn(5L);
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.FAILED))
                .thenReturn(List.of(createFailedEvent("e1", FailedEvent.FailureStatus.FAILED)));
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.RETRYING))
                .thenReturn(List.of());
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.RESOLVED))
                .thenReturn(List.of(createFailedEvent("e2", FailedEvent.FailureStatus.RESOLVED)));
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED))
                .thenReturn(List.of(createFailedEvent("e3", FailedEvent.FailureStatus.PERMANENTLY_FAILED)));

        mockMvc.perform(get("/api/v1/dlq/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.failed", is(1)))
                .andExpect(jsonPath("$.retrying", is(0)))
                .andExpect(jsonPath("$.resolved", is(1)))
                .andExpect(jsonPath("$.permanently_failed", is(1)));

        verify(failedEventRepository).count();
        verify(failedEventRepository, times(4)).findByStatus(any());
    }

    @Test
    @DisplayName("Should mark failed event as resolved")
    void shouldMarkFailedEventAsResolved() throws Exception {
        FailedEvent event = createFailedEvent("event-123", FailedEvent.FailureStatus.FAILED);
        FailedEvent resolvedEvent = createFailedEvent("event-123", FailedEvent.FailureStatus.RESOLVED);
        
        when(failedEventRepository.findByEventId("event-123"))
                .thenReturn(Optional.of(event));
        when(failedEventRepository.save(any(FailedEvent.class)))
                .thenReturn(resolvedEvent);

        mockMvc.perform(put("/api/v1/dlq/event-123/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is("event-123")))
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        verify(failedEventRepository).findByEventId("event-123");
        verify(failedEventRepository).save(any(FailedEvent.class));
    }

    @Test
    @DisplayName("Should delete failed event")
    void shouldDeleteFailedEvent() throws Exception {
        FailedEvent event = createFailedEvent("event-123", FailedEvent.FailureStatus.RESOLVED);
        
        when(failedEventRepository.findByEventId("event-123"))
                .thenReturn(Optional.of(event));
        doNothing().when(failedEventRepository).delete(event);

        mockMvc.perform(delete("/api/v1/dlq/event-123"))
                .andExpect(status().isNoContent());

        verify(failedEventRepository).findByEventId("event-123");
        verify(failedEventRepository).delete(event);
    }

    @Test
    @DisplayName("Should return events with inventory-specific error details")
    void shouldReturnEventsWithInventoryErrorDetails() throws Exception {
        FailedEvent event = createFailedEvent("event-123", FailedEvent.FailureStatus.FAILED);
        event.setErrorMessage("Insufficient stock for product 1. Available: 5, Requested: 10");
        event.setRetryCount(3);
        
        when(failedEventRepository.findByEventId("event-123"))
                .thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/v1/dlq/event-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorMessage", containsString("Insufficient stock")))
                .andExpect(jsonPath("$.retryCount", is(3)));
    }

    @Test
    @DisplayName("Should handle empty DLQ stats")
    void shouldHandleEmptyDlqStats() throws Exception {
        when(failedEventRepository.count()).thenReturn(0L);
        when(failedEventRepository.findByStatus(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dlq/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.failed", is(0)))
                .andExpect(jsonPath("$.permanently_failed", is(0)));
    }

    private FailedEvent createFailedEvent(String eventId, FailedEvent.FailureStatus status) {
        return FailedEvent.builder()
                .id(1L)
                .eventId(eventId)
                .eventType("InventoryReserved")
                .topic("inventory-events")
                .partition(0)
                .offset(456L)
                .payload("{\"orderId\":1,\"items\":[]}")
                .errorMessage("Test error")
                .stackTrace("Stack trace here")
                .retryCount(0)
                .status(status)
                .build();
    }
}
