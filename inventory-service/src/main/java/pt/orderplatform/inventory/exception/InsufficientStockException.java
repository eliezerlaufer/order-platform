package pt.orderplatform.inventory.exception;

import java.util.UUID;

// =============================================================================
// INSUFFICIENT STOCK — lançada quando se tenta reservar mais do que o disponível
// =============================================================================
// Devolvida como 409 Conflict pelos endpoints REST.
// No fluxo Kafka é capturada e transformada em ReservationOutcome.failure(...).
// =============================================================================
public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Insufficient stock for product " + productId
                + ": requested=" + requested + ", available=" + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public UUID getProductId() { return productId; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
