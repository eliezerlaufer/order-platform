package pt.orderplatform.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment_service.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
