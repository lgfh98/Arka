package com.arka.backend.inventory.infrastructure.event;

import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.AdjustStockReservationUseCase;
import com.arka.backend.inventory.domain.port.in.DeductStockUseCase;
import com.arka.backend.inventory.domain.port.in.ReleaseStockReservationUseCase;
import com.arka.backend.inventory.domain.port.in.ReserveStockUseCase;
import com.arka.backend.ordering.domain.model.event.OrderCancelledEvent;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderItemPayload;
import com.arka.backend.ordering.domain.model.event.OrderModifiedEvent;
import com.arka.backend.ordering.domain.model.event.OrderRegisteredEvent;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaEntity;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private static final String CONSUMER_CONTEXT = "inventory";

    private final ReserveStockUseCase reserveStockUseCase;
    private final AdjustStockReservationUseCase adjustStockReservationUseCase;
    private final DeductStockUseCase deductStockUseCase;
    private final ReleaseStockReservationUseCase releaseStockReservationUseCase;
    private final ProcessedEventJpaRepository processedEventRepository;

    @Transactional
    @EventListener
    public void onOrderRegistered(OrderRegisteredEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Inventory] Procesando reserva para orden={}", event.orderId().value());
        for (OrderItemPayload item : event.items()) {
            reserveStockUseCase.reserveStock(
                    event.orderId().value(),
                    new ProductId(item.productId()),
                    item.quantity()
            );
        }

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderModified(OrderModifiedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Inventory] Reajustando reserva para orden={}", event.orderId().value());
        Map<UUID, Integer> oldMap = event.oldItems().stream()
                .collect(Collectors.toMap(OrderItemPayload::productId, OrderItemPayload::quantity, Integer::sum));

        for (OrderItemPayload newItem : event.newItems()) {
            int oldQty = oldMap.getOrDefault(newItem.productId(), 0);
            adjustStockReservationUseCase.adjustReservation(
                    event.orderId().value(),
                    new ProductId(newItem.productId()),
                    oldQty,
                    newItem.quantity()
            );
        }

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Inventory] Descontando stock permanente para orden={}", event.orderId().value());
        for (OrderItemPayload item : event.items()) {
            deductStockUseCase.deductStock(
                    new ProductId(item.productId()),
                    item.quantity()
            );
        }

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Inventory] Liberando reserva para orden cancelada={}", event.orderId().value());
        for (OrderItemPayload item : event.items()) {
            releaseStockReservationUseCase.releaseReservation(
                    new ProductId(item.productId()),
                    item.quantity()
            );
        }

        markAsProcessed(event.eventId());
    }

    private boolean isAlreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsByEventIdAndConsumerContext(eventId, CONSUMER_CONTEXT)) {
            log.warn("[Inventory] Evento duplicado ignorado: {}", eventId);
            return true;
        }
        return false;
    }

    private void markAsProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEventJpaEntity(eventId, CONSUMER_CONTEXT, Instant.now()));
    }
}
