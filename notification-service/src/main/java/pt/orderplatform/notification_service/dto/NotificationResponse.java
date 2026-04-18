package pt.orderplatform.notification_service.dto;

import pt.orderplatform.notification_service.domain.Notification;
import pt.orderplatform.notification_service.domain.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        NotificationType type,
        String channel,
        String message,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getOrderId(),
                n.getCustomerId(),
                n.getType(),
                n.getChannel(),
                n.getMessage(),
                n.getCreatedAt()
        );
    }
}
