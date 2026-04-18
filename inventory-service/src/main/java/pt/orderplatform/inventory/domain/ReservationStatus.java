package pt.orderplatform.inventory.domain;

// =============================================================================
// RESERVATION STATUS — estado de uma reserva de stock
// =============================================================================
// RESERVED  → stock retirado do disponível, à espera de confirmação
// RELEASED  → reserva cancelada (stock devolvido ao disponível)
// CONFIRMED → pagamento aprovado, stock sai definitivamente do armazém
//
// Corresponde ao tipo enum `reservation_status` no PostgreSQL (V1).
// =============================================================================
public enum ReservationStatus {
    RESERVED,
    RELEASED,
    CONFIRMED
}
