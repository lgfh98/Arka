package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

import java.util.List;

public interface GetInventoryQuery {
    InventoryItem getByProductId(ProductId productId);
    List<InventoryItem> getAll();
    List<InventoryItem> getLowStockItems();
}
