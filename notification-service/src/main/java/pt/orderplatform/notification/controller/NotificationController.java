package pt.orderplatform.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.orderplatform.notification.dto.NotificationResponse;
import pt.orderplatform.notification.service.NotificationService;

import java.util.List;
import java.util.UUID;

// =============================================================================
// NOTIFICATION CONTROLLER — endpoints REST de consulta
// =============================================================================
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Consulta de notificações da Order Platform")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Listar notificações por orderId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificações encontradas"),
            @ApiResponse(responseCode = "404", description = "Sem notificações para este orderId")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationResponse>> getByOrderId(@PathVariable UUID orderId) {
        log.debug("GET /api/notifications/order/{}", orderId);
        return ResponseEntity.ok(notificationService.getByOrderId(orderId));
    }

    @Operation(summary = "Listar notificações por customerId")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationResponse>> getByCustomerId(@PathVariable UUID customerId) {
        log.debug("GET /api/notifications/customer/{}", customerId);
        return ResponseEntity.ok(notificationService.getByCustomerId(customerId));
    }
}
