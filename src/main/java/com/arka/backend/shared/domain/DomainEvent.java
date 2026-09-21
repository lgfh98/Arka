package com.arka.backend.shared.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();
    String aggregateType();
    String aggregateId();
}
