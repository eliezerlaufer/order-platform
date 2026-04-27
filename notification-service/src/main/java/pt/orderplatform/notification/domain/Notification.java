package pt.orderplatform.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

// =============================================================================
// NOTIFICATION — registo de uma notificação enviada ao cliente
// =============================================================================
@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "channel", nullable = false)
    @Builder.Default
    private String channel = "EMAIL";

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static Notification of(UUID orderId, UUID customerId, NotificationType type, String message) {
        return Notification.builder()
                .orderId(orderId)
                .customerId(customerId)
                .type(type)
                .message(message)
                .build();
    }
}
