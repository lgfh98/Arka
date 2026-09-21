package com.arka.backend.shared.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, ProcessedEventJpaEntity.ProcessedEventId> {
    boolean existsByEventIdAndConsumerContext(UUID eventId, String consumerContext);
}
