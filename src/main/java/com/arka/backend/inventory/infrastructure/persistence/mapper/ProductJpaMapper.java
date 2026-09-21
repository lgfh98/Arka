package com.arka.backend.inventory.infrastructure.persistence.mapper;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class ProductJpaMapper {

    public ProductJpaEntity toEntity(Product domain) {
        if (domain == null) return null;
        return ProductJpaEntity.builder()
                .id(domain.getId().value())
                .name(domain.getName())
                .description(domain.getDescription())
                .price(domain.getPrice())
                .category(domain.getCategory())
                .attributes(domain.getAttributes() != null ? new HashMap<>(domain.getAttributes()) : new HashMap<>())
                .build();
    }

    public Product toDomain(ProductJpaEntity entity) {
        if (entity == null) return null;
        return new Product(
                new ProductId(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getAttributes()
        );
    }
}
