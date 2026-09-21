package com.arka.backend.shared.infrastructure.outbox;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.port.in.RegisterProductUseCase;
import com.arka.backend.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.CreateOrderUseCase;
import com.arka.backend.ordering.infrastructure.persistence.repository.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Nivel 2: Pruebas de Integración - Atomicidad Agregado y Transactional Outbox")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private RegisterProductUseCase registerProductUseCase;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxRepository;

    @Test
    @DisplayName("Debe persistir el agregado Product y su evento en outbox_events")
    void shouldPersistProductAndOutboxEventAtomically() {
        // Arrange
        var command = new RegisterProductUseCase.RegisterProductCommand(
                "Mouse Inalámbrico Pro Test",
                "Descripción test",
                new BigDecimal("99.90"),
                "Periféricos",
                Map.of("color", "Negro"),
                20,
                5
        );

        // Act
        Product product = registerProductUseCase.registerProduct(command);

        // Assert: Agregado persistido
        assertThat(productJpaRepository.findById(product.getId().value())).isPresent();

        // Assert: Evento persistido en Outbox
        var outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents)
                .anyMatch(e -> e.getAggregateId().equals(product.getId().value().toString())
                        && e.getAggregateType().equals("Product"));
    }

    @Test
    @DisplayName("Debe persistir la orden PurchaseOrder y su evento en outbox_events")
    void shouldPersistOrderAndOutboxEventAtomically() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var command = new CreateOrderUseCase.CreateOrderCommand(
                customerId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, 2, new BigDecimal("45.00")))
        );

        // Act
        PurchaseOrder order = createOrderUseCase.createOrder(command);

        // Assert: Agregado persistido
        assertThat(orderJpaRepository.findById(order.getId().value())).isPresent();

        // Assert: Evento persistido en Outbox
        var outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents)
                .anyMatch(e -> e.getAggregateId().equals(order.getId().value().toString())
                        && e.getAggregateType().equals("PurchaseOrder"));
    }
}
