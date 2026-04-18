package pt.orderplatform.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.notification_service.domain.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
