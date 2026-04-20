package pt.orderplatform.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.order.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
