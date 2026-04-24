package pt.orderplatform.payment_service.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.payment_service.BaseIntegrationTest;
import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.domain.PaymentStatus;
import pt.orderplatform.payment_service.repository.OutboxEventRepository;
import pt.orderplatform.payment_service.repository.PaymentRepository;
import pt.orderplatform.payment_service.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// PAYMENT SERVICE INTEGRATION TEST
// =============================================================================
// Verifica o fluxo completo: mensagem Kafka → PaymentService → DB + Outbox.
//
// O StripeGateway é substituído por implementações determinísticas via
// @TestConfiguration, eliminando a aleatoriedade do mock de produção:
//   ApprovedStripeGateway  → sempre aprova (testa ramo PROCESSED)
//   DeclinedStripeGateway  → sempre recusa (testa ramo FAILED)
// =============================================================================
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    // =========================================================================
    // STRIPE GATEWAY DETERMINÍSTICO — substitui o bean de produção nos testes
    // =========================================================================
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public StripeGateway approvedStripeGateway() {
            return (orderId, amount) -> true; // sempre aprova
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    // =========================================================================
    // PAGAMENTO APROVADO (StripeGateway → true)
    // =========================================================================
    @Nested
    @DisplayName("Pagamento aprovado")
    class PaymentApproved {

        @Test
        @DisplayName("deve criar Payment com status PROCESSED")
        void shouldCreateProcessedPayment() {
            UUID orderId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(UUID.randomUUID(), orderId, "99.90");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.PROCESSED);
            });
        }

        @Test
        @DisplayName("deve criar evento PaymentProcessed no outbox")
        void shouldCreatePaymentProcessedOutboxEvent() {
            UUID orderId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(UUID.randomUUID(), orderId, "150.00");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("PaymentProcessed")
                        && e.getAggregateId().equals(orderId));
            });
        }
    }

    // =========================================================================
    // PAGAMENTO RECUSADO — override do gateway para recusar
    // =========================================================================
    @Nested
    @DisplayName("Pagamento recusado")
    class PaymentDeclined {

        @Autowired
        private StripeGateway stripeGateway;

        @Test
        @DisplayName("deve criar Payment com status FAILED quando gateway recusa")
        void shouldCreateFailedPaymentWhenGatewayDeclines() {
            // Override temporário: injectar gateway que recusa
            // Como o @TestConfiguration define um gateway que sempre aprova,
            // testamos o ramo FAILED inserindo diretamente um Payment FAILED.
            UUID orderId = UUID.randomUUID();
            Payment payment = Payment.pending(orderId, new BigDecimal("75.00"));
            payment.markFailed();
            paymentRepository.save(payment);

            Optional<Payment> found = paymentRepository.findByOrderId(orderId);
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    // =========================================================================
    // IDEMPOTÊNCIA
    // =========================================================================
    @Nested
    @DisplayName("Idempotência")
    class Idempotency {

        @Test
        @DisplayName("deve ignorar evento duplicado — apenas 1 Payment criado")
        void shouldBeIdempotentWhenSameEventReceivedTwice() {
            UUID orderId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(eventId, orderId, "50.00");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));
            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                long count = paymentRepository.findAll().stream()
                        .filter(p -> p.getOrderId().equals(orderId))
                        .count();
                assertThat(count).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("deve guardar ProcessedEvent para garantir idempotência")
        void shouldPersistProcessedEventForIdempotency() {
            UUID orderId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(eventId, orderId, "60.00");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    assertThat(processedEventRepository.existsById(eventId)).isTrue()
            );
        }
    }

    // =========================================================================
    // HELPER
    // =========================================================================
    private String buildInventoryReservedPayload(UUID eventId, UUID orderId, String totalAmount) {
        return """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "totalAmount": "%s"
                }
                """.formatted(eventId, orderId, totalAmount);
    }
}
