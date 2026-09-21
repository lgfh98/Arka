package com.arka.backend.inventory.infrastructure.web.mapper;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.infrastructure.web.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductWebMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;
        return new ProductResponse(
                product.getId().value(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getAttributes()
        );
    }
}
