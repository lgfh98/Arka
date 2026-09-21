package com.arka.backend.inventory.infrastructure.event;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderItemPayload;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Nivel 3: Pruebas de Bounded Context - Idempotent Consumer (At-Least-Once Resilience)")
class InventoryEventListenerIdempotencyTest {

    @Autowired
    private InventoryEventListener inventoryEventListener;

    @Autowired
    private InventoryJpaRepository inventoryRepository;

    @Autowired
    private ProcessedEventJpaRepository processedEventRepository;

    @Test
    @DisplayName("Idempotencia: Evento duplicado no debe re-aplicar descuento de stock")
    void shouldIgnoreDuplicateEventDeliveries() {
        // Arrange
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.initialize(InventoryId.generate(), new ProductId(productId), 100, 10);
        item.reserveStock(UUID.randomUUID(), 20); // Reservar 20 unidades previamente
        var entity = com.arka.backend.inventory.infrastructure.persistence.entity.InventoryJpaEntity.builder()
                .id(item.getId().value())
                .productId(productId)
                .physicalStock(100)
                .reservedStock(20)
                .minimumThreshold(10)
                .updatedAt(Instant.now())
                .build();
        inventoryRepository.save(entity);

        UUID duplicateEventId = UUID.randomUUID();
        OrderId orderId = OrderId.generate();
        CustomerId customerId = CustomerId.generate();
        List<OrderItemPayload> items = List.of(new OrderItemPayload(productId, 20, new BigDecimal("50.00")));

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                duplicateEventId,
                orderId,
                customerId,
                items,
                new BigDecimal("1000.00"),
                Instant.now()
        );

        // 1er Despacho (Normal)
        inventoryEventListener.onOrderConfirmed(event);

        var afterFirst = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(afterFirst.getPhysicalStock()).isEqualTo(80); // 100 - 20
        assertThat(afterFirst.getReservedStock()).isEqualTo(0);
        assertThat(processedEventRepository.existsByEventIdAndConsumerContext(duplicateEventId, "inventory")).isTrue();

        // 2do Despacho (Reintento de red / Redelivery duplicado)
        inventoryEventListener.onOrderConfirmed(event);

        var afterSecond = inventoryRepository.findByProductId(productId).orElseThrow();
        // El stock NO debe descontarse nuevamente (debe permanecer en 80)
        assertThat(afterSecond.getPhysicalStock()).isEqualTo(80);
    }
}
