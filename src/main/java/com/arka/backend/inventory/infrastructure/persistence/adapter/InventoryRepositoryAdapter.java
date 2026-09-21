package com.arka.backend.inventory.infrastructure.persistence.adapter;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import com.arka.backend.inventory.infrastructure.persistence.mapper.InventoryJpaMapper;
import com.arka.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryJpaMapper inventoryJpaMapper;

    @Override
    public InventoryItem save(InventoryItem inventoryItem) {
        var entity = inventoryJpaMapper.toEntity(inventoryItem);
        inventoryJpaRepository.save(entity);
        return inventoryItem;
    }

    @Override
    public Optional<InventoryItem> findByProductId(ProductId productId) {
        return inventoryJpaRepository.findByProductId(productId.value())
                .map(inventoryJpaMapper::toDomain);
    }

    @Override
    public List<InventoryItem> findAll() {
        return inventoryJpaRepository.findAll().stream()
                .map(inventoryJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<InventoryItem> findLowStockItems() {
        return inventoryJpaRepository.findLowStockItems().stream()
                .map(inventoryJpaMapper::toDomain)
                .toList();
    }
}
