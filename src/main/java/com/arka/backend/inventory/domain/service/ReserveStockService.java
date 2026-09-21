package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.ReserveStockUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ReserveStockService implements ReserveStockUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryItem reserveStock(UUID orderId, ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventario no encontrado para el producto: " + productId.value()));

        item.reserveStock(orderId, quantity);
        return inventoryRepository.save(item);
    }
}
