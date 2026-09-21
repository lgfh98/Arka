package com.arka.backend.inventory.infrastructure.persistence.adapter;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.out.ProductRepository;
import com.arka.backend.inventory.infrastructure.persistence.mapper.ProductJpaMapper;
import com.arka.backend.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductJpaMapper productJpaMapper;

    @Override
    public Product save(Product product) {
        var entity = productJpaMapper.toEntity(product);
        productJpaRepository.save(entity);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return productJpaRepository.findById(id.value())
                .map(productJpaMapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(productJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByCategory(String category) {
        return productJpaRepository.findByCategoryIgnoreCase(category).stream()
                .map(productJpaMapper::toDomain)
                .toList();
    }
}
