package pt.orderplatform.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.inventory.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
