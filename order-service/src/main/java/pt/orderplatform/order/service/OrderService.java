package pt.orderplatform.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.order.domain.Order;
import pt.orderplatform.order.domain.OrderItem;
import pt.orderplatform.order.domain.OutboxEvent;
import pt.orderplatform.order.dto.CreateOrderRequest;
import pt.orderplatform.order.dto.OrderResponse;
import pt.orderplatform.order.exception.OrderCancellationException;
import pt.orderplatform.order.exception.OrderNotFoundException;
import pt.orderplatform.order.repository.OrderRepository;
import pt.orderplatform.order.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// =============================================================================
// ORDER SERVICE — camada de lógica de negócio
// =============================================================================
// Regra de ouro: o Controller não tem lógica de negócio. Só recebe pedidos
// HTTP e delega para o Service. O Service não sabe nada de HTTP.
//
// @Service → Spring regista esta classe como bean gerido (injetável com @Autowired)
// @Transactional → cada método público é executado dentro de uma transação SQL.
//   Se algo falhar, a transação faz rollback automático.
//   readOnly = true → optimização: o Hibernate não monitoriza mudanças (mais rápido)
//
// @RequiredArgsConstructor (Lombok) → gera construtor com os campos final.
//   Spring Boot injeta automaticamente as dependências pelo construtor.
//   Preferir injeção por construtor em vez de @Autowired nos campos:
//   facilita testes (podemos passar mocks facilmente).
//
// @Slf4j (Lombok) → gera um logger: log.info(), log.error(), log.debug()
// =============================================================================
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // por defeito: operações de leitura (sem escrita)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;  // Jackson: serialização JSON

    // =========================================================================
    // CRIAR PEDIDO
    // =========================================================================
    // @Transactional (sem readOnly) → esta operação escreve na DB
    // O que acontece aqui numa transação atómica:
    //   1. Calcula o total do pedido
    //   2. Cria a entidade Order com os OrderItems
    //   3. Guarda na tabela orders + order_items
    //   4. Cria um evento na tabela outbox_events
    //   5. Commit — tudo junto, ou nada (se falhar, rollback)
    // =========================================================================
    @Transactional
    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request) {
        log.info("Creating order for customer {}", customerId);

        // Calcular total: soma de (quantidade * preço) de cada item
        BigDecimal totalAmount = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Construir a entidade Order (ainda sem ID — o Hibernate gera no save)
        Order order = Order.builder()
                .customerId(customerId)
                .totalAmount(totalAmount)
                .build();

        // Construir os OrderItems e associá-los ao pedido
        // Nota: não precisamos de chamar order.getItems().add(item) explicitamente
        // porque o cascade ALL propaga o save dos items quando guardamos o Order
        List<OrderItem> items = request.items().stream()
                .map(req -> OrderItem.of(order, req.productId(), req.productName(),
                        req.quantity(), req.unitPrice()))
                .toList();

        // Adicionar items ao pedido (necessário para o cascade funcionar)
        order.getItems().addAll(items);

        // Guardar o pedido (o Hibernate guarda também os items por cascade)
        Order savedOrder = orderRepository.save(order);

        // Criar evento no Outbox (mesma transação)
        saveOutboxEvent(savedOrder, "OrderCreated");

        log.info("Order {} created successfully for customer {}", savedOrder.getId(), customerId);
        return OrderResponse.from(savedOrder);
    }

    // =========================================================================
    // BUSCAR PEDIDO POR ID
    // =========================================================================
    // findByIdWithItems → JOIN FETCH para carregar items numa só query (evita N+1)
    // orElseThrow → lança 404 se não existir
    // =========================================================================
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order);
    }

    // =========================================================================
    // LISTAR PEDIDOS DO CLIENTE
    // =========================================================================
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    // =========================================================================
    // BUSCAR PEDIDO POR ID (com verificação de dono)
    // =========================================================================
    public OrderResponse getOrderByIdForCustomer(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(orderId);
        }

        return OrderResponse.from(order);
    }

    // =========================================================================
    // CANCELAR PEDIDO
    // =========================================================================
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verificar que o pedido pertence ao cliente que pede o cancelamento
        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderCancellationException(orderId, order.getStatus());
        }

        // Delegar a lógica de negócio para a entidade (DDD — rich domain model)
        // A entidade valida se o estado permite cancelamento
        try {
            order.cancel();
        } catch (IllegalStateException e) {
            throw new OrderCancellationException(orderId, order.getStatus());
        }

        orderRepository.save(order);
        saveOutboxEvent(order, "OrderCancelled");

        log.info("Order {} cancelled by customer {}", orderId, customerId);
        return OrderResponse.from(order);
    }

    // =========================================================================
    // SAGA — Confirmar pedido (chamado pelo SagaEventListener)
    // =========================================================================
    @Transactional
    public void confirmOrderBySaga(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.confirm();
        orderRepository.save(order);
        saveOutboxEvent(order, "OrderConfirmed");
        log.info("Order {} confirmed by saga", orderId);
    }

    // =========================================================================
    // SAGA — Cancelar pedido (chamado pelo SagaEventListener)
    // =========================================================================
    @Transactional
    public void cancelOrderBySaga(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel();
        orderRepository.save(order);
        saveOutboxEvent(order, "OrderCancelled");
        log.info("Order {} cancelled by saga", orderId);
    }

    // =========================================================================
    // MÉTODO PRIVADO — Guardar evento no Outbox
    // =========================================================================
    // Chamado sempre que algo muda no pedido e precisa de notificar outros serviços.
    // O payload é serializado para JSON aqui mesmo.
    //
    // Campos do payload:
    //   eventId     → UUID único por evento, usado pelos consumidores para
    //                 idempotência (detectar entregas duplicadas do Kafka).
    //   orderId     → ID do pedido (aggregate).
    //   customerId  → dono do pedido (útil para notification-service).
    //   status      → estado do pedido no momento do evento.
    //   totalAmount → valor total (útil para payment-service).
    //   items       → lista de {productId, quantity} — necessário para o
    //                 inventory-service reservar stock.
    // =========================================================================
    private void saveOutboxEvent(Order order, String eventType) {
        try {
            String eventId = UUID.randomUUID().toString();

            // Snapshot dos items em formato serializável — inventory-service consome isto
            List<Map<String, Object>> itemsPayload = order.getItems().stream()
                    .map(item -> Map.<String, Object>of(
                            "productId", item.getProductId().toString(),
                            "quantity", item.getQuantity()
                    ))
                    .toList();

            // LinkedHashMap preserva a ordem dos campos no JSON (legibilidade em logs)
            Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
            payloadMap.put("eventId", eventId);
            payloadMap.put("orderId", order.getId().toString());
            payloadMap.put("customerId", order.getCustomerId().toString());
            payloadMap.put("status", order.getStatus().name());
            payloadMap.put("totalAmount", order.getTotalAmount());
            payloadMap.put("items", itemsPayload);

            String payload = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent event = OutboxEvent.of("Order", order.getId(), eventType, payload);
            outboxEventRepository.save(event);

            log.debug("Outbox event {} (eventId={}) created for order {}",
                    eventType, eventId, order.getId());
        } catch (JsonProcessingException e) {
            // Nunca deve acontecer com tipos simples, mas obrigatório tratar
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
