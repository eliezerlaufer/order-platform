package pt.orderplatform.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// =============================================================================
// REQUEST DTO — corpo do POST /api/orders
// =============================================================================
// Exemplo de JSON que o cliente envia:
// {
//   "items": [
//     {
//       "productId": "550e8400-e29b-41d4-a716-446655440000",
//       "productName": "Teclado Mecânico",
//       "quantity": 2,
//       "unitPrice": 89.99
//     }
//   ]
// }
//
// Nota: customerId NÃO vem no body — é extraído do token JWT.
// Nunca confiar no cliente para dizer quem é. Sempre ler do token.
// =============================================================================
public record CreateOrderRequest(

        // @Valid → activa a validação recursiva dentro dos items (valida cada OrderItemRequest)
        // @NotEmpty → a lista não pode ser null nem vazia
        // @Size → máximo 50 itens por pedido
        @Valid
        @NotNull(message = "Items list is required")
        @NotEmpty(message = "Order must have at least one item")
        @Size(max = 50, message = "Order cannot have more than 50 items")
        List<OrderItemRequest> items
) {}
