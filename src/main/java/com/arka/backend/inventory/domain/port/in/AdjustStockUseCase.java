package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

public interface AdjustStockUseCase {

    InventoryItem adjustStock(AdjustStockCommand command);

    record AdjustStockCommand(
            ProductId productId,
            int newStock,
            String reason
    ) {}
}
