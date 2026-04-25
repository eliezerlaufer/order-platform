package pt.orderplatform.inventory.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.inventory.domain.OutboxEvent;
import pt.orderplatform.inventory.domain.ProcessedEvent;
import pt.orderplatform.inventory.repository.OutboxEventRepository;
import pt.orderplatform.inventory.repository.ProcessedEventRepository;
import pt.orderplatform.inventory.service.ReservationLine;
import pt.orderplatform.inventory.service.ReservationOutcome;
import pt.orderplatform.inventory.service.ReservationService;

import java.math.BigDecimal;
import java.util.*;

// =============================================================================
// ORDER EVENT LISTENER — consumidor Kafka do inventory-service
// =============================================================================
// Consome:
//   orders.order.created     → reservar stock → InventoryReserved | InventoryReservationFailed
//   orders.order.cancelled   → libertar stock → InventoryReleased
//   payments.payment.failed  → libertar stock → InventoryReleased (compensação)
//
// Idempotência: ProcessedEvent table (unique by eventId).
// Ack manual após commit da transação (MANUAL_IMMEDIATE no application.yml).
// =============================================================================
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ReservationService reservationService;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // ORDERS.ORDER.CREATED → reservar stock
    // =========================================================================
    @KafkaListener(topics = "orders.order.created", groupId = "inventory-service")
    @Transactional
    public void onOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId   = UUID.fromString(payload.get("eventId").asText());
        UUID orderId   = UUID.fromString(payload.get("orderId").asText());
        BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on orders.order.created — skipping", eventId, orderId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        List<ReservationLine> lines = new ArrayList<>();
        for (JsonNode item : payload.get("items")) {
            UUID productId = UUID.fromString(item.get("productId").asText());
            int  quantity  = item.get("quantity").asInt();
            lines.add(new ReservationLine(productId, quantity));
        }

        ReservationOutcome outcome = reservationService.reserveForOrder(orderId, lines);

        if (outcome instanceof ReservationOutcome.Success) {
            String outboxPayload = buildReservedPayload(orderId, totalAmount);
            outboxEventRepository.save(OutboxEvent.of("Inventory", orderId, "InventoryReserved", outboxPayload));
            log.info("InventoryReserved outbox event created for order {}", orderId);

        } else if (outcome instanceof ReservationOutcome.Failure failure) {
            String outboxPayload = buildFailedPayload(orderId, failure.reason());
            outboxEventRepository.save(OutboxEvent.of("Inventory", orderId, "InventoryReservationFailed", outboxPayload));
            log.warn("InventoryReservationFailed for order {}: {}", orderId, failure.reason());
        }

        ack.acknowledge();
    }

    // =========================================================================
    // ORDERS.ORDER.CANCELLED + PAYMENTS.PAYMENT.FAILED → libertar stock
    // =========================================================================
    @KafkaListener(topics = {"orders.order.cancelled", "payments.payment.failed"}, groupId = "inventory-service")
    @Transactional
    public void onReleaseStock(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId = UUID.fromString(payload.get("eventId").asText());
        UUID orderId = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} for order {} on {} — skipping", eventId, orderId, record.topic());
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        List<UUID> released = reservationService.releaseForOrder(orderId);

        if (!released.isEmpty()) {
            String outboxPayload = buildReleasedPayload(orderId);
            outboxEventRepository.save(OutboxEvent.of("Inventory", orderId, "InventoryReleased", outboxPayload));
            log.info("InventoryReleased {} product(s) for order {} (topic={})",
                    released.size(), orderId, record.topic());
        }

        ack.acknowledge();
    }

    // =========================================================================
    // PAYLOAD BUILDERS
    // =========================================================================

    private String buildReservedPayload(UUID orderId, BigDecimal totalAmount) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId",     UUID.randomUUID().toString());
            map.put("orderId",     orderId.toString());
            map.put("totalAmount", totalAmount);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize InventoryReserved payload", e);
        }
    }

    private String buildFailedPayload(UUID orderId, String reason) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", UUID.randomUUID().toString());
            map.put("orderId", orderId.toString());
            map.put("reason",  reason);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize InventoryReservationFailed payload", e);
        }
    }

    private String buildReleasedPayload(UUID orderId) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", UUID.randomUUID().toString());
            map.put("orderId", orderId.toString());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize InventoryReleased payload", e);
        }
    }
}
