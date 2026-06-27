package com.nexcart.inventoryservice.inventory.repository;

import com.nexcart.inventoryservice.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderLevel")
    List<Inventory> findLowStockInventories();

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity > 0")
    List<Inventory> findInStockInventories();

    boolean existsByProductId(Long productId);
}
