package pt.orderplatform.inventory.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.inventory.domain.Product;
import pt.orderplatform.inventory.domain.ReservationStatus;
import pt.orderplatform.inventory.domain.StockReservation;
import pt.orderplatform.inventory.exception.InsufficientStockException;
import pt.orderplatform.inventory.exception.ProductNotFoundException;
import pt.orderplatform.inventory.repository.ProductRepository;
import pt.orderplatform.inventory.repository.StockReservationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// =============================================================================
// RESERVATION SERVICE — lógica de reserva e libertação de stock
// =============================================================================
// reserveForOrder  : tenta reservar TODAS as linhas all-or-nothing. Se qualquer
//                    linha falhar, a transação é rolled back e retorna Failure.
// releaseForOrder  : idempotente — só liberta o que ainda está em RESERVED.
//                    Se não houver nada por libertar, retorna lista vazia.
//
// Optimistic locking: Product tem @Version. Em colisões o Hibernate lança
// OptimisticLockException; reagimos com retry até 3 vezes (cenário realista
// em concorrência elevada sobre o mesmo SKU).
// =============================================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int MAX_OPTIMISTIC_RETRIES = 3;

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    // =========================================================================
    // RESERVE
    // =========================================================================

    /**
     * Reserva stock para todas as linhas de uma order. All-or-nothing:
     * se qualquer produto não tem stock, nada é reservado.
     *
     * Com retry em colisões optimistic-lock.
     */
    public ReservationOutcome reserveForOrder(UUID orderId, List<ReservationLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Reservation lines must not be empty");
        }

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return reserveForOrderTransactional(orderId, lines);
            } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
                if (attempt >= MAX_OPTIMISTIC_RETRIES) {
                    log.error("Reservation for order {} failed after {} optimistic-lock retries",
                            orderId, attempt);
                    throw ex;
                }
                log.warn("Optimistic lock collision on reservation for order {} (attempt {}/{}), retrying",
                        orderId, attempt, MAX_OPTIMISTIC_RETRIES);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected ReservationOutcome reserveForOrderTransactional(UUID orderId,
                                                              List<ReservationLine> lines) {
        List<UUID> reservationIds = new ArrayList<>(lines.size());

        for (ReservationLine line : lines) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ProductNotFoundException(line.productId()));

            try {
                product.reserve(line.quantity());
            } catch (InsufficientStockException ex) {
                log.info("Insufficient stock for product {} on order {}: requested={}, available={}",
                        line.productId(), orderId, line.quantity(), product.getAvailableQuantity());
                return new ReservationOutcome.Failure(
                        orderId,
                        "Insufficient stock for product " + line.productId(),
                        line.productId());
            }

            productRepository.save(product);

            StockReservation reservation = StockReservation.of(orderId, line.productId(), line.quantity());
            StockReservation saved = stockReservationRepository.save(reservation);
            reservationIds.add(saved.getId());
        }

        log.info("Reserved stock for order {}: {} line(s)", orderId, reservationIds.size());
        return new ReservationOutcome.Success(orderId, reservationIds);
    }

    // =========================================================================
    // RELEASE (COMPENSATION)
    // =========================================================================

    /**
     * Liberta todas as reservas RESERVED de uma order (compensação).
     * Idempotente: se já estiver tudo RELEASED/CONFIRMED, não faz nada.
     *
     * @return lista de productIds libertados (pode ser vazia).
     */
    @Transactional
    public List<UUID> releaseForOrder(UUID orderId) {
        List<StockReservation> activeReservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        if (activeReservations.isEmpty()) {
            log.info("releaseForOrder({}) — no active reservations, idempotent no-op", orderId);
            return List.of();
        }

        List<UUID> releasedProducts = new ArrayList<>(activeReservations.size());

        for (StockReservation reservation : activeReservations) {
            releaseWithRetry(reservation);
            releasedProducts.add(reservation.getProductId());
        }

        log.info("Released {} reservation(s) for order {}", releasedProducts.size(), orderId);
        return releasedProducts;
    }

    private void releaseWithRetry(StockReservation reservation) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                releaseOneTransactional(reservation.getId());
                return;
            } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
                if (attempt >= MAX_OPTIMISTIC_RETRIES) {
                    log.error("Release of reservation {} failed after {} retries",
                            reservation.getId(), attempt);
                    throw ex;
                }
                log.warn("Optimistic lock collision on release of reservation {} (attempt {}/{}), retrying",
                        reservation.getId(), attempt, MAX_OPTIMISTIC_RETRIES);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void releaseOneTransactional(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reservation disappeared mid-release: " + reservationId));

        // Idempotency guard dentro da transação (outra thread pode ter libertado já)
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            return;
        }

        Product product = productRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(reservation.getProductId()));

        product.release(reservation.getQuantity());
        productRepository.save(product);

        reservation.release();
        stockReservationRepository.save(reservation);
    }
}
