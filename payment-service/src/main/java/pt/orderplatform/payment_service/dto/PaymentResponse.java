package pt.orderplatform.payment_service.dto;

import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
