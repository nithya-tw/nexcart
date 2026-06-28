package com.nexcart.inventoryservice.inventory.repository;

import com.nexcart.inventoryservice.inventory.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    boolean existsByEventId(String eventId);
}
