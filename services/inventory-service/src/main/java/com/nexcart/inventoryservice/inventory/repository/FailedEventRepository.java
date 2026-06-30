package com.nexcart.inventoryservice.inventory.repository;

import com.nexcart.inventoryservice.inventory.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {
    
    Optional<FailedEvent> findByEventId(String eventId);
    
    List<FailedEvent> findByStatus(FailedEvent.FailureStatus status);
    
    List<FailedEvent> findByTopicAndStatus(String topic, FailedEvent.FailureStatus status);
    
    boolean existsByEventId(String eventId);
}
