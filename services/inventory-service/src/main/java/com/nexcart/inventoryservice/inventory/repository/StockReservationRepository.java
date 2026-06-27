package com.nexcart.inventoryservice.inventory.repository;

import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderId(Long orderId);

    List<StockReservation> findByStatus(StockReservation.ReservationStatus status);

    @Query("SELECT r FROM StockReservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(LocalDateTime now);

    List<StockReservation> findByInventoryId(Long inventoryId);
}
