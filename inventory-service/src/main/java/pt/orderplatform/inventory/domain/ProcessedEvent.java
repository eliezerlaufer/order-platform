package pt.orderplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// PROCESSED EVENT — idempotência do consumidor Kafka
// =============================================================================
// Antes de processar um evento, inserimos o seu UUID aqui. Se o INSERT falhar
// com unique-key violation, significa que já foi processado (deduplicação).
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
