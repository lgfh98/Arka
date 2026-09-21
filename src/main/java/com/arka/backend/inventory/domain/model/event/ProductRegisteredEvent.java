package com.arka.backend.inventory.domain.model.event;

import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductRegisteredEvent(
        UUID eventId,
        ProductId productId,
        String name,
        String category,
        BigDecimal price,
        Instant occurredOn
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "Product";
    }

    @Override
    public String aggregateId() {
        return productId.value().toString();
    }
}
