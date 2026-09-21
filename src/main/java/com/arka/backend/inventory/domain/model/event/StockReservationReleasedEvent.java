package com.arka.backend.inventory.domain.model.event;

import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StockReservationReleasedEvent(
        UUID eventId,
        InventoryId inventoryId,
        ProductId productId,
        int quantity,
        Instant occurredOn
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "InventoryItem";
    }

    @Override
    public String aggregateId() {
        return inventoryId.value().toString();
    }
}
