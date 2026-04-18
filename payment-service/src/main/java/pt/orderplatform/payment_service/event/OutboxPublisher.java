package pt.orderplatform.payment_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.payment_service.domain.OutboxEvent;
import pt.orderplatform.payment_service.repository.OutboxEventRepository;

import java.util.List;

// =============================================================================
// OUTBOX PUBLISHER — publica eventos pendentes do payment-service no Kafka
// =============================================================================
// Mapeamento de eventTypes:
//   PaymentProcessed → payments.payment.processed
//   PaymentFailed    → payments.payment.failed
// =============================================================================
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if (pending.isEmpty()) {
            return;
        }

        log.info("OutboxPublisher: {} event(s) to publish", pending.size());

        for (OutboxEvent event : pending) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {
        String topic = resolveTopic(event.getEventType());

        try {
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

            event.markAsPublished();
            outboxEventRepository.save(event);

        } catch (Exception ex) {
            log.error("Unexpected error publishing event {} (id={}): {}",
                    event.getEventType(), event.getId(), ex.getMessage());
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "PaymentProcessed" -> "payments.payment.processed";
            case "PaymentFailed"    -> "payments.payment.failed";
            default -> {
                log.warn("Unknown event type: {}, using fallback topic", eventType);
                yield "payments.unknown";
            }
        };
    }
}
