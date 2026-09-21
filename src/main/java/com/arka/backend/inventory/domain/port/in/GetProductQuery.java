package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;

import java.util.List;

public interface GetProductQuery {
    Product getById(ProductId id);
    List<Product> getAll();
    List<Product> getByCategory(String category);
}
