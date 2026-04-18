package pt.orderplatform.payment_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// PAYMENT — entidade raiz do payment-service
// =============================================================================
// Um pagamento por order (order_id UNIQUE). Ciclo de vida:
//   PENDING → PROCESSED (gateway aprovado)
//   PENDING → FAILED    (gateway recusado)
// =============================================================================
@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // =========================================================================
    // FACTORY
    // =========================================================================

    public static Payment pending(UUID orderId, BigDecimal amount) {
        return Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();
    }

    // =========================================================================
    // TRANSIÇÕES DE ESTADO
    // =========================================================================

    public void markProcessed() {
        this.status = PaymentStatus.PROCESSED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = OffsetDateTime.now();
    }
}
