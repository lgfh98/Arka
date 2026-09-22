package com.arka.backend.ordering.infrastructure.persistence;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.model.valueobject.OrderStatus;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import com.arka.backend.ordering.infrastructure.persistence.entity.OrderJpaEntity;
import com.arka.backend.ordering.infrastructure.persistence.repository.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Nivel 2: Pruebas de Integración - Persistencia de Órdenes y Mitigación N+1")
class OrderPersistenceIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Test
    @DisplayName("Debe consultar órdenes por cliente cargando items mediante EntityGraph en un solo query")
    void shouldFindOrdersByCustomerIdWithItemsViaEntityGraph() {
        UUID customerId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        // Crear Orden 1
        PurchaseOrder order1 = new PurchaseOrder(
                OrderId.generate(),
                new CustomerId(customerId),
                OrderStatus.PENDING,
                List.of(
                        new OrderItem(productA, 2, new BigDecimal("50.00")),
                        new OrderItem(productB, 1, new BigDecimal("100.00"))
                ),
                new BigDecimal("200.00"),
                Instant.now(),
                Instant.now()
        );

        // Crear Orden 2
        PurchaseOrder order2 = new PurchaseOrder(
                OrderId.generate(),
                new CustomerId(customerId),
                OrderStatus.CONFIRMED,
                List.of(
                        new OrderItem(productA, 5, new BigDecimal("50.00"))
                ),
                new BigDecimal("250.00"),
                Instant.now(),
                Instant.now()
        );

        orderRepository.save(order1);
        orderRepository.save(order2);

        // Consultar repositorio Spring Data JPA directamente con EntityGraph
        List<OrderJpaEntity> entities = orderJpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getItems()).isNotEmpty();
        assertThat(entities.get(1).getItems()).isNotEmpty();

        // Consultar a través del adapter de dominio
        List<PurchaseOrder> domainOrders = orderRepository.findByCustomerId(new CustomerId(customerId));
        assertThat(domainOrders).hasSize(2);
        assertThat(domainOrders.stream().flatMap(o -> o.getItems().stream()).toList()).hasSize(3);
    }

    @Test
    @DisplayName("Debe consultar orden individual por ID con sus items vía EntityGraph")
    void shouldFindOrderByIdWithItems() {
        UUID customerId = UUID.randomUUID();
        OrderId orderId = OrderId.generate();

        PurchaseOrder order = new PurchaseOrder(
                orderId,
                new CustomerId(customerId),
                OrderStatus.PENDING,
                List.of(
                        new OrderItem(UUID.randomUUID(), 3, new BigDecimal("75.00"))
                ),
                new BigDecimal("225.00"),
                Instant.now(),
                Instant.now()
        );

        orderRepository.save(order);

        Optional<PurchaseOrder> found = orderRepository.findById(orderId);
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().getFirst().getQuantity()).isEqualTo(3);
    }
}
