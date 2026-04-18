package pt.orderplatform.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment_service.domain.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
