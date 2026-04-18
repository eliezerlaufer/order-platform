package pt.orderplatform.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.notification_service.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
