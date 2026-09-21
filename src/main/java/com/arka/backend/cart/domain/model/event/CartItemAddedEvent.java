package com.arka.backend.cart.domain.model.event;

import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CartItemAddedEvent(
        UUID eventId,
        CartId cartId,
        UUID customerId,
        UUID productId,
        int quantity,
        Instant occurredOn
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "Cart";
    }

    @Override
    public String aggregateId() {
        return cartId.value().toString();
    }
}
