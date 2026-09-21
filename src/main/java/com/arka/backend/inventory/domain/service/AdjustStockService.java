package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.port.in.AdjustStockUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdjustStockService implements AdjustStockUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryItem adjustStock(AdjustStockCommand command) {
        InventoryItem item = inventoryRepository.findByProductId(command.productId())
                .orElseThrow(() -> new InventoryNotFoundException("No se encontró inventario para el producto: " + command.productId().value()));

        item.adjustPhysicalStock(command.newStock(), command.reason());
        return inventoryRepository.save(item);
    }
}
