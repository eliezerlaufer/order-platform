package pt.orderplatform.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.inventory.domain.ReservationStatus;
import pt.orderplatform.inventory.domain.StockReservation;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderId(UUID orderId);

    List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
