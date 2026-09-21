package com.arka.backend.ordering.domain.model.event;

import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        OrderId orderId,
        CustomerId customerId,
        List<OrderItemPayload> items,
        Instant occurredOn
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "PurchaseOrder";
    }

    @Override
    public String aggregateId() {
        return orderId.value().toString();
    }
}
