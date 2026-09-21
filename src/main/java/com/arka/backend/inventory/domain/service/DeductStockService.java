package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.DeductStockUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeductStockService implements DeductStockUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryItem deductStock(ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventario no encontrado para el producto: " + productId.value()));

        item.deductPermanent(quantity);
        return inventoryRepository.save(item);
    }
}
