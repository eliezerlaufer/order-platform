package pt.orderplatform.order.exception;

import pt.orderplatform.order.domain.OrderStatus;

import java.util.UUID;

public class OrderCancellationException extends RuntimeException {

    public OrderCancellationException(UUID orderId, OrderStatus currentStatus) {
        super("Cannot cancel order " + orderId + " in status: " + currentStatus);
    }
}
