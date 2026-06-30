package com.nexcart.inventoryservice.inventory.service;

import com.nexcart.inventoryservice.inventory.entity.Inventory;
import com.nexcart.inventoryservice.inventory.entity.StockReservation;
import com.nexcart.inventoryservice.inventory.repository.InventoryRepository;
import com.nexcart.inventoryservice.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {
    
    private final StockReservationRepository stockReservationRepository;
    private final InventoryRepository inventoryRepository;
    
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        log.debug("Starting cleanup of expired reservations");
        
        List<StockReservation> expiredReservations = 
            stockReservationRepository.findByStatusAndExpiresAtBefore(
                StockReservation.ReservationStatus.RESERVED,
                LocalDateTime.now()
            );
        
        if (expiredReservations.isEmpty()) {
            log.debug("No expired reservations found");
            return;
        }
        
        log.info("Found {} expired reservations to clean up", expiredReservations.size());
        
        for (StockReservation reservation : expiredReservations) {
            try {
                Inventory inventory = reservation.getInventory();
                
                Integer currentReserved = inventory.getReservedQuantity();
                inventory.setReservedQuantity(currentReserved - reservation.getQuantity());
                inventoryRepository.save(inventory);
                
                reservation.setStatus(StockReservation.ReservationStatus.EXPIRED);
                stockReservationRepository.save(reservation);
                
                log.info("Cleaned up expired reservation: reservationId={}, orderId={}, productId={}, quantity={}", 
                    reservation.getId(), 
                    reservation.getOrderId(),
                    inventory.getProductId(),
                    reservation.getQuantity());
                    
            } catch (Exception e) {
                log.error("Failed to cleanup expired reservation: reservationId={}", 
                    reservation.getId(), e);
            }
        }
        
        log.info("Cleanup completed. Released {} expired reservations", expiredReservations.size());
    }
}
