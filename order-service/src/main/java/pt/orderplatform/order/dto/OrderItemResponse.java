package pt.orderplatform.order.dto;

import pt.orderplatform.order.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

// =============================================================================
// RESPONSE DTO — representa um item numa resposta de pedido
// =============================================================================
// O static factory method "from(OrderItem)" faz o mapeamento Entidade → DTO.
// Mantemos este mapeamento no próprio DTO para simplicidade.
// Em projetos maiores usaria-se MapStruct (gerador de código de mapeamento).
// =============================================================================
public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
