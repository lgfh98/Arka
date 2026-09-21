package com.arka.backend.shared.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(OutboxEventJpaEntity entity) {
        try {
            Class<?> targetClass = Class.forName(entity.getEventType());
            Object domainEvent = objectMapper.readValue(entity.getPayload(), targetClass);

            applicationEventPublisher.publishEvent(domainEvent);

            entity.setStatus(OutboxStatus.PROCESSED);
            entity.setProcessedAt(Instant.now());
            outboxRepository.save(entity);
            log.info("Evento despachado exitosamente desde Outbox: [{}] id={}",
                    targetClass.getSimpleName(), entity.getEventId());
        } catch (Exception e) {
            log.error("Error al procesar evento Outbox id={}: {}", entity.getEventId(), e.getMessage());
            entity.setRetryCount(entity.getRetryCount() + 1);
            if (entity.getRetryCount() >= 5) {
                entity.setStatus(OutboxStatus.FAILED);
            }
            outboxRepository.save(entity);
        }
    }
}
