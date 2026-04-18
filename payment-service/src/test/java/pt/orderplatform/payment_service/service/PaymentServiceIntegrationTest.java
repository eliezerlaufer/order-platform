package pt.orderplatform.payment_service.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.payment_service.BaseIntegrationTest;
import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.domain.PaymentStatus;
import pt.orderplatform.payment_service.repository.PaymentRepository;
import pt.orderplatform.payment_service.repository.ProcessedEventRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// PAYMENT SERVICE INTEGRATION TEST
// =============================================================================
// Verifica o fluxo completo: mensagem Kafka → PaymentService → DB.
// O StripeGateway mock pode aprovar ou recusar; testamos ambos os casos
// verificando que o Payment fica em PROCESSED ou FAILED.
// =============================================================================
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldCreatePaymentWhenInventoryReservedReceived() {
        UUID orderId  = UUID.randomUUID();
        UUID eventId  = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "totalAmount": "99.90"
                }
                """.formatted(eventId, orderId);

        kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
            assertThat(payment).isPresent();
            assertThat(payment.get().getStatus())
                    .isIn(PaymentStatus.PROCESSED, PaymentStatus.FAILED);
        });
    }

    @Test
    void shouldBeIdempotentWhenSameEventReceivedTwice() {
        UUID orderId  = UUID.randomUUID();
        UUID eventId  = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "totalAmount": "50.00"
                }
                """.formatted(eventId, orderId);

        kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));
        kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", orderId.toString(), payload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long count = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .count();
            assertThat(count).isEqualTo(1);
        });
    }
}
