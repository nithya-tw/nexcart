package com.nexcart.orderservice.order.controller;

import com.nexcart.orderservice.order.entity.FailedEvent;
import com.nexcart.orderservice.order.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
@Slf4j
public class DLQController {
    
    private final FailedEventRepository failedEventRepository;
    
    @GetMapping
    public ResponseEntity<List<FailedEvent>> getAllFailedEvents() {
        log.info("Fetching all failed events");
        return ResponseEntity.ok(failedEventRepository.findAll());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FailedEvent>> getFailedEventsByStatus(
            @PathVariable FailedEvent.FailureStatus status) {
        log.info("Fetching failed events with status: {}", status);
        return ResponseEntity.ok(failedEventRepository.findByStatus(status));
    }
    
    @GetMapping("/{eventId}")
    public ResponseEntity<FailedEvent> getFailedEventByEventId(@PathVariable String eventId) {
        log.info("Fetching failed event: {}", eventId);
        return failedEventRepository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getFailedEventsStats() {
        log.info("Fetching failed events statistics");
        
        long total = failedEventRepository.count();
        long failed = failedEventRepository.findByStatus(FailedEvent.FailureStatus.FAILED).size();
        long retrying = failedEventRepository.findByStatus(FailedEvent.FailureStatus.RETRYING).size();
        long resolved = failedEventRepository.findByStatus(FailedEvent.FailureStatus.RESOLVED).size();
        long permanentlyFailed = failedEventRepository.findByStatus(FailedEvent.FailureStatus.PERMANENTLY_FAILED).size();
        
        Map<String, Long> stats = Map.of(
                "total", total,
                "failed", failed,
                "retrying", retrying,
                "resolved", resolved,
                "permanently_failed", permanentlyFailed
        );
        
        return ResponseEntity.ok(stats);
    }
    
    @PutMapping("/{eventId}/resolve")
    public ResponseEntity<FailedEvent> markAsResolved(@PathVariable String eventId) {
        log.info("Marking failed event as resolved: {}", eventId);
        
        return failedEventRepository.findByEventId(eventId)
                .map(event -> {
                    event.setStatus(FailedEvent.FailureStatus.RESOLVED);
                    return ResponseEntity.ok(failedEventRepository.save(event));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteFailedEvent(@PathVariable String eventId) {
        log.info("Deleting failed event: {}", eventId);
        
        return failedEventRepository.findByEventId(eventId)
                .map(event -> {
                    failedEventRepository.delete(event);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
