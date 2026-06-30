package com.nexcart.orderservice.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.orderservice.order.entity.FailedEvent;
import com.nexcart.orderservice.order.repository.FailedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @DisplayName("Should return empty list when no failed events")
    void shouldReturnEmptyListWhenNoFailedEvents() throws Exception {
        when(failedEventRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(failedEventRepository).findAll();
    }

    @Test
    @DisplayName("Should filter failed events by status")
    void shouldFilterFailedEventsByStatus() throws Exception {
        FailedEvent event = createFailedEvent("event-1", FailedEvent.FailureStatus.FAILED);
        
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.FAILED))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/dlq/status/FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("FAILED")));

        verify(failedEventRepository).findByStatus(FailedEvent.FailureStatus.FAILED);
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
                .andExpect(jsonPath("$.eventType", is("OrderPlaced")));

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
        when(failedEventRepository.count()).thenReturn(10L);
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.FAILED))
                .thenReturn(List.of(createFailedEvent("e1", FailedEvent.FailureStatus.FAILED),
                        createFailedEvent("e2", FailedEvent.FailureStatus.FAILED)));
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.RETRYING))
                .thenReturn(List.of(createFailedEvent("e3", FailedEvent.FailureStatus.RETRYING)));
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.RESOLVED))
                .thenReturn(List.of());
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED))
                .thenReturn(List.of(createFailedEvent("e4", FailedEvent.FailureStatus.PERMANENTLY_FAILED),
                        createFailedEvent("e5", FailedEvent.FailureStatus.PERMANENTLY_FAILED),
                        createFailedEvent("e6", FailedEvent.FailureStatus.PERMANENTLY_FAILED)));

        mockMvc.perform(get("/api/v1/dlq/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(10)))
                .andExpect(jsonPath("$.failed", is(2)))
                .andExpect(jsonPath("$.retrying", is(1)))
                .andExpect(jsonPath("$.resolved", is(0)))
                .andExpect(jsonPath("$.permanently_failed", is(3)));

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
    @DisplayName("Should return 404 when resolving non-existent event")
    void shouldReturn404WhenResolvingNonExistentEvent() throws Exception {
        when(failedEventRepository.findByEventId("non-existent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/dlq/non-existent/resolve"))
                .andExpect(status().isNotFound());

        verify(failedEventRepository).findByEventId("non-existent");
        verify(failedEventRepository, never()).save(any());
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
    @DisplayName("Should return 404 when deleting non-existent event")
    void shouldReturn404WhenDeletingNonExistentEvent() throws Exception {
        when(failedEventRepository.findByEventId("non-existent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/dlq/non-existent"))
                .andExpect(status().isNotFound());

        verify(failedEventRepository).findByEventId("non-existent");
        verify(failedEventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should handle different failure statuses")
    void shouldHandleDifferentFailureStatuses() throws Exception {
        FailedEvent retryingEvent = createFailedEvent("event-1", FailedEvent.FailureStatus.RETRYING);
        retryingEvent.setRetryCount(2);
        
        when(failedEventRepository.findByStatus(FailedEvent.FailureStatus.RETRYING))
                .thenReturn(List.of(retryingEvent));

        mockMvc.perform(get("/api/v1/dlq/status/RETRYING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].retryCount", is(2)));

        verify(failedEventRepository).findByStatus(FailedEvent.FailureStatus.RETRYING);
    }

    @Test
    @DisplayName("Should return events with error details")
    void shouldReturnEventsWithErrorDetails() throws Exception {
        FailedEvent event = createFailedEvent("event-123", FailedEvent.FailureStatus.FAILED);
        event.setErrorMessage("Product not found in inventory");
        event.setStackTrace("java.lang.RuntimeException: Product not found...");
        event.setRetryCount(3);
        
        when(failedEventRepository.findByEventId("event-123"))
                .thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/v1/dlq/event-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorMessage", is("Product not found in inventory")))
                .andExpect(jsonPath("$.retryCount", is(3)));
    }

    private FailedEvent createFailedEvent(String eventId, FailedEvent.FailureStatus status) {
        return FailedEvent.builder()
                .id(1L)
                .eventId(eventId)
                .eventType("OrderPlaced")
                .topic("order-events")
                .partition(0)
                .offset(123L)
                .payload("{\"orderId\":1}")
                .errorMessage("Test error")
                .stackTrace("Stack trace here")
                .retryCount(0)
                .status(status)
                .build();
    }
}
