package com.nexcart.orderservice.order.repository;

import com.nexcart.orderservice.order.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    boolean existsByEventId(String eventId);
}
