package pt.orderplatform.notification_service.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.notification_service.BaseIntegrationTest;
import pt.orderplatform.notification_service.domain.NotificationType;
import pt.orderplatform.notification_service.domain.Notification;
import pt.orderplatform.notification_service.repository.NotificationRepository;
import pt.orderplatform.notification_service.repository.OrderContextRepository;
import pt.orderplatform.notification_service.repository.ProcessedEventRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// NOTIFICATION SERVICE INTEGRATION TEST
// =============================================================================
class NotificationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired NotificationRepository notificationRepository;
    @Autowired OrderContextRepository orderContextRepository;
    @Autowired ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        orderContextRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldCreateNotificationOnOrderCreated() {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId    = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "PENDING",
                  "totalAmount": "99.90",
                  "items": []
                }
                """.formatted(eventId, orderId, customerId);

        kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ORDER_CREATED);
            assertThat(notifications.get(0).getCustomerId()).isEqualTo(customerId);
        });
    }

    @Test
    void shouldResolveCustomerFromContextForPaymentProcessed() {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        // Primeiro: popular OrderContext via orders.order.created
        UUID createEventId = UUID.randomUUID();
        String orderPayload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "PENDING",
                  "totalAmount": "50.00",
                  "items": []
                }
                """.formatted(createEventId, orderId, customerId);
        kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), orderPayload));

        // Aguardar que o OrderContext seja gravado
        await().atMost(Duration.ofSeconds(20)).until(
                () -> orderContextRepository.existsById(orderId));

        // Depois: enviar payments.payment.processed
        UUID paymentEventId = UUID.randomUUID();
        String paymentPayload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "amount": "50.00"
                }
                """.formatted(paymentEventId, orderId);
        kafkaTemplate.send(new ProducerRecord<>("payments.payment.processed", orderId.toString(), paymentPayload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
            assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.PAYMENT_PROCESSED);
        });
    }

    @Test
    void shouldBeIdempotentOnDuplicateEvent() {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId    = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "PENDING",
                  "totalAmount": "30.00",
                  "items": []
                }
                """.formatted(eventId, orderId, customerId);

        kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));
        kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long count = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId).size();
            assertThat(count).isEqualTo(1);
        });
    }
}
