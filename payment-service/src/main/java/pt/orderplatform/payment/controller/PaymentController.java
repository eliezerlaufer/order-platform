package pt.orderplatform.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.orderplatform.payment.dto.PaymentResponse;
import pt.orderplatform.payment.service.PaymentService;

import java.util.UUID;

// =============================================================================
// PAYMENT CONTROLLER — endpoints REST de consulta
// =============================================================================
// A criação de pagamentos é feita automaticamente pelo Kafka listener.
// Este controller expõe apenas leitura (para a frontend/api-gateway consultar).
// =============================================================================
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Consulta de pagamentos da Order Platform")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Consultar pagamento por orderId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Nenhum pagamento para este orderId")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable UUID orderId) {
        log.debug("GET /api/payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}
