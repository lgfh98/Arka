package com.arka.backend.shared.infrastructure.outbox;

import com.arka.backend.shared.domain.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDomainEventPublisher {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity entity = OutboxEventJpaEntity.builder()
                    .eventId(event.eventId())
                    .aggregateType(event.aggregateType())
                    .aggregateId(event.aggregateId())
                    .eventType(event.getClass().getName())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(entity);
            log.info("Evento guardado en Outbox: [{}] id={}", event.getClass().getSimpleName(), event.eventId());

            // Publica notificación para relay After-Commit
            applicationEventPublisher.publishEvent(new OutboxEventStoredNotification(event.eventId()));
        } catch (JsonProcessingException e) {
            log.error("Error al serializar evento para Outbox: {}", event, e);
            throw new RuntimeException("Fallo al serializar DomainEvent en Outbox", e);
        }
    }

    public record OutboxEventStoredNotification(java.util.UUID eventId) {}
}
