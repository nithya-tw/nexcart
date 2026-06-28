package com.nexcart.orderservice.order.repository;

import com.nexcart.orderservice.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
