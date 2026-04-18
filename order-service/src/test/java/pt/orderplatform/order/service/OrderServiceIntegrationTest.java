package pt.orderplatform.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.orderplatform.order.BaseIntegrationTest;
import pt.orderplatform.order.domain.OrderStatus;
import pt.orderplatform.order.dto.CreateOrderRequest;
import pt.orderplatform.order.dto.OrderItemRequest;
import pt.orderplatform.order.dto.OrderResponse;
import pt.orderplatform.order.exception.OrderCancellationException;
import pt.orderplatform.order.exception.OrderNotFoundException;
import pt.orderplatform.order.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

// =============================================================================
// ORDER SERVICE INTEGRATION TEST
// =============================================================================
// Testa o OrderService contra um PostgreSQL REAL (via Testcontainers).
// O Flyway aplica as migrações antes dos testes — o schema é o mesmo da produção.
//
// ASSERTJ vs JUnit assertions:
//   Em vez de assertEquals(expected, actual) (JUnit),
//   usamos assertThat(actual).isEqualTo(expected) (AssertJ).
//   AssertJ é mais legível e dá mensagens de erro mais claras.
//
// @Nested → agrupa testes relacionados dentro de uma classe.
//   Melhora a legibilidade e permite @BeforeEach por grupo.
//
// @DisplayName → nome legível nos relatórios de testes (em vez do nome do método).
// =============================================================================
class OrderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID customerId;
    private CreateOrderRequest validRequest;

    // -------------------------------------------------------------------------
    // SETUP — corre antes de cada teste
    // -------------------------------------------------------------------------
    // Criamos dados frescos para cada teste para garantir isolamento.
    // -------------------------------------------------------------------------
    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        validRequest = new CreateOrderRequest(List.of(
                new OrderItemRequest(
                        UUID.randomUUID(),     // productId
                        "Teclado Mecânico",    // productName
                        2,                     // quantity
                        new BigDecimal("89.99") // unitPrice
                ),
                new OrderItemRequest(
                        UUID.randomUUID(),
                        "Rato Gaming",
                        1,
                        new BigDecimal("49.99")
                )
        ));
    }

    // =========================================================================
    // CRIAR PEDIDO
    // =========================================================================
    @Nested
    @DisplayName("Criar pedido")
    class CreateOrder {

        @Test
        @DisplayName("deve criar pedido com status PENDING e calcular total correcto")
        void shouldCreateOrderWithPendingStatusAndCorrectTotal() {
            // ACT
            OrderResponse response = orderService.createOrder(customerId, validRequest);

            // ASSERT
            assertThat(response.id()).isNotNull();
            assertThat(response.customerId()).isEqualTo(customerId);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.currency()).isEqualTo("EUR");

            // Total: (2 * 89.99) + (1 * 49.99) = 179.98 + 49.99 = 229.97
            assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("229.97"));
        }

        @Test
        @DisplayName("deve persistir os itens do pedido")
        void shouldPersistOrderItems() {
            OrderResponse response = orderService.createOrder(customerId, validRequest);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items())
                    .extracting("productName")
                    .containsExactlyInAnyOrder("Teclado Mecânico", "Rato Gaming");
        }

        @Test
        @DisplayName("deve criar evento OrderCreated no outbox")
        void shouldCreateOutboxEventOnOrderCreation() {
            // Contar eventos antes
            long beforeCount = outboxEventRepository.count();

            orderService.createOrder(customerId, validRequest);

            // Verificar que foi criado exactamente 1 novo evento
            long afterCount = outboxEventRepository.count();
            assertThat(afterCount).isEqualTo(beforeCount + 1);

            // Verificar o conteúdo do evento
            var pendingEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
            assertThat(pendingEvents).isNotEmpty();

            var event = pendingEvents.get(pendingEvents.size() - 1); // o mais recente
            assertThat(event.getEventType()).isEqualTo("OrderCreated");
            assertThat(event.getAggregateType()).isEqualTo("Order");
            assertThat(event.isPublished()).isFalse();
        }

        @Test
        @DisplayName("payload do outbox deve incluir eventId, orderId e items — contrato consumido pelo inventory-service")
        void outboxPayloadShouldIncludeEventIdAndItems() throws Exception {
            OrderResponse response = orderService.createOrder(customerId, validRequest);

            var pendingEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
            var event = pendingEvents.stream()
                    .filter(e -> e.getAggregateId().equals(response.id()))
                    .findFirst()
                    .orElseThrow();

            JsonNode payload = objectMapper.readTree(event.getPayload());

            // eventId — UUID único para idempotência no consumer
            assertThat(payload.get("eventId")).isNotNull();
            assertThat(UUID.fromString(payload.get("eventId").asText())).isNotNull();

            // orderId
            assertThat(payload.get("orderId").asText()).isEqualTo(response.id().toString());

            // items — necessário para o inventory-service reservar stock
            JsonNode items = payload.get("items");
            assertThat(items).isNotNull();
            assertThat(items.isArray()).isTrue();
            assertThat(items).hasSize(2);
            assertThat(items.get(0).get("productId").asText()).isNotBlank();
            assertThat(items.get(0).get("quantity").asInt()).isPositive();
        }

        @Test
        @DisplayName("deve calcular totalPrice de cada item correctamente")
        void shouldCalculateItemTotalPriceCorrectly() {
            OrderResponse response = orderService.createOrder(customerId, validRequest);

            var teclado = response.items().stream()
                    .filter(i -> i.productName().equals("Teclado Mecânico"))
                    .findFirst().orElseThrow();

            // 2 * 89.99 = 179.98
            assertThat(teclado.totalPrice()).isEqualByComparingTo(new BigDecimal("179.98"));
        }
    }

    // =========================================================================
    // BUSCAR PEDIDO
    // =========================================================================
    @Nested
    @DisplayName("Buscar pedido")
    class GetOrder {

        @Test
        @DisplayName("deve devolver pedido existente com os seus itens")
        void shouldReturnExistingOrderWithItems() {
            OrderResponse created = orderService.createOrder(customerId, validRequest);

            OrderResponse found = orderService.getOrderById(created.id());

            assertThat(found.id()).isEqualTo(created.id());
            assertThat(found.items()).hasSize(2);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException para ID inexistente")
        void shouldThrowNotFoundForUnknownId() {
            UUID unknownId = UUID.randomUUID();

            // assertThatThrownBy → verifica que o código lança uma excepção específica
            assertThatThrownBy(() -> orderService.getOrderById(unknownId))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        @DisplayName("deve devolver lista de pedidos do cliente")
        void shouldReturnOrdersForCustomer() {
            // Criar dois pedidos para o mesmo cliente
            orderService.createOrder(customerId, validRequest);
            orderService.createOrder(customerId, validRequest);

            // Criar um pedido de outro cliente (não deve aparecer)
            orderService.createOrder(UUID.randomUUID(), validRequest);

            List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);

            assertThat(orders).hasSize(2);
            assertThat(orders).allMatch(o -> o.customerId().equals(customerId));
        }
    }

    // =========================================================================
    // CANCELAR PEDIDO
    // =========================================================================
    @Nested
    @DisplayName("Cancelar pedido")
    class CancelOrder {

        @Test
        @DisplayName("deve cancelar pedido PENDING com sucesso")
        void shouldCancelPendingOrder() {
            OrderResponse created = orderService.createOrder(customerId, validRequest);

            OrderResponse cancelled = orderService.cancelOrder(created.id(), customerId);

            assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("deve criar evento OrderCancelled no outbox ao cancelar")
        void shouldCreateOutboxEventOnCancellation() {
            OrderResponse created = orderService.createOrder(customerId, validRequest);
            long eventsBeforeCancel = outboxEventRepository.count();

            orderService.cancelOrder(created.id(), customerId);

            assertThat(outboxEventRepository.count()).isEqualTo(eventsBeforeCancel + 1);
        }

        @Test
        @DisplayName("deve lançar excepção ao cancelar pedido de outro cliente")
        void shouldRejectCancellationByWrongCustomer() {
            OrderResponse created = orderService.createOrder(customerId, validRequest);
            UUID wrongCustomer = UUID.randomUUID();

            assertThatThrownBy(() -> orderService.cancelOrder(created.id(), wrongCustomer))
                    .isInstanceOf(OrderCancellationException.class);
        }

        @Test
        @DisplayName("deve lançar excepção ao cancelar pedido já cancelado")
        void shouldRejectCancellationOfAlreadyCancelledOrder() {
            OrderResponse created = orderService.createOrder(customerId, validRequest);
            orderService.cancelOrder(created.id(), customerId); // primeiro cancelamento

            // segundo cancelamento deve falhar
            assertThatThrownBy(() -> orderService.cancelOrder(created.id(), customerId))
                    .isInstanceOf(OrderCancellationException.class);
        }
    }
}
