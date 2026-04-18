package pt.orderplatform.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// ENTIDADE STOCK RESERVATION
// =============================================================================
// Uma linha por (order, product). Armazena a quantidade reservada e o estado.
// Transições permitidas:
//   RESERVED → RELEASED  (compensação por cancelamento ou falha de pagamento)
//   RESERVED → CONFIRMED (pagamento aprovado — stock sai definitivamente)
// =============================================================================
@Entity
@Table(name = "stock_reservations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // @JdbcTypeCode(NAMED_ENUM) → diz ao Hibernate para mapear para o enum
    // nativo do Postgres (reservation_status) em vez de VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "reservation_status")
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // =========================================================================
    // FACTORY
    // =========================================================================
    public static StockReservation of(UUID orderId, UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
        return StockReservation.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .build();
    }

    // =========================================================================
    // TRANSIÇÕES DE ESTADO
    // =========================================================================

    /** Liberta a reserva (compensação). Só é válido a partir de RESERVED. */
    public void release() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot release reservation in status: " + status);
        }
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Confirma a reserva (pagamento aprovado). */
    public void confirm() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot confirm reservation in status: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = OffsetDateTime.now();
    }
}
