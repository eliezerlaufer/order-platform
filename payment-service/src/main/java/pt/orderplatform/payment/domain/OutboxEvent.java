package pt.orderplatform.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// OUTBOX EVENT — Transactional Outbox Pattern
// =============================================================================
// Idêntico em estrutura ao do order-service e inventory-service.
// Cada serviço tem o seu próprio outbox (database-per-service).
// =============================================================================
@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    public void markAsPublished() {
        this.published = true;
        this.publishedAt = OffsetDateTime.now();
    }

    public static OutboxEvent of(String aggregateType, UUID aggregateId,
                                 String eventType, String payload) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .build();
    }
}
