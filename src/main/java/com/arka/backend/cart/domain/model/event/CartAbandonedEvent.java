package com.arka.backend.cart.domain.model.event;

import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartAbandonedEvent(
        UUID eventId,
        CartId cartId,
        UUID customerId,
        int totalItems,
        BigDecimal estimatedTotal,
        Instant lastActivityAt,
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
