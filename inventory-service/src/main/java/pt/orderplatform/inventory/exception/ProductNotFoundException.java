package pt.orderplatform.inventory.exception;

import java.util.UUID;

// =============================================================================
// PRODUCT NOT FOUND — produto inexistente no catálogo
// =============================================================================
// 404 nos endpoints REST; nos consumers Kafka é transformada em
// ReservationOutcome.failure(PRODUCT_NOT_FOUND, productId).
// =============================================================================
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId);
    }

    public ProductNotFoundException(String sku) {
        super("Product not found with SKU: " + sku);
    }
}
