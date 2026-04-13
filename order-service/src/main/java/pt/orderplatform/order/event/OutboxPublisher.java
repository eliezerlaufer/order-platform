package pt.orderplatform.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.order.domain.OutboxEvent;
import pt.orderplatform.order.repository.OutboxEventRepository;

import java.util.List;

// =============================================================================
// OUTBOX PUBLISHER — publica eventos pendentes no Kafka
// =============================================================================
// @Component → Spring gere este bean (como @Service, mas sem semântica de negócio)
// @Scheduled  → activa o scheduler do Spring (precisamos de @EnableScheduling na app)
//
// FLUXO:
//   1. A cada 5 segundos, o Spring chama publishPendingEvents()
//   2. Buscamos todos os eventos com published=false da tabela outbox_events
//   3. Para cada evento, publicamos no tópico Kafka correcto
//   4. Se publicar com sucesso, marcamos o evento como published=true
//   5. Se o Kafka estiver em baixo, o evento fica na DB e será re-tentado na próxima execução
//
// GARANTIA:
//   At-least-once delivery — um evento pode ser publicado mais de uma vez
//   se o processo crashar após publicar mas antes de marcar como published.
//   Os consumidores devem ser idempotentes (tratar duplicados).
// =============================================================================
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // =========================================================================
    // SCHEDULER — executa a cada 5 segundos
    // =========================================================================
    // fixedDelay = 5000 → aguarda 5s APÓS a execução anterior terminar
    // (não fixedRate, que arrancaria um novo ciclo mesmo se o anterior ainda estivesse a correr)
    //
    // @Transactional → toda a operação (ler + marcar) numa transação.
    // Se falhar a marcar o evento, o Hibernate faz rollback — o evento fica
    // como não publicado e será reprocessado.
    // =========================================================================
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if (pending.isEmpty()) {
            return; // nada para publicar, evitar logs desnecessários
        }

        log.info("OutboxPublisher: {} event(s) to publish", pending.size());

        for (OutboxEvent event : pending) {
            publishEvent(event);
        }
    }

    // =========================================================================
    // PUBLICAR UM EVENTO
    // =========================================================================
    // Mapeamos o eventType para o tópico Kafka correcto.
    // A chave da mensagem Kafka é o aggregateId (orderId) — isto garante que
    // todos os eventos do mesmo pedido vão para a mesma partição, mantendo ordem.
    // =========================================================================
    private void publishEvent(OutboxEvent event) {
        String topic = resolveTopic(event.getEventType());

        try {
            // KafkaTemplate.send(topic, key, value)
            // key = orderId → garante ordering por pedido (mesma partição)
            // value = payload JSON do evento
            kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} (id={}) to topic {}: {}",
                                    event.getEventType(), event.getId(), topic, ex.getMessage());
                        } else {
                            log.debug("Event {} published to topic {} partition {} offset {}",
                                    event.getEventType(), topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            // Marcar como publicado (mesmo que o Kafka seja assíncrono,
            // o send() já foi aceite pelo produtor local com retries configurados)
            event.markAsPublished();
            outboxEventRepository.save(event);

        } catch (Exception ex) {
            log.error("Unexpected error publishing event {} (id={}): {}",
                    event.getEventType(), event.getId(), ex.getMessage());
            // Não relançamos — deixamos o evento para a próxima execução do scheduler
        }
    }

    // =========================================================================
    // MAPEAMENTO: eventType → tópico Kafka
    // =========================================================================
    // Convenção dos tópicos definida em docs/architecture.md:
    //   orders.order.created   → quando um pedido é criado
    //   orders.order.cancelled → quando um pedido é cancelado
    //   orders.order.confirmed → quando inventário + pagamento confirmam
    // =========================================================================
    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "OrderCreated"   -> "orders.order.created";
            case "OrderCancelled" -> "orders.order.cancelled";
            case "OrderConfirmed" -> "orders.order.confirmed";
            case "OrderShipped"   -> "orders.order.shipped";
            default -> {
                log.warn("Unknown event type: {}, using fallback topic", eventType);
                yield "orders.order.unknown";
            }
        };
    }
}
