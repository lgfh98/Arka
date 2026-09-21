package com.arka.backend.notification.infrastructure.event;

import com.arka.backend.cart.domain.model.event.CartAbandonedEvent;
import com.arka.backend.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.arka.backend.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import com.arka.backend.ordering.domain.model.event.OrderCancelledEvent;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderDeliveredEvent;
import com.arka.backend.ordering.domain.model.event.OrderInDispatchEvent;
import com.arka.backend.ordering.domain.model.event.OrderRegisteredEvent;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaEntity;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String CONSUMER_CONTEXT = "notification";

    private final NotificationJpaRepository notificationRepository;
    private final ProcessedEventJpaRepository processedEventRepository;

    @Transactional
    @EventListener
    public void onOrderRegistered(OrderRegisteredEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().value().toString() + "@arka-client.com",
                "Orden Registrada: " + event.orderId().value(),
                "Su orden con " + event.items().size() + " productos y total $" + event.totalAmount() + " ha sido registrada en estado PENDIENTE.",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().value().toString() + "@arka-client.com",
                "Orden Confirmada: " + event.orderId().value(),
                "Su orden ha sido confirmada y pasa a proceso logístico de preparación.",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderInDispatch(OrderInDispatchEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().value().toString() + "@arka-client.com",
                "Orden en Despacho: " + event.orderId().value(),
                "Su pedido ha salido de nuestro centro de distribución y está en camino.",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderDelivered(OrderDeliveredEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().value().toString() + "@arka-client.com",
                "Orden Entregada: " + event.orderId().value(),
                "Su pedido ha sido entregado exitosamente en su almacén. ¡Gracias por elegir Arka!",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().value().toString() + "@arka-client.com",
                "Orden Cancelada: " + event.orderId().value(),
                "Su orden ha sido cancelada y el stock retenido fue liberado.",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onCartAbandoned(CartAbandonedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        saveNotification(
                event.customerId().toString() + "@arka-client.com",
                "Recordatorio: Tienes productos pendientes en tu carrito de Arka",
                "Hola, notamos que dejaste " + event.totalItems() + " productos en tu carrito por un valor estimado de $" +
                        event.estimatedTotal() + ". ¡Completa tu pedido antes de que se agoten!",
                "EMAIL",
                event.getClass().getSimpleName()
        );

        markAsProcessed(event.eventId());
    }

    private void saveNotification(String recipient, String subject, String message, String channel, String eventType) {
        var entity = NotificationJpaEntity.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .channel(channel)
                .eventType(eventType)
                .sentAt(Instant.now())
                .build();
        notificationRepository.save(entity);
        log.info("[Notification] Despachada notificación para {} | Asunto: {}", recipient, subject);
    }

    private boolean isAlreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsByEventIdAndConsumerContext(eventId, CONSUMER_CONTEXT)) {
            log.warn("[Notification] Evento duplicado ignorado: {}", eventId);
            return true;
        }
        return false;
    }

    private void markAsProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEventJpaEntity(eventId, CONSUMER_CONTEXT, Instant.now()));
    }
}
