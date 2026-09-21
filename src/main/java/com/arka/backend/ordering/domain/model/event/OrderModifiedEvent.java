package com.arka.backend.ordering.domain.model.event;

import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderModifiedEvent(
        UUID eventId,
        OrderId orderId,
        CustomerId customerId,
        List<OrderItemPayload> oldItems,
        List<OrderItemPayload> newItems,
        BigDecimal newTotalAmount,
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
