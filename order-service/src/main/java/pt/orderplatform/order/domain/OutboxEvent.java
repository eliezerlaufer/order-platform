package pt.orderplatform.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// ENTIDADE OUTBOX EVENT — Padrão Transactional Outbox
// =============================================================================
// PROBLEMA QUE RESOLVE:
//   Imagina que temos de fazer duas coisas ao mesmo tempo:
//     1. Guardar o pedido na base de dados (transação SQL)
//     2. Publicar evento no Kafka
//
//   Se publicarmos no Kafka dentro da transação SQL e o Kafka falhar,
//   o pedido fica guardado mas o evento nunca chegou aos outros serviços.
//   Se publicarmos APÓS commit e a app crashar, mesmo problema.
//
// SOLUÇÃO (Outbox Pattern):
//   Em vez de publicar no Kafka diretamente, guardamos o evento na tabela
//   outbox_events NA MESMA TRANSAÇÃO que guarda o pedido.
//   Um processo separado (OutboxPublisher) lê esta tabela e publica no Kafka.
//
//   Transação: [INSERT orders] + [INSERT outbox_events] → commit atómico
//   Depois:    OutboxPublisher lê outbox_events → publica no Kafka → marca published=true
//
//   Resultado: nunca perdemos eventos. Se o Kafka estiver em baixo, os eventos
//   ficam guardados na DB e são publicados quando o Kafka voltar.
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

    // Que tipo de agregado gerou este evento? Ex: "Order"
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    // UUID do agregado (ID do pedido)
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    // Tipo do evento: "OrderCreated", "OrderCancelled", etc.
    // Este valor vai tornar-se o tipo de mensagem no Kafka
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Conteúdo do evento em JSON (serializado)
    // @JdbcTypeCode(SqlTypes.JSON) → Hibernate envia como tipo JSON nativo do PostgreSQL
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Quando foi publicado no Kafka (null = ainda não publicado)
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    // Flag para o OutboxPublisher: false = pendente, true = já publicado
    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    // =========================================================================
    // MÉTODOS DE NEGÓCIO
    // =========================================================================

    // Chamado pelo OutboxPublisher após publicar com sucesso no Kafka
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = OffsetDateTime.now();
    }

    // Factory method — cria um evento de outbox de forma consistente
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
