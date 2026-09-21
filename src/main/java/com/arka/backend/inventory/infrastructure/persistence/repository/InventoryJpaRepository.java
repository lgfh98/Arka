package com.arka.backend.inventory.infrastructure.persistence.repository;

import com.arka.backend.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, UUID> {
    Optional<InventoryJpaEntity> findByProductId(UUID productId);

    @Query("SELECT i FROM InventoryJpaEntity i WHERE (i.physicalStock - i.reservedStock) <= i.minimumThreshold")
    List<InventoryJpaEntity> findLowStockItems();
}
