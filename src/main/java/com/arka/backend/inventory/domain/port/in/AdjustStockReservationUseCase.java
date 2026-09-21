package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

import java.util.UUID;

public interface AdjustStockReservationUseCase {
    InventoryItem adjustReservation(UUID orderId, ProductId productId, int oldQuantity, int newQuantity);
}
