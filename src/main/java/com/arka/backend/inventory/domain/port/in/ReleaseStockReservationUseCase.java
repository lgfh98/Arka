package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

public interface ReleaseStockReservationUseCase {
    InventoryItem releaseReservation(ProductId productId, int quantity);
}
