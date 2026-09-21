package com.arka.backend.cart.domain.model.aggregate;

import com.arka.backend.cart.domain.model.entity.CartItem;
import com.arka.backend.cart.domain.model.event.CartAbandonedEvent;
import com.arka.backend.cart.domain.model.event.CartItemAddedEvent;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.shared.domain.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Cart {

    private final CartId id;
    private final UUID customerId;
    private CartStatus status;
    private List<CartItem> items;
    private Instant lastActivityAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Cart(CartId id, UUID customerId, CartStatus status, List<CartItem> items, Instant lastActivityAt) {
        this.id = Objects.requireNonNull(id, "CartId no puede ser nulo");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId no puede ser nulo");
        this.status = Objects.requireNonNull(status, "CartStatus no puede ser nulo");
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.lastActivityAt = lastActivityAt != null ? lastActivityAt : Instant.now();
    }

    public static Cart create(CartId id, UUID customerId) {
        return new Cart(id, customerId, CartStatus.ACTIVE, new ArrayList<>(), Instant.now());
    }

    public void addItem(UUID productId, int quantity, BigDecimal unitPrice) {
        if (this.status != CartStatus.ACTIVE) {
            this.status = CartStatus.ACTIVE;
        }

        var existing = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
        } else {
            items.add(new CartItem(productId, quantity, unitPrice));
        }

        this.lastActivityAt = Instant.now();
        recordEvent(new CartItemAddedEvent(
                UUID.randomUUID(),
                this.id,
                this.customerId,
                productId,
                quantity,
                this.lastActivityAt
        ));
    }

    public void removeItem(UUID productId) {
        this.items.removeIf(item -> item.getProductId().equals(productId));
        this.lastActivityAt = Instant.now();
    }

    public void markAsAbandoned() {
        if (this.status == CartStatus.ACTIVE && !this.items.isEmpty()) {
            this.status = CartStatus.ABANDONED;
            recordEvent(new CartAbandonedEvent(
                    UUID.randomUUID(),
                    this.id,
                    this.customerId,
                    this.items.size(),
                    getTotal(),
                    this.lastActivityAt,
                    Instant.now()
            ));
        }
    }

    public void markAsConverted() {
        this.status = CartStatus.CONVERTED;
        this.lastActivityAt = Instant.now();
    }

    public boolean isInactiveFor(Duration duration) {
        return Duration.between(this.lastActivityAt, Instant.now()).compareTo(duration) > 0;
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
