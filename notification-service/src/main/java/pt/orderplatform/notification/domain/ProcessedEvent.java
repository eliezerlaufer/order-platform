package pt.orderplatform.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// PROCESSED EVENT — idempotência do consumidor Kafka
// =============================================================================
@Entity
@Table(name = "processed_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "consumed_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime consumedAt = OffsetDateTime.now();

    public static ProcessedEvent of(UUID eventId) {
        return ProcessedEvent.builder()
                .eventId(eventId)
                .consumedAt(OffsetDateTime.now())
                .build();
    }
}
