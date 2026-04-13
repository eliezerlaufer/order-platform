package pt.orderplatform.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.order.domain.OutboxEvent;

import java.util.List;
import java.util.UUID;

// =============================================================================
// OUTBOX EVENT REPOSITORY
// =============================================================================
// O OutboxPublisher vai usar findByPublishedFalse() para buscar eventos
// pendentes e publicá-los no Kafka.
//
// Spring Data interpreta o nome do método:
//   findByPublishedFalse → WHERE published = false
//
// Limitamos a 100 por execução para não sobrecarregar o Kafka de uma vez
// se houver muitos eventos acumulados (ex: após downtime do Kafka).
// =============================================================================
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Buscar eventos ainda não publicados (o OutboxPublisher processa estes)
    // Spring Data: findBy + Published + False → WHERE published = false
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
