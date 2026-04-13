package pt.orderplatform.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pt.orderplatform.order.dto.CreateOrderRequest;
import pt.orderplatform.order.dto.OrderResponse;
import pt.orderplatform.order.service.OrderService;

import java.util.List;
import java.util.UUID;

// =============================================================================
// ORDER CONTROLLER — camada HTTP
// =============================================================================
// @Tag → nome do grupo no Swagger UI (agrupa endpoints relacionados)
// @Operation → descreve um endpoint específico
// @ApiResponses → documenta os possíveis códigos de resposta HTTP
// =============================================================================
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestão do ciclo de vida dos pedidos")
public class OrderController {

    private final OrderService orderService;

    // =========================================================================
    // POST /api/orders — Criar pedido
    // =========================================================================
    @Operation(
        summary = "Criar novo pedido",
        description = "Cria um pedido com um ou mais itens. O cliente é identificado pelo token JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (validação falhou)"),
        @ApiResponse(responseCode = "401", description = "Token JWT em falta ou inválido")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = extractCustomerId(jwt);
        OrderResponse response = orderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/orders — Listar pedidos do cliente autenticado
    // =========================================================================
    @Operation(summary = "Listar os meus pedidos", description = "Devolve todos os pedidos do cliente autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pedidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT em falta ou inválido")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = extractCustomerId(jwt);
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    // =========================================================================
    // GET /api/orders/{id} — Buscar pedido por ID
    // =========================================================================
    @Operation(summary = "Buscar pedido por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "401", description = "Token JWT em falta ou inválido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // =========================================================================
    // DELETE /api/orders/{id} — Cancelar pedido
    // =========================================================================
    @Operation(
        summary = "Cancelar pedido",
        description = "Cancela o pedido se ainda estiver em estado PENDING ou CONFIRMED. Pedidos SHIPPED ou DELIVERED não podem ser cancelados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pedido não pode ser cancelado no estado actual"),
        @ApiResponse(responseCode = "401", description = "Token JWT em falta ou inválido")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = extractCustomerId(jwt);
        return ResponseEntity.ok(orderService.cancelOrder(id, customerId));
    }

    private UUID extractCustomerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
