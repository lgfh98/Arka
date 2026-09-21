package com.arka.backend.cart.domain;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.event.CartAbandonedEvent;
import com.arka.backend.cart.domain.model.event.CartItemAddedEvent;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Nivel 1: Pruebas de Dominio Puro - Cart (Mutation-Ready)")
class CartDomainTest {

    @Test
    @DisplayName("Debe agregar ítems al carrito e incrementar cantidad si ya existe")
    void shouldAddItemsToCartAndAccumulateQuantity() {
        CartId id = CartId.generate();
        UUID customerId = UUID.randomUUID();
        Cart cart = Cart.create(id, customerId);

        UUID product1 = UUID.randomUUID();
        cart.addItem(product1, 2, new BigDecimal("25.00"));
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Agregar de nuevo el mismo producto
        cart.addItem(product1, 3, new BigDecimal("25.00"));
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(5);
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("125.00"));

        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).hasSize(2);
        assertThat(events.getFirst()).isInstanceOf(CartItemAddedEvent.class);
    }

    @Test
    @DisplayName("Debe remover ítems del carrito")
    void shouldRemoveItemFromCart() {
        Cart cart = Cart.create(CartId.generate(), UUID.randomUUID());
        UUID product = UUID.randomUUID();
        cart.addItem(product, 2, new BigDecimal("10.00"));
        assertThat(cart.getItems()).hasSize(1);

        cart.removeItem(product);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("INV-07: Debe detectar abandono de carrito tras ventana de inactividad")
    void shouldDetectAbandonedCart() {
        CartId id = CartId.generate();
        UUID customerId = UUID.randomUUID();
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(3));

        Cart cart = new Cart(id, customerId, CartStatus.ACTIVE, null, twoHoursAgo);
        cart.addItem(UUID.randomUUID(), 2, new BigDecimal("50.00"));
        cart.pullDomainEvents(); // Clear events

        cart.markAsAbandoned();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.ABANDONED);
        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(CartAbandonedEvent.class);
    }
}
