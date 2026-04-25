package pt.orderplatform.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.order.domain.ProcessedEvent;
import pt.orderplatform.order.repository.ProcessedEventRepository;
import pt.orderplatform.order.service.OrderService;

import java.util.UUID;

// =============================================================================
// SAGA EVENT LISTENER — fecha o loop da saga choreography
// =============================================================================
// Consome:
//   inventory.reserved             → confirmar order (PENDING → CONFIRMED)
//   inventory.reservation.failed   → cancelar order (PENDING → CANCELLED)
//   payments.payment.processed     → log apenas (order já CONFIRMED)
//   payments.payment.failed        → cancelar order (CONFIRMED → CANCELLED)
//
// Idempotência: ProcessedEvent table (unique by eventId).
// Ack manual após commit da transação (MANUAL_IMMEDIATE no application.yml).
// =============================================================================
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // INVENTORY.RESERVED → confirmar order
    // =========================================================================
    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    @Transactional
    public void onInventoryReserved(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId = UUID.fromString(payload.get("eventId").asText());
        UUID orderId = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on inventory.reserved — skipping", eventId, orderId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        try {
            orderService.confirmOrderBySaga(orderId);
        } catch (IllegalStateException ex) {
            // Order já transitou para estado terminal — ack sem retry
            log.warn("Cannot confirm order (state conflict): {}", ex.getMessage());
        }
        ack.acknowledge();
    }

    // =========================================================================
    // INVENTORY.RESERVATION.FAILED → cancelar order
    // =========================================================================
    @KafkaListener(topics = "inventory.reservation.failed", groupId = "order-service")
    @Transactional
    public void onInventoryReservationFailed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId = UUID.fromString(payload.get("eventId").asText());
        UUID orderId = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on inventory.reservation.failed — skipping", eventId, orderId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        try {
            orderService.cancelOrderBySaga(orderId);
        } catch (IllegalStateException ex) {
            log.warn("Cannot cancel order (state conflict): {}", ex.getMessage());
        }
        ack.acknowledge();
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.PROCESSED → log apenas (order já CONFIRMED)
    // =========================================================================
    @KafkaListener(topics = "payments.payment.processed", groupId = "order-service")
    @Transactional
    public void onPaymentProcessed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId = UUID.fromString(payload.get("eventId").asText());
        UUID orderId = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on payments.payment.processed — skipping", eventId, orderId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        log.info("Payment processed for order {} — order already CONFIRMED, no state change needed", orderId);
        ack.acknowledge();
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.FAILED → cancelar order
    // =========================================================================
    @KafkaListener(topics = "payments.payment.failed", groupId = "order-service")
    @Transactional
    public void onPaymentFailed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId = UUID.fromString(payload.get("eventId").asText());
        UUID orderId = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on payments.payment.failed — skipping", eventId, orderId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        try {
            orderService.cancelOrderBySaga(orderId);
        } catch (IllegalStateException ex) {
            log.warn("Cannot cancel order after payment failure (state conflict): {}", ex.getMessage());
        }
        ack.acknowledge();
    }
}
