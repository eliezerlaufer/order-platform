package pt.orderplatform.inventory.dto;

import pt.orderplatform.inventory.domain.Product;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        int availableQuantity,
        int reservedQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getAvailableQuantity(),
                p.getReservedQuantity(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
