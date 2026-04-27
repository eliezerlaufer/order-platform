package pt.orderplatform.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);
}
