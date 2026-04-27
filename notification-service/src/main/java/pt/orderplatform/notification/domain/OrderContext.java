package pt.orderplatform.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// ORDER CONTEXT — cache local de orderId → customerId
// =============================================================================
// Populado quando orders.order.created é consumido.
// Permite ao notification-service identificar o cliente em eventos posteriores
// (payments.payment.*, inventory.reservation.failed) que não trazem customerId.
// =============================================================================
@Entity
@Table(name = "order_contexts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderContext {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static OrderContext of(UUID orderId, UUID customerId) {
        return OrderContext.builder()
                .orderId(orderId)
                .customerId(customerId)
                .build();
    }
}
