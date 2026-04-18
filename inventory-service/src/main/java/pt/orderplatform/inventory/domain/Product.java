package pt.orderplatform.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pt.orderplatform.inventory.exception.InsufficientStockException;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// ENTIDADE PRODUCT
// =============================================================================
// Mantém o stock de um produto no armazém, decomposto em:
//   available_quantity  → unidades livres para reservar
//   reserved_quantity   → unidades já reservadas mas ainda não confirmadas
//
// Métodos de negócio (rich domain):
//   reserve(qty)  → available -= qty, reserved += qty (exige stock suficiente)
//   release(qty)  → reserved -= qty, available += qty (cancela reserva)
//   confirm(qty)  → reserved -= qty (stock sai definitivamente)
//   restock(qty)  → available += qty (reposição)
//
// @Version habilita optimistic locking; colisões concorrentes dão
// OptimisticLockException, tratada com retry no ReservationService.
// =============================================================================
@Entity
@Table(name = "products")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // =========================================================================
    // FACTORY
    // =========================================================================
    public static Product of(String name, String sku, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("Initial quantity must be >= 0");
        }
        return Product.builder()
                .name(name)
                .sku(sku)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .build();
    }

    // =========================================================================
    // MÉTODOS DE NEGÓCIO
    // =========================================================================

    /** Reserva uma quantidade do stock disponível. */
    public void reserve(int quantity) {
        requirePositive(quantity);
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(id, quantity, availableQuantity);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Liberta uma reserva: devolve ao stock disponível. */
    public void release(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Cannot release " + quantity + " units: only " + reservedQuantity + " reserved");
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Confirma a reserva: stock sai definitivamente (reserved--). */
    public void confirm(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Cannot confirm " + quantity + " units: only " + reservedQuantity + " reserved");
        }
        this.reservedQuantity -= quantity;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Reposição de stock — adiciona unidades ao disponível. */
    public void restock(int quantity) {
        requirePositive(quantity);
        this.availableQuantity += quantity;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
    }
}
