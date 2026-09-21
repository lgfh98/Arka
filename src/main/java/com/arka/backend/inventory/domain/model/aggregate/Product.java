package com.arka.backend.inventory.domain.model.aggregate;

import com.arka.backend.inventory.domain.exception.InvalidProductDataException;
import com.arka.backend.inventory.domain.model.event.ProductRegisteredEvent;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Product {

    private final ProductId id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Map<String, String> attributes;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Product(ProductId id, String name, String description, BigDecimal price, String category, Map<String, String> attributes) {
        validateInvariants(name, price, category);
        this.id = Objects.requireNonNull(id, "ProductId no puede ser nulo");
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.price = price;
        this.category = category.trim();
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    public static Product register(ProductId id, String name, String description, BigDecimal price, String category, Map<String, String> attributes) {
        Product product = new Product(id, name, description, price, category, attributes);
        product.recordEvent(new ProductRegisteredEvent(
                UUID.randomUUID(),
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                Instant.now()
        ));
        return product;
    }

    public void updateDetails(String name, String description, BigDecimal price, String category, Map<String, String> attributes) {
        validateInvariants(name, price, category);
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.price = price;
        this.category = category.trim();
        if (attributes != null) {
            this.attributes = new HashMap<>(attributes);
        }
    }

    private void validateInvariants(String name, BigDecimal price, String category) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidProductDataException("El nombre del producto no puede estar vacío (INV-01)");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidProductDataException("La categoría del producto no puede estar vacía (INV-01)");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductDataException("El precio debe ser estrictamente mayor a cero (INV-01)");
        }
    }

    protected void recordEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
}
