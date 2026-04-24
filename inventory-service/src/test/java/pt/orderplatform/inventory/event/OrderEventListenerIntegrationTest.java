package pt.orderplatform.inventory.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.inventory.BaseIntegrationTest;
import pt.orderplatform.inventory.domain.Product;
import pt.orderplatform.inventory.domain.ReservationStatus;
import pt.orderplatform.inventory.domain.StockReservation;
import pt.orderplatform.inventory.repository.OutboxEventRepository;
import pt.orderplatform.inventory.repository.ProcessedEventRepository;
import pt.orderplatform.inventory.repository.ProductRepository;
import pt.orderplatform.inventory.repository.StockReservationRepository;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// ORDER EVENT LISTENER — integration tests
// =============================================================================
// Verifica o fluxo completo: mensagem Kafka → OrderEventListener → DB + Outbox.
//
// Cenários cobertos:
//   orders.order.created + stock disponível   → outbox InventoryReserved
//   orders.order.created + stock insuficiente → outbox InventoryReservationFailed
//   orders.order.cancelled                    → outbox InventoryReleased
//   payments.payment.failed                   → outbox InventoryReleased
//   idempotência                              → evento duplicado ignorado
// =============================================================================
class OrderEventListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        stockReservationRepository.deleteAll();
        productRepository.deleteAll();
    }

    // =========================================================================
    // ORDERS.ORDER.CREATED — reserva bem-sucedida
    // =========================================================================
    @Nested
    @DisplayName("orders.order.created — stock disponível")
    class OrderCreatedWithStock {

        @Test
        @DisplayName("deve criar outbox InventoryReserved quando há stock")
        void shouldCreateInventoryReservedOutboxEvent() {
            Product product = productRepository.save(Product.of("Teclado", "SKU-001", 10));
            UUID orderId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();

            String payload = buildOrderCreatedPayload(eventId, orderId, product.getId(), 3, "100.00");

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("InventoryReserved")
                        && e.getAggregateId().equals(orderId));
            });
        }

        @Test
        @DisplayName("deve decrementar availableQuantity do produto após reserva")
        void shouldDecrementProductStock() {
            Product product = productRepository.save(Product.of("Rato", "SKU-002", 5));
            UUID orderId = UUID.randomUUID();

            String payload = buildOrderCreatedPayload(UUID.randomUUID(), orderId, product.getId(), 2, "50.00");

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                Product updated = productRepository.findById(product.getId()).orElseThrow();
                assertThat(updated.getAvailableQuantity()).isEqualTo(3);
                assertThat(updated.getReservedQuantity()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("deve ignorar evento duplicado (idempotência)")
        void shouldSkipDuplicateOrderCreatedEvent() {
            Product product = productRepository.save(Product.of("Monitor", "SKU-003", 10));
            UUID orderId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();

            String payload = buildOrderCreatedPayload(eventId, orderId, product.getId(), 1, "200.00");

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));
            await().atMost(Duration.ofSeconds(30)).until(() ->
                    outboxEventRepository.findAll().stream()
                            .anyMatch(e -> e.getEventType().equals("InventoryReserved")));

            // Enviar duplicado
            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                // Apenas 1 ProcessedEvent para este eventId
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
                // Apenas 1 reserva para este orderId
                var reservations = stockReservationRepository.findAll();
                assertThat(reservations.stream().filter(r -> r.getOrderId().equals(orderId))).hasSize(1);
            });
        }
    }

    // =========================================================================
    // ORDERS.ORDER.CREATED — stock insuficiente
    // =========================================================================
    @Nested
    @DisplayName("orders.order.created — stock insuficiente")
    class OrderCreatedWithoutStock {

        @Test
        @DisplayName("deve criar outbox InventoryReservationFailed quando stock é insuficiente")
        void shouldCreateInventoryReservationFailedOutboxEvent() {
            Product product = productRepository.save(Product.of("Cadeira", "SKU-004", 2));
            UUID orderId = UUID.randomUUID();

            // pede 5, só tem 2
            String payload = buildOrderCreatedPayload(UUID.randomUUID(), orderId, product.getId(), 5, "500.00");

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("InventoryReservationFailed")
                        && e.getAggregateId().equals(orderId));
            });
        }

        @Test
        @DisplayName("não deve alterar o stock do produto quando reserva falha")
        void shouldNotChangeStockOnReservationFailure() {
            Product product = productRepository.save(Product.of("Mesa", "SKU-005", 1));
            UUID orderId = UUID.randomUUID();

            String payload = buildOrderCreatedPayload(UUID.randomUUID(), orderId, product.getId(), 10, "1000.00");

            kafkaTemplate.send(new ProducerRecord<>("orders.order.created", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).until(() ->
                    outboxEventRepository.findAll().stream()
                            .anyMatch(e -> e.getEventType().equals("InventoryReservationFailed")));

            Product unchanged = productRepository.findById(product.getId()).orElseThrow();
            assertThat(unchanged.getAvailableQuantity()).isEqualTo(1);
            assertThat(unchanged.getReservedQuantity()).isEqualTo(0);
        }
    }

    // =========================================================================
    // ORDERS.ORDER.CANCELLED → libertar stock
    // =========================================================================
    @Nested
    @DisplayName("orders.order.cancelled")
    class OrderCancelled {

        @Test
        @DisplayName("deve criar outbox InventoryReleased quando pedido é cancelado")
        void shouldCreateInventoryReleasedOnOrderCancelled() {
            // Setup: criar produto e reserva directamente na DB
            Product product = productRepository.save(Product.of("Teclado Pro", "SKU-006", 10));
            product.reserve(3);
            productRepository.save(product);

            UUID orderId = UUID.randomUUID();
            StockReservation reservation = StockReservation.of(orderId, product.getId(), 3);
            stockReservationRepository.save(reservation);

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "customerId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), orderId, UUID.randomUUID());

            kafkaTemplate.send(new ProducerRecord<>("orders.order.cancelled", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("InventoryReleased")
                        && e.getAggregateId().equals(orderId));
            });
        }

        @Test
        @DisplayName("deve libertar stock (incrementar available, zerar reserved)")
        void shouldRestoreStockOnOrderCancelled() {
            Product product = productRepository.save(Product.of("Rato Pro", "SKU-007", 10));
            product.reserve(4);
            productRepository.save(product);

            UUID orderId = UUID.randomUUID();
            stockReservationRepository.save(StockReservation.of(orderId, product.getId(), 4));

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "customerId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), orderId, UUID.randomUUID());

            kafkaTemplate.send(new ProducerRecord<>("orders.order.cancelled", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                Product updated = productRepository.findById(product.getId()).orElseThrow();
                assertThat(updated.getAvailableQuantity()).isEqualTo(10);
                assertThat(updated.getReservedQuantity()).isEqualTo(0);
            });
        }
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.FAILED → libertar stock (compensação)
    // =========================================================================
    @Nested
    @DisplayName("payments.payment.failed")
    class PaymentFailed {

        @Test
        @DisplayName("deve criar outbox InventoryReleased quando pagamento falha")
        void shouldReleaseStockOnPaymentFailed() {
            Product product = productRepository.save(Product.of("Headset", "SKU-008", 10));
            product.reserve(2);
            productRepository.save(product);

            UUID orderId = UUID.randomUUID();
            stockReservationRepository.save(StockReservation.of(orderId, product.getId(), 2));

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), orderId);

            kafkaTemplate.send(new ProducerRecord<>("payments.payment.failed", orderId.toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("InventoryReleased")
                        && e.getAggregateId().equals(orderId));
            });
        }
    }

    // =========================================================================
    // HELPER — constrói payload orders.order.created
    // =========================================================================
    private String buildOrderCreatedPayload(UUID eventId, UUID orderId, UUID productId,
                                             int quantity, String totalAmount) {
        return """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "PENDING",
                  "totalAmount": "%s",
                  "items": [
                    { "productId": "%s", "quantity": %d }
                  ]
                }
                """.formatted(eventId, orderId, UUID.randomUUID(), totalAmount, productId, quantity);
    }
}
