package pt.orderplatform.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment_service.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);
}
