package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.GetInventoryQuery;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetInventoryService implements GetInventoryQuery {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryItem getByProductId(ProductId productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventario no encontrado para el producto: " + productId.value()));
    }

    @Override
    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public List<InventoryItem> getLowStockItems() {
        return inventoryRepository.findLowStockItems();
    }
}
