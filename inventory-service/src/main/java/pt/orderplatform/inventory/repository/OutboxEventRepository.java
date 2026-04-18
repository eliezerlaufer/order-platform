package pt.orderplatform.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.inventory.domain.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
