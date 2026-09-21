package com.arka.backend.inventory.domain.service;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.RegisterProductUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import com.arka.backend.inventory.domain.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterProductService implements RegisterProductUseCase {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public Product registerProduct(RegisterProductCommand command) {
        ProductId productId = ProductId.generate();
        Product product = Product.register(
                productId,
                command.name(),
                command.description(),
                command.price(),
                command.category(),
                command.attributes()
        );
        productRepository.save(product);

        InventoryItem inventoryItem = InventoryItem.initialize(
                InventoryId.generate(),
                productId,
                Math.max(0, command.initialStock()),
                Math.max(0, command.minimumThreshold())
        );
        inventoryRepository.save(inventoryItem);

        return product;
    }
}
