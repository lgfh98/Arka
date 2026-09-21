package com.arka.backend.inventory.infrastructure.web.mapper;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.infrastructure.web.dto.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryWebMapper {

    public InventoryResponse toResponse(InventoryItem item) {
        if (item == null) return null;
        return new InventoryResponse(
                item.getId().value(),
                item.getProductId().value(),
                item.getPhysicalStock(),
                item.getReservedStock(),
                item.getAvailableStock(),
                item.getMinimumThreshold(),
                item.isLowStock()
        );
    }
}
