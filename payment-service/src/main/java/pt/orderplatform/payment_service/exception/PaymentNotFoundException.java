package pt.orderplatform.payment_service.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID orderId) {
        super("No payment found for orderId: " + orderId);
    }
}
