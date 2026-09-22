package com.arka.backend.cart.infrastructure.persistence;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.domain.port.out.CartRepository;
import com.arka.backend.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.arka.backend.cart.infrastructure.persistence.repository.CartJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Nivel 2: Pruebas de Integración - Persistencia de Carrito y Mitigación N+1")
class CartPersistenceIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartJpaRepository cartJpaRepository;

    @Test
    @DisplayName("Debe consultar carritos por estado cargando items mediante EntityGraph")
    void shouldFindCartsByStatusWithItemsViaEntityGraph() {
        UUID customer1 = UUID.randomUUID();
        UUID customer2 = UUID.randomUUID();

        Cart cart1 = Cart.create(CartId.generate(), customer1);
        cart1.addItem(UUID.randomUUID(), 2, new java.math.BigDecimal("25.00"));
        cart1.addItem(UUID.randomUUID(), 1, new java.math.BigDecimal("50.00"));

        Cart cart2 = Cart.create(CartId.generate(), customer2);
        cart2.addItem(UUID.randomUUID(), 4, new java.math.BigDecimal("15.00"));

        cartRepository.save(cart1);
        cartRepository.save(cart2);

        List<CartJpaEntity> activeCarts = cartJpaRepository.findByStatus(CartStatus.ACTIVE);
        assertThat(activeCarts).isNotEmpty();
        assertThat(activeCarts.stream().anyMatch(c -> c.getCustomerId().equals(customer1))).isTrue();

        List<Cart> domainCarts = cartRepository.findByStatus(CartStatus.ACTIVE);
        assertThat(domainCarts).isNotEmpty();

        Cart foundCart1 = domainCarts.stream()
                .filter(c -> c.getCustomerId().equals(customer1))
                .findFirst()
                .orElseThrow();
        assertThat(foundCart1.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Debe consultar carrito por customerId cargando items con EntityGraph")
    void shouldFindCartByCustomerIdWithItems() {
        UUID customerId = UUID.randomUUID();
        Cart cart = Cart.create(CartId.generate(), customerId);
        cart.addItem(UUID.randomUUID(), 5, new java.math.BigDecimal("30.00"));

        cartRepository.save(cart);

        Optional<Cart> found = cartRepository.findByCustomerId(customerId);
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().getFirst().getQuantity()).isEqualTo(5);
    }
}
