package pt.orderplatform.notification_service.domain;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CANCELLED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    STOCK_UNAVAILABLE
}
