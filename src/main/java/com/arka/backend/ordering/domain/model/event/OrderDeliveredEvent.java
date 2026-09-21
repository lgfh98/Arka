package com.arka.backend.ordering.domain.model.event;

import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderDeliveredEvent(
        UUID eventId,
        OrderId orderId,
        CustomerId customerId,
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
