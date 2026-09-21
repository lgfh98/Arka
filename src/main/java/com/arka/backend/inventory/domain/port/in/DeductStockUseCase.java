package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

public interface DeductStockUseCase {
    InventoryItem deductStock(ProductId productId, int quantity);
}
