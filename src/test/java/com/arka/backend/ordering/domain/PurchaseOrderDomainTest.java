package com.arka.backend.ordering.domain;

import com.arka.backend.ordering.domain.exception.EmptyOrderException;
import com.arka.backend.ordering.domain.exception.InvalidOrderStateException;
import com.arka.backend.ordering.domain.exception.OrderNotModifiableException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderDeliveredEvent;
import com.arka.backend.ordering.domain.model.event.OrderInDispatchEvent;
import com.arka.backend.ordering.domain.model.event.OrderModifiedEvent;
import com.arka.backend.ordering.domain.model.event.OrderRegisteredEvent;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.model.valueobject.OrderStatus;
import com.arka.backend.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nivel 1: Pruebas de Dominio Puro - PurchaseOrder (Mutation-Ready)")
class PurchaseOrderDomainTest {

    @Test
    @DisplayName("Debe crear una orden de compra en estado PENDIENTE y emitir OrderRegisteredEvent")
    void shouldCreateOrderSuccessfully() {
        OrderId orderId = OrderId.generate();
        CustomerId customerId = CustomerId.generate();
        UUID productId = UUID.randomUUID();
        OrderItem item = new OrderItem(productId, 5, new BigDecimal("100.00"));

        PurchaseOrder order = PurchaseOrder.create(orderId, customerId, List.of(item));

        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(order.getItems()).hasSize(1);

        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(OrderRegisteredEvent.class);
        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("INV-06: Debe rechazar una orden sin productos")
    void shouldRejectOrderWithEmptyItems() {
        OrderId orderId = OrderId.generate();
        CustomerId customerId = CustomerId.generate();

        assertThatThrownBy(() -> PurchaseOrder.create(orderId, customerId, Collections.emptyList()))
                .isInstanceOf(EmptyOrderException.class)
                .hasMessageContaining("INV-06");
    }

    @Test
    @DisplayName("INV-04: Debe permitir modificar productos solo en estado PENDIENTE")
    void shouldModifyOrderItemsWhenPending() {
        PurchaseOrder order = createSampleOrder();
        order.pullDomainEvents();

        UUID newProduct = UUID.randomUUID();
        OrderItem newItem = new OrderItem(newProduct, 2, new BigDecimal("200.00"));

        order.modifyItems(List.of(newItem));

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));

        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(OrderModifiedEvent.class);
    }

    @Test
    @DisplayName("INV-04: Debe rechazar modificaciones si la orden ya fue confirmada")
    void shouldRejectModificationWhenConfirmed() {
        PurchaseOrder order = createSampleOrder();
        order.confirm();

        UUID newProduct = UUID.randomUUID();
        OrderItem newItem = new OrderItem(newProduct, 2, new BigDecimal("200.00"));

        assertThatThrownBy(() -> order.modifyItems(List.of(newItem)))
                .isInstanceOf(OrderNotModifiableException.class)
                .hasMessageContaining("INV-04");
    }

    @Test
    @DisplayName("INV-05: Ciclo de vida completo de la orden (PENDIENTE -> CONFIRMADO -> EN_DESPACHO -> ENTREGADO)")
    void shouldFollowValidOrderLifecycleTransitions() {
        PurchaseOrder order = createSampleOrder();

        // 1. Confirm
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderConfirmedEvent);

        // 2. Dispatch
        order.dispatch();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_DISPATCH);
        events = order.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderInDispatchEvent);

        // 3. Deliver
        order.markDelivered();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        events = order.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderDeliveredEvent);
    }

    @Test
    @DisplayName("INV-05: Debe rechazar transiciones de estado inválidas")
    void shouldRejectInvalidStateTransitions() {
        PurchaseOrder order = createSampleOrder();

        // Intentar despachar directamente sin confirmar previamente
        assertThatThrownBy(order::dispatch)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("INV-05");

        // Intentar entregar directamente sin despachar previamente
        assertThatThrownBy(order::markDelivered)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("INV-05");
    }

    private PurchaseOrder createSampleOrder() {
        return PurchaseOrder.create(
                OrderId.generate(),
                CustomerId.generate(),
                List.of(new OrderItem(UUID.randomUUID(), 3, new BigDecimal("50.00")))
        );
    }
}
