package com.arka.backend.shared.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private final OutboxEventJpaRepository outboxRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventStored(OutboxDomainEventPublisher.OutboxEventStoredNotification notification) {
        outboxRepository.findById(notification.eventId()).ifPresent(outboxEventProcessor::processEvent);
    }

    @Scheduled(fixedDelay = 5000)
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents =
                outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (!pendingEvents.isEmpty()) {
            log.debug("Procesando {} eventos pendientes en Outbox relay", pendingEvents.size());
            for (OutboxEventJpaEntity event : pendingEvents) {
                outboxEventProcessor.processEvent(event);
            }
        }
    }
}
