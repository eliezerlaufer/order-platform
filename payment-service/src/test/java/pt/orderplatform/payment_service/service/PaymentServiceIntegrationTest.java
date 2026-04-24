package pt.orderplatform.payment_service.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.orderplatform.payment_service.BaseIntegrationTest;
import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.domain.PaymentStatus;
import pt.orderplatform.payment_service.repository.OutboxEventRepository;
import pt.orderplatform.payment_service.repository.PaymentRepository;
import pt.orderplatform.payment_service.repository.ProcessedEventRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// =============================================================================
// PAYMENT SERVICE INTEGRATION TEST
// =============================================================================
// Verifica o fluxo completo: mensagem Kafka → PaymentService → DB + Outbox.
//
// O StripeGateway é mockado via @MockitoBean para eliminar a aleatoriedade:
//   when(stripeGateway.charge(...)).thenReturn(true)  → testa ramo PROCESSED
//   when(stripeGateway.charge(...)).thenReturn(false) → testa ramo FAILED
// =============================================================================
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private StripeGateway stripeGateway;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
        // lenient: este stub pode ser sobrescrito por @BeforeEach de @Nested
        // sem lançar UnnecessaryStubbingException (strict stubs)
        lenient().when(stripeGateway.charge(any(), any())).thenReturn(true);
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
    // PAGAMENTO RECUSADO (StripeGateway → false)
    // =========================================================================
    @Nested
    @DisplayName("Pagamento recusado")
    class PaymentDeclined {

        // Sobrescreve o stub lenient do @BeforeEach externo para todos os testes
        // deste grupo. Corre DEPOIS do setUp() externo, garantindo que o mock
        // retorna false quando o listener Kafka processar a mensagem.
        @BeforeEach
        void configureDeclinedGateway() {
            when(stripeGateway.charge(any(), any())).thenReturn(false);
        }

        @Test
        @DisplayName("deve criar Payment com status FAILED quando gateway recusa")
        void shouldCreateFailedPaymentWhenGatewayDeclines() {
            UUID orderId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(UUID.randomUUID(), orderId, "75.00");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
            });
        }

        @Test
        @DisplayName("deve criar evento PaymentFailed no outbox quando gateway recusa")
        void shouldCreatePaymentFailedOutboxEvent() {
            UUID orderId = UUID.randomUUID();
            String payload = buildInventoryReservedPayload(UUID.randomUUID(), orderId, "75.00");

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("PaymentFailed")
                        && e.getAggregateId().equals(orderId));
            });
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
