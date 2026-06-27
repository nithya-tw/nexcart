package com.nexcart.orderservice.order.repository;

import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") Long userId);
}
