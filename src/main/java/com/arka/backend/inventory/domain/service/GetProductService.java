package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.exception.ProductNotFoundException;
import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.GetProductQuery;
import com.arka.backend.inventory.domain.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetProductService implements GetProductQuery {

    private final ProductRepository productRepository;

    @Override
    public Product getById(ProductId id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con id: " + id.value()));
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
