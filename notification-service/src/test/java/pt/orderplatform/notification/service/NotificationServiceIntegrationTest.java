package pt.orderplatform.notification.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.notification.BaseIntegrationTest;
import pt.orderplatform.notification.domain.Notification;
import pt.orderplatform.notification.domain.NotificationType;
import pt.orderplatform.notification.repository.NotificationRepository;
import pt.orderplatform.notification.repository.OrderContextRepository;
import pt.orderplatform.notification.repository.ProcessedEventRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// NOTIFICATION SERVICE — integration tests
// =============================================================================
// Tópicos cobertos:
//   orders.order.created        → ORDER_CREATED + OrderContext guardado
//   orders.order.cancelled      → ORDER_CANCELLED
//   payments.payment.processed  → PAYMENT_PROCESSED (resolve via OrderContext)
//   payments.payment.failed     → PAYMENT_FAILED (resolve via OrderContext)
//   inventory.reservation.failed→ STOCK_UNAVAILABLE (resolve via OrderContext)
//   idempotência                → evento duplicado ignorado
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

    // =========================================================================
    // ORDERS.ORDER.CREATED → ORDER_CREATED
    // =========================================================================
    @Nested
    @DisplayName("orders.order.created")
    class OrderCreated {

        @Test
        @DisplayName("deve criar notificação ORDER_CREATED")
        void shouldCreateOrderCreatedNotification() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(),
                    buildOrderCreatedPayload(UUID.randomUUID(), orderId, customerId)));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
                assertThat(notifications).hasSize(1);
                assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ORDER_CREATED);
                assertThat(notifications.get(0).getCustomerId()).isEqualTo(customerId);
            });
        }

        @Test
        @DisplayName("deve guardar OrderContext para eventos futuros sem customerId")
        void shouldPersistOrderContext() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(),
                    buildOrderCreatedPayload(UUID.randomUUID(), orderId, customerId)));

            await().atMost(Duration.ofSeconds(30)).until(() -> orderContextRepository.existsById(orderId));

            var ctx = orderContextRepository.findById(orderId).orElseThrow();
            assertThat(ctx.getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("deve ignorar evento duplicado (idempotência)")
        void shouldBeIdempotentOnDuplicateEvent() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            UUID eventId    = UUID.randomUUID();

            String payload = buildOrderCreatedPayload(eventId, orderId, customerId);
            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));
            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                long count = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId).size();
                assertThat(count).isEqualTo(1);
            });
        }
    }

    // =========================================================================
    // ORDERS.ORDER.CANCELLED → ORDER_CANCELLED
    // =========================================================================
    @Nested
    @DisplayName("orders.order.cancelled")
    class OrderCancelled {

        @Test
        @DisplayName("deve criar notificação ORDER_CANCELLED")
        void shouldCreateOrderCancelledNotification() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "customerId": "%s",
                      "status": "CANCELLED"
                    }
                    """.formatted(UUID.randomUUID(), orderId, customerId);

            kafkaTemplate.send(new ProducerRecord<>("orders.order.cancelled", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
                assertThat(notifications).hasSize(1);
                assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
                assertThat(notifications.get(0).getCustomerId()).isEqualTo(customerId);
            });
        }
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.PROCESSED → PAYMENT_PROCESSED
    // =========================================================================
    @Nested
    @DisplayName("payments.payment.processed")
    class PaymentProcessed {

        @Test
        @DisplayName("deve criar notificação PAYMENT_PROCESSED resolvendo customerId via OrderContext")
        void shouldCreatePaymentProcessedNotification() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(),
                    buildOrderCreatedPayload(UUID.randomUUID(), orderId, customerId)));
            await().atMost(Duration.ofSeconds(20)).until(() -> orderContextRepository.existsById(orderId));

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "amount": "50.00"
                    }
                    """.formatted(UUID.randomUUID(), orderId);
            kafkaTemplate.send(new ProducerRecord<>("payments.payment.processed", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
                assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.PAYMENT_PROCESSED);
            });
        }
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.FAILED → PAYMENT_FAILED
    // =========================================================================
    @Nested
    @DisplayName("payments.payment.failed")
    class PaymentFailed {

        @Test
        @DisplayName("deve criar notificação PAYMENT_FAILED resolvendo customerId via OrderContext")
        void shouldCreatePaymentFailedNotification() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(),
                    buildOrderCreatedPayload(UUID.randomUUID(), orderId, customerId)));
            await().atMost(Duration.ofSeconds(20)).until(() -> orderContextRepository.existsById(orderId));

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), orderId);
            kafkaTemplate.send(new ProducerRecord<>("payments.payment.failed", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
                assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.PAYMENT_FAILED);
            });
        }
    }

    // =========================================================================
    // INVENTORY.RESERVATION.FAILED → STOCK_UNAVAILABLE
    // =========================================================================
    @Nested
    @DisplayName("inventory.reservation.failed")
    class InventoryReservationFailed {

        @Test
        @DisplayName("deve criar notificação STOCK_UNAVAILABLE resolvendo customerId via OrderContext")
        void shouldCreateStockUnavailableNotification() {
            UUID orderId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(),
                    buildOrderCreatedPayload(UUID.randomUUID(), orderId, customerId)));
            await().atMost(Duration.ofSeconds(20)).until(() -> orderContextRepository.existsById(orderId));

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "reason": "Insufficient stock"
                    }
                    """.formatted(UUID.randomUUID(), orderId);
            kafkaTemplate.send(new ProducerRecord<>("inventory.reservation.failed", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
                assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.STOCK_UNAVAILABLE);
            });
        }
    }

    // =========================================================================
    // HELPER
    // =========================================================================
    private String buildOrderCreatedPayload(UUID eventId, UUID orderId, UUID customerId) {
        return """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "PENDING",
                  "totalAmount": "99.90",
                  "items": []
                }
                """.formatted(eventId, orderId, customerId);
    }
}
