package pt.orderplatform.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.notification.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
