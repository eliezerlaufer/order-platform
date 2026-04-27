package pt.orderplatform.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.notification.domain.OrderContext;

import java.util.UUID;

public interface OrderContextRepository extends JpaRepository<OrderContext, UUID> {
}
