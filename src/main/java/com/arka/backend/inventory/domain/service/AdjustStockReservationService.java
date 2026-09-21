package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.AdjustStockReservationUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class AdjustStockReservationService implements AdjustStockReservationUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryItem adjustReservation(UUID orderId, ProductId productId, int oldQuantity, int newQuantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventario no encontrado para el producto: " + productId.value()));

        item.adjustReservation(orderId, oldQuantity, newQuantity);
        return inventoryRepository.save(item);
    }
}
