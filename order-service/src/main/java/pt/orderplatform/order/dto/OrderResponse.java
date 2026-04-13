package pt.orderplatform.order.dto;

import pt.orderplatform.order.domain.Order;
import pt.orderplatform.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// =============================================================================
// RESPONSE DTO — o que a API devolve ao cliente
// =============================================================================
// Exemplo de JSON devolvido:
// {
//   "id": "550e8400-...",
//   "customerId": "...",
//   "status": "PENDING",
//   "totalAmount": 179.98,
//   "currency": "EUR",
//   "items": [...],
//   "createdAt": "2026-04-11T10:00:00+01:00"
// }
// =============================================================================
public record OrderResponse(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    // Mapeamento Entidade → DTO
    // Chamado pelo Service antes de devolver ao Controller
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
