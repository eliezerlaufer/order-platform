package pt.orderplatform.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.payment.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
