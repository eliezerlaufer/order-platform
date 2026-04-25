package pt.orderplatform.order.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.orderplatform.order.BaseIntegrationTest;
import pt.orderplatform.order.domain.OutboxEvent;
import pt.orderplatform.order.repository.OutboxEventRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

// =============================================================================
// OUTBOX PUBLISHER INTEGRATION TEST
// =============================================================================
// Verifica que o OutboxPublisher:
//   1. Lê eventos pendentes da DB e os publica no Kafka
//   2. Marca eventos como published=true após publicar
//   3. Não faz nada quando não há eventos pendentes
//
// NÃO usa @Nested para evitar múltiplos Spring contexts.
// =============================================================================
class OutboxPublisherIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("deve marcar eventos como publicados após publishPendingEvents()")
    void shouldMarkEventsAsPublishedAfterPublishing() {
        UUID orderId = UUID.randomUUID();
        String payload = buildPayload(orderId);
        outboxEventRepository.save(OutboxEvent.of("Order", orderId, "OrderCreated", payload));

        outboxPublisher.publishPendingEvents();

        var events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).isPublished()).isTrue();
        assertThat(events.get(0).getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("não deve lançar excepção quando não há eventos pendentes")
    void shouldDoNothingWhenNoPendingEvents() {
        assertThatNoException().isThrownBy(() -> outboxPublisher.publishPendingEvents());
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("deve publicar múltiplos eventos — OrderCreated, OrderCancelled, OrderConfirmed")
    void shouldPublishAllEventTypes() {
        UUID orderId = UUID.randomUUID();
        String payload = buildPayload(orderId);

        outboxEventRepository.save(OutboxEvent.of("Order", orderId, "OrderCreated", payload));
        outboxEventRepository.save(OutboxEvent.of("Order", orderId, "OrderCancelled", payload));
        outboxEventRepository.save(OutboxEvent.of("Order", orderId, "OrderConfirmed", payload));

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc()).isEmpty();
        assertThat(outboxEventRepository.findAll()).allMatch(OutboxEvent::isPublished);
    }

    @Test
    @DisplayName("deve publicar evento com tipo desconhecido para tópico fallback")
    void shouldPublishUnknownEventTypeToFallbackTopic() {
        UUID orderId = UUID.randomUUID();
        outboxEventRepository.save(OutboxEvent.of("Order", orderId, "OrderShipped", buildPayload(orderId)));

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc()).isEmpty();
    }

    @Test
    @DisplayName("não deve re-publicar eventos já marcados como published")
    void shouldNotRepublishAlreadyPublishedEvents() {
        UUID orderId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.of("Order", orderId, "OrderCreated", buildPayload(orderId));
        event.markAsPublished();
        outboxEventRepository.save(event);

        // Não há pendentes — publishPendingEvents não deve processar este evento
        outboxPublisher.publishPendingEvents();

        // O evento continua publicado (não foi alterado)
        var saved = outboxEventRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isPublished()).isTrue();
    }

    // =========================================================================
    // HELPER
    // =========================================================================
    private String buildPayload(UUID orderId) {
        return """
                {"eventId":"%s","orderId":"%s","customerId":"%s","status":"PENDING","totalAmount":99.99,"items":[]}
                """.formatted(UUID.randomUUID(), orderId, UUID.randomUUID()).strip();
    }
}
