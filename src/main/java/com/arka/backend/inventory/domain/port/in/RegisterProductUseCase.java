package com.arka.backend.inventory.domain.port.in;

import com.arka.backend.inventory.domain.model.aggregate.Product;

import java.math.BigDecimal;
import java.util.Map;

public interface RegisterProductUseCase {

    Product registerProduct(RegisterProductCommand command);

    record RegisterProductCommand(
            String name,
            String description,
            BigDecimal price,
            String category,
            Map<String, String> attributes,
            int initialStock,
            int minimumThreshold
    ) {}
}
