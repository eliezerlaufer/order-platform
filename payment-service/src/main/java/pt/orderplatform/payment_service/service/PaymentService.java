package pt.orderplatform.payment_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.payment_service.domain.OutboxEvent;
import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.domain.ProcessedEvent;
import pt.orderplatform.payment_service.repository.OutboxEventRepository;
import pt.orderplatform.payment_service.repository.PaymentRepository;
import pt.orderplatform.payment_service.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// =============================================================================
// PAYMENT SERVICE — lógica de negócio + consumidor Kafka
// =============================================================================
// Consome: inventory.reserved
// Fluxo:
//   1. Parse do payload: orderId + totalAmount
//   2. Idempotência via ProcessedEvent
//   3. Cria Payment (PENDING) na DB
//   4. Chama StripeGateway.charge()
//   5. Atualiza status do Payment (PROCESSED | FAILED)
//   6. Publica outbox event (PaymentProcessed | PaymentFailed)
// =============================================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final StripeGateway stripeGateway;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // KAFKA LISTENER — inventory.reserved
    // =========================================================================

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service")
    @Transactional
    public void onInventoryReserved(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload    = objectMapper.readTree(record.value());
            UUID eventId        = UUID.fromString(payload.get("eventId").asText());
            UUID orderId        = UUID.fromString(payload.get("orderId").asText());
            BigDecimal amount   = new BigDecimal(payload.get("totalAmount").asText());

            if (processedEventRepository.existsById(eventId)) {
                log.info("Duplicate event {} for order {} on inventory.reserved — skipping", eventId, orderId);
                ack.acknowledge();
                return;
            }
            processedEventRepository.save(ProcessedEvent.of(eventId));

            Payment payment = Payment.pending(orderId, amount);
            paymentRepository.save(payment);
            log.info("Payment PENDING created for order {} amount={}", orderId, amount);

            boolean approved = stripeGateway.charge(orderId, amount);

            if (approved) {
                payment.markProcessed();
                paymentRepository.save(payment);
                String outboxPayload = buildProcessedPayload(orderId, amount);
                outboxEventRepository.save(OutboxEvent.of("Payment", orderId, "PaymentProcessed", outboxPayload));
                log.info("Payment PROCESSED for order {}", orderId);
            } else {
                payment.markFailed();
                paymentRepository.save(payment);
                String outboxPayload = buildFailedPayload(orderId);
                outboxEventRepository.save(OutboxEvent.of("Payment", orderId, "PaymentFailed", outboxPayload));
                log.warn("Payment FAILED for order {}", orderId);
            }

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error processing inventory.reserved (offset={}): {}", record.offset(), ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // QUERY — usado pelo controller
    // =========================================================================

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public pt.orderplatform.payment_service.dto.PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new pt.orderplatform.payment_service.exception.PaymentNotFoundException(orderId));
        return pt.orderplatform.payment_service.dto.PaymentResponse.from(payment);
    }

    // =========================================================================
    // PAYLOAD BUILDERS
    // =========================================================================

    private String buildProcessedPayload(UUID orderId, BigDecimal amount) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", UUID.randomUUID().toString());
            map.put("orderId", orderId.toString());
            map.put("amount",  amount);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize PaymentProcessed payload", e);
        }
    }

    private String buildFailedPayload(UUID orderId) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", UUID.randomUUID().toString());
            map.put("orderId", orderId.toString());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize PaymentFailed payload", e);
        }
    }
}
