package com.arka.backend.inventory.domain;

import com.arka.backend.inventory.domain.exception.InvalidProductDataException;
import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.event.ProductRegisteredEvent;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nivel 1: Pruebas de Dominio Puro - Product (Mutation-Ready)")
class ProductDomainTest {

    @Test
    @DisplayName("Debe registrar un producto válidamente y emitir ProductRegisteredEvent")
    void shouldRegisterProductSuccessfully() {
        // Arrange
        ProductId id = ProductId.generate();
        String name = "Mouse Gamer RGB";
        String description = "Mouse óptico 12000 DPI";
        BigDecimal price = new BigDecimal("49.99");
        String category = "Periféricos";
        Map<String, String> attributes = Map.of("marca", "Razer", "color", "Negro");

        // Act
        Product product = Product.register(id, name, description, price, category, attributes);

        // Assert
        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getPrice()).isEqualByComparingTo(price);
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getAttributes()).containsEntry("marca", "Razer");

        // Pull events
        List<DomainEvent> events = product.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(ProductRegisteredEvent.class);
        ProductRegisteredEvent event = (ProductRegisteredEvent) events.getFirst();
        assertThat(event.productId()).isEqualTo(id);
        assertThat(event.price()).isEqualByComparingTo(price);

        // Pulling again must be empty
        assertThat(product.pullDomainEvents()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("INV-01: Debe fallar si el nombre está vacío o en blanco")
    void shouldRejectBlankName(String invalidName) {
        ProductId id = ProductId.generate();
        assertThatThrownBy(() -> Product.register(
                id, invalidName, "Desc", new BigDecimal("10.00"), "Periféricos", Map.of()
        )).isInstanceOf(InvalidProductDataException.class)
          .hasMessageContaining("INV-01");
    }

    @Test
    @DisplayName("INV-01: Debe fallar si el precio es cero o negativo (Valores límite)")
    void shouldRejectZeroOrNegativePrice() {
        ProductId id = ProductId.generate();

        // Exact boundary 0.00
        assertThatThrownBy(() -> Product.register(
                id, "Teclado", "Desc", BigDecimal.ZERO, "Periféricos", Map.of()
        )).isInstanceOf(InvalidProductDataException.class);

        // Negative boundary -0.01
        assertThatThrownBy(() -> Product.register(
                id, "Teclado", "Desc", new BigDecimal("-0.01"), "Periféricos", Map.of()
        )).isInstanceOf(InvalidProductDataException.class);
    }
}
