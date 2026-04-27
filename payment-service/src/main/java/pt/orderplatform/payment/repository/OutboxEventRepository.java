package pt.orderplatform.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment.domain.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
