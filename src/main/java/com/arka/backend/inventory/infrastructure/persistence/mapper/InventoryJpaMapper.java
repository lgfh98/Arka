package com.arka.backend.inventory.infrastructure.persistence.mapper;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InventoryJpaMapper {

    public InventoryJpaEntity toEntity(InventoryItem domain) {
        if (domain == null) return null;
        return InventoryJpaEntity.builder()
                .id(domain.getId().value())
                .productId(domain.getProductId().value())
                .physicalStock(domain.getPhysicalStock())
                .reservedStock(domain.getReservedStock())
                .minimumThreshold(domain.getMinimumThreshold())
                .updatedAt(Instant.now())
                .build();
    }

    public InventoryItem toDomain(InventoryJpaEntity entity) {
        if (entity == null) return null;
        return new InventoryItem(
                new InventoryId(entity.getId()),
                new ProductId(entity.getProductId()),
                entity.getPhysicalStock(),
                entity.getReservedStock(),
                entity.getMinimumThreshold()
        );
    }
}
