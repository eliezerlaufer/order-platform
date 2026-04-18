package pt.orderplatform.inventory.service;

import java.util.UUID;

// =============================================================================
// RESERVATION LINE — item individual a reservar (productId + quantity)
// =============================================================================
// Input para ReservationService.reserveForOrder. Corresponde a uma linha da
// order recebida via evento orders.order.created.
// =============================================================================
public record ReservationLine(UUID productId, int quantity) {

    public ReservationLine {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
    }
}
