package com.arka.backend.shared.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
