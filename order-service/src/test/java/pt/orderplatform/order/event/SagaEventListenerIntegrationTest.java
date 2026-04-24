package pt.orderplatform.order.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import pt.orderplatform.order.BaseIntegrationTest;
import pt.orderplatform.order.domain.OrderStatus;
import pt.orderplatform.order.dto.CreateOrderRequest;
import pt.orderplatform.order.dto.OrderItemRequest;
import pt.orderplatform.order.dto.OrderResponse;
import pt.orderplatform.order.repository.OrderRepository;
import pt.orderplatform.order.repository.OutboxEventRepository;
import pt.orderplatform.order.repository.ProcessedEventRepository;
import pt.orderplatform.order.service.OrderService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// =============================================================================
// SAGA EVENT LISTENER — integration tests
// =============================================================================
// Cada teste envia uma mensagem Kafka ao tópico correcto e verifica a
// transição de estado do pedido (via OrderRepository).
//
// Usa Awaitility porque o listener é assíncrono: o Kafka entrega a mensagem
// fora da thread do teste, por isso precisamos de polling com timeout.
// =============================================================================
class SagaEventListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private UUID customerId;
    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        customerId = UUID.randomUUID();
        validRequest = new CreateOrderRequest(List.of(
                new OrderItemRequest(UUID.randomUUID(), "Produto Teste", 1, new BigDecimal("50.00"))
        ));
    }

    // =========================================================================
    // INVENTORY.RESERVED → PENDING → CONFIRMED
    // =========================================================================
    @Nested
    @DisplayName("inventory.reserved")
    class InventoryReserved {

        @Test
        @DisplayName("deve confirmar pedido PENDING quando inventory.reserved é recebido")
        void shouldConfirmPendingOrderOnInventoryReserved() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            UUID eventId = UUID.randomUUID();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(eventId, order.id());

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var found = orderRepository.findByIdWithItems(order.id()).orElseThrow();
                assertThat(found.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            });
        }

        @Test
        @DisplayName("deve criar evento OrderConfirmed no outbox após confirmação")
        void shouldCreateOrderConfirmedOutboxEvent() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            // limpar outbox do OrderCreated
            outboxEventRepository.deleteAll();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), order.id());

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("OrderConfirmed")
                        && e.getAggregateId().equals(order.id()));
            });
        }

        @Test
        @DisplayName("deve ignorar evento duplicado (idempotência)")
        void shouldSkipDuplicateInventoryReservedEvent() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            UUID eventId = UUID.randomUUID();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(eventId, order.id());

            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", order.id().toString(), payload));
            // aguardar primeiro processamento
            await().atMost(Duration.ofSeconds(30)).until(() ->
                    orderRepository.findByIdWithItems(order.id())
                            .map(o -> o.getStatus() == OrderStatus.CONFIRMED)
                            .orElse(false));

            // enviar duplicado — não deve criar outro ProcessedEvent
            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                long count = processedEventRepository.count();
                // apenas 1 ProcessedEvent para este eventId
                assertThat(count).isEqualTo(1);
            });
        }
    }

    // =========================================================================
    // INVENTORY.RESERVATION.FAILED → PENDING → CANCELLED
    // =========================================================================
    @Nested
    @DisplayName("inventory.reservation.failed")
    class InventoryReservationFailed {

        @Test
        @DisplayName("deve cancelar pedido PENDING quando inventory.reservation.failed é recebido")
        void shouldCancelPendingOrderOnReservationFailed() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "reason": "Insufficient stock"
                    }
                    """.formatted(UUID.randomUUID(), order.id());

            kafkaTemplate.send(new ProducerRecord<>("inventory.reservation.failed", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var found = orderRepository.findByIdWithItems(order.id()).orElseThrow();
                assertThat(found.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            });
        }

        @Test
        @DisplayName("deve criar evento OrderCancelled no outbox")
        void shouldCreateOrderCancelledOutboxEvent() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            outboxEventRepository.deleteAll();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "reason": "Insufficient stock"
                    }
                    """.formatted(UUID.randomUUID(), order.id());

            kafkaTemplate.send(new ProducerRecord<>("inventory.reservation.failed", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var events = outboxEventRepository.findAll();
                assertThat(events).anyMatch(e -> e.getEventType().equals("OrderCancelled")
                        && e.getAggregateId().equals(order.id()));
            });
        }
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.PROCESSED → log apenas, sem mudança de estado
    // =========================================================================
    @Nested
    @DisplayName("payments.payment.processed")
    class PaymentProcessed {

        @Test
        @DisplayName("deve guardar ProcessedEvent e não alterar estado do pedido")
        void shouldSaveProcessedEventAndNotChangeOrderStatus() {
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            UUID eventId = UUID.randomUUID();
            processedEventRepository.deleteAll();

            String payload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s",
                      "amount": "50.00"
                    }
                    """.formatted(eventId, order.id());

            kafkaTemplate.send(new ProducerRecord<>("payments.payment.processed", order.id().toString(), payload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
                // estado não muda — permanece PENDING (order-service confirma via inventory.reserved)
                var found = orderRepository.findByIdWithItems(order.id()).orElseThrow();
                assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
            });
        }
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.FAILED → CONFIRMED → CANCELLED
    // =========================================================================
    @Nested
    @DisplayName("payments.payment.failed")
    class PaymentFailed {

        @Test
        @DisplayName("deve cancelar pedido quando payments.payment.failed é recebido")
        void shouldCancelOrderOnPaymentFailed() {
            // Criar e confirmar o pedido via inventory.reserved
            OrderResponse order = orderService.createOrder(customerId, validRequest);
            String reservedPayload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), order.id());
            kafkaTemplate.send(new ProducerRecord<>("inventory.reserved", order.id().toString(), reservedPayload));
            await().atMost(Duration.ofSeconds(30)).until(() ->
                    orderRepository.findByIdWithItems(order.id())
                            .map(o -> o.getStatus() == OrderStatus.CONFIRMED)
                            .orElse(false));

            // Agora enviar payments.payment.failed
            String failedPayload = """
                    {
                      "eventId": "%s",
                      "orderId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), order.id());

            kafkaTemplate.send(new ProducerRecord<>("payments.payment.failed", order.id().toString(), failedPayload));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var found = orderRepository.findByIdWithItems(order.id()).orElseThrow();
                assertThat(found.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            });
        }
    }
}
