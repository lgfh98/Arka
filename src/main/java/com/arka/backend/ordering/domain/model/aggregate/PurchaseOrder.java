package com.arka.backend.ordering.domain.model.aggregate;

import com.arka.backend.ordering.domain.exception.EmptyOrderException;
import com.arka.backend.ordering.domain.exception.InvalidOrderStateException;
import com.arka.backend.ordering.domain.exception.OrderNotModifiableException;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.event.OrderCancelledEvent;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderDeliveredEvent;
import com.arka.backend.ordering.domain.model.event.OrderInDispatchEvent;
import com.arka.backend.ordering.domain.model.event.OrderItemPayload;
import com.arka.backend.ordering.domain.model.event.OrderModifiedEvent;
import com.arka.backend.ordering.domain.model.event.OrderRegisteredEvent;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.model.valueobject.OrderStatus;
import com.arka.backend.shared.domain.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class PurchaseOrder {

    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public PurchaseOrder(OrderId id, CustomerId customerId, OrderStatus status, List<OrderItem> items,
                         BigDecimal totalAmount, Instant createdAt, Instant updatedAt) {
        validateItems(items);
        this.id = Objects.requireNonNull(id, "OrderId no puede ser nulo");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId no puede ser nulo");
        this.status = Objects.requireNonNull(status, "OrderStatus no puede ser nulo");
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount != null ? totalAmount : calculateTotal(items);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static PurchaseOrder create(OrderId id, CustomerId customerId, List<OrderItem> items) {
        validateItems(items);
        BigDecimal total = calculateTotal(items);
        PurchaseOrder order = new PurchaseOrder(
                id,
                customerId,
                OrderStatus.PENDING,
                items,
                total,
                Instant.now(),
                Instant.now()
        );

        order.recordEvent(new OrderRegisteredEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                toPayloadList(items),
                total,
                order.getCreatedAt()
        ));
        return order;
    }

    public void modifyItems(List<OrderItem> newItems) {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderNotModifiableException(
                    "Solo se pueden modificar pedidos en estado 'PENDIENTE'. Estado actual: " + this.status + " (INV-04)"
            );
        }
        validateItems(newItems);

        List<OrderItemPayload> oldPayloads = toPayloadList(this.items);
        this.items = new ArrayList<>(newItems);
        this.totalAmount = calculateTotal(newItems);
        this.updatedAt = Instant.now();

        recordEvent(new OrderModifiedEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                oldPayloads,
                toPayloadList(newItems),
                this.totalAmount,
                this.updatedAt
        ));
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Solo se pueden confirmar pedidos en estado 'PENDIENTE'. Estado actual: " + this.status + " (INV-05)"
            );
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();

        recordEvent(new OrderConfirmedEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                toPayloadList(this.items),
                this.totalAmount,
                this.updatedAt
        ));
    }

    public void dispatch() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Solo se pueden despachar pedidos en estado 'CONFIRMADO'. Estado actual: " + this.status + " (INV-05)"
            );
        }
        this.status = OrderStatus.IN_DISPATCH;
        this.updatedAt = Instant.now();

        recordEvent(new OrderInDispatchEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                this.updatedAt
        ));
    }

    public void markDelivered() {
        if (this.status != OrderStatus.IN_DISPATCH) {
            throw new InvalidOrderStateException(
                    "Solo se pueden marcar como entregados pedidos en estado 'EN_DESPACHO'. Estado actual: " + this.status + " (INV-05)"
            );
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();

        recordEvent(new OrderDeliveredEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                this.updatedAt
        ));
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderNotModifiableException(
                    "Solo se pueden cancelar pedidos en estado 'PENDIENTE'. Estado actual: " + this.status + " (INV-04 / INV-05)"
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();

        recordEvent(new OrderCancelledEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                toPayloadList(this.items),
                this.updatedAt
        ));
    }

    private static void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderException("Una orden de compra debe contener al menos un producto (INV-06)");
        }
    }

    private static BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<OrderItemPayload> toPayloadList(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemPayload(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
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
