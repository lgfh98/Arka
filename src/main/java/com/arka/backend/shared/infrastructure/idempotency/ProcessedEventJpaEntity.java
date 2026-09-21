package com.arka.backend.shared.infrastructure.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventJpaEntity.ProcessedEventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Id
    @Column(name = "consumer_context", nullable = false, length = 100)
    private String consumerContext;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessedEventId implements Serializable {
        private UUID eventId;
        private String consumerContext;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedEventId that)) return false;
            return Objects.equals(eventId, that.eventId) && Objects.equals(consumerContext, that.consumerContext);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumerContext);
        }
    }
}
