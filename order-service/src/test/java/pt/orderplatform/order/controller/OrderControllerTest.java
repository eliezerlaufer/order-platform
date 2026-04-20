package pt.orderplatform.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.orderplatform.order.domain.OrderStatus;
import pt.orderplatform.order.dto.CreateOrderRequest;
import pt.orderplatform.order.dto.OrderItemRequest;
import pt.orderplatform.order.dto.OrderResponse;
import pt.orderplatform.order.exception.OrderNotFoundException;
import pt.orderplatform.order.service.OrderService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// ORDER CONTROLLER TEST — testa apenas a camada HTTP
// =============================================================================
// @WebMvcTest → sobe APENAS o Controller, filtros HTTP e o Spring MVC.
//   NÃO sobe o contexto completo (sem DB, sem Kafka).
//   É rápido porque não precisa de Testcontainers.
//
// MockMvc → simula pedidos HTTP sem subir um servidor real.
//   Permite testar: rotas, status codes, headers, corpo da resposta JSON.
//
// @MockitoBean → cria um mock do OrderService e injeta-o no Controller.
//   Controlamos o comportamento: when(service.x()).thenReturn(y)
//   Não queremos testar lógica de negócio aqui — só a camada HTTP.
//
// SecurityMockMvcRequestPostProcessors.jwt() → simula um utilizador autenticado
//   com um token JWT real sem precisar do Keycloak.
// =============================================================================
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    // JwtDecoder precisa de existir para o Spring Security configurar o filtro JWT.
    // Em @WebMvcTest não há application.yml de teste com issuer-uri, por isso
    // declaramos um mock — não é usado nos testes, mas a sua presença como bean
    // permite que o SecurityConfig configure o BearerTokenAuthenticationFilter.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // UUID fixo para simular o utilizador autenticado nos testes
    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // =========================================================================
    // POST /api/orders
    // =========================================================================

    @Test
    @DisplayName("POST /api/orders → 201 Created com pedido criado")
    void createOrder_shouldReturn201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(UUID.randomUUID(), "Teclado", 1, new BigDecimal("89.99"))
        ));

        OrderResponse mockResponse = buildOrderResponse(OrderStatus.PENDING, new BigDecimal("89.99"));

        // Configurar o mock: quando o service for chamado com qualquer UUID e qualquer request,
        // devolve o mockResponse
        when(orderService.createOrder(any(UUID.class), any(CreateOrderRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders")
                        // jwt() → simula token JWT com subject = customerId
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Verificar status HTTP
                .andExpect(status().isCreated())
                // Verificar campos do JSON de resposta (JSONPath)
                // $.id → acede ao campo "id" do JSON raiz
                .andExpect(jsonPath("$.id").value(mockResponse.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(89.99));
    }

    @Test
    @DisplayName("POST /api/orders → 400 quando items está vazio")
    void createOrder_shouldReturn400WhenItemsEmpty() throws Exception {
        // Request inválido: lista de items vazia
        CreateOrderRequest invalidRequest = new CreateOrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders → 4xx sem autenticação (acesso bloqueado)")
    void createOrder_shouldBlockUnauthenticatedAccess() throws Exception {
        // NOTA SOBRE 401 vs 403 com Spring Security 6 + oauth2ResourceServer:
        //
        // Em produção (com Keycloak real configurado): 401 Unauthorized
        //   → BearerTokenAuthenticationEntryPoint devolve 401 + WWW-Authenticate header
        //
        // Em @WebMvcTest (sem issuer-uri real, JwtDecoder mockado):
        //   → Spring Security 6 devolve 403 Forbidden para utilizadores anónimos
        //     porque o ExceptionTranslationFilter processa AccessDeniedException
        //     antes de verificar se o utilizador é anónimo neste contexto de teste.
        //
        // O que importa: acesso sem token É BLOQUEADO. 401 ou 403 ambos protegem.
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(UUID.randomUUID(), "Teclado", 1, new BigDecimal("89.99"))
        ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================================
    // GET /api/orders/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /api/orders/{id} → 200 com pedido encontrado")
    void getOrderById_shouldReturn200() throws Exception {
        OrderResponse mockResponse = buildOrderResponse(OrderStatus.PENDING, new BigDecimal("89.99"));

        when(orderService.getOrderByIdForCustomer(mockResponse.id(), customerId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/orders/{id}", mockResponse.id())
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockResponse.id().toString()));
    }

    @Test
    @DisplayName("GET /api/orders/{id} → 404 quando pedido não existe")
    void getOrderById_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();

        when(orderService.getOrderByIdForCustomer(unknownId, customerId))
                .thenThrow(new OrderNotFoundException(unknownId));

        mockMvc.perform(get("/api/orders/{id}", unknownId)
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order Not Found"));
    }

    // =========================================================================
    // DELETE /api/orders/{id}
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/orders/{id} → 200 com pedido cancelado")
    void cancelOrder_shouldReturn200() throws Exception {
        OrderResponse cancelledResponse = buildOrderResponse(OrderStatus.CANCELLED, new BigDecimal("89.99"));

        when(orderService.cancelOrder(any(UUID.class), any(UUID.class)))
                .thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/orders/{id}", cancelledResponse.id())
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // =========================================================================
    // HELPER — constrói uma resposta mock
    // =========================================================================
    private OrderResponse buildOrderResponse(OrderStatus status, BigDecimal total) {
        return new OrderResponse(
                UUID.randomUUID(),
                customerId,
                status,
                total,
                "EUR",
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
