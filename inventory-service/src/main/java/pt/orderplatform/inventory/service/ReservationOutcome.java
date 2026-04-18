package pt.orderplatform.inventory.service;

import java.util.List;
import java.util.UUID;

// =============================================================================
// RESERVATION OUTCOME — resultado sealed de tentar reservar stock para uma order
// =============================================================================
// Success  → todas as linhas reservadas atomicamente; traz os reservationIds
// Failure  → pelo menos uma linha não tinha stock suficiente; traz o motivo
//
// Sendo sealed, qualquer consumidor (listener/publisher) é forçado pelo compilador
// a lidar com os dois casos — evita esquecer de publicar InventoryReservationFailed.
// =============================================================================
public sealed interface ReservationOutcome
        permits ReservationOutcome.Success, ReservationOutcome.Failure {

    UUID orderId();

    record Success(UUID orderId, List<UUID> reservationIds) implements ReservationOutcome {}

    record Failure(UUID orderId, String reason, UUID offendingProductId) implements ReservationOutcome {}
}
