package pt.orderplatform.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.notification_service.domain.OrderContext;

import java.util.UUID;

public interface OrderContextRepository extends JpaRepository<OrderContext, UUID> {
}
