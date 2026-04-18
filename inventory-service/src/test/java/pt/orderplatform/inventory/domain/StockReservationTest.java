package pt.orderplatform.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    @Test
    @DisplayName("factory cria reserva com status RESERVED")
    void factoryCreatesReservedStatus() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(r.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("factory rejeita quantidade <= 0")
    void factoryRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("release transita de RESERVED para RELEASED")
    void releaseFromReserved() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        r.release();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("release falha se já RELEASED")
    void releaseTwiceThrows() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        r.release();
        assertThatThrownBy(r::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("confirm transita de RESERVED para CONFIRMED")
    void confirmFromReserved() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        r.confirm();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirm falha se já CONFIRMED")
    void confirmTwiceThrows() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        r.confirm();
        assertThatThrownBy(r::confirm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("não é possível confirmar uma reserva já libertada")
    void cannotConfirmReleased() {
        StockReservation r = StockReservation.of(UUID.randomUUID(), UUID.randomUUID(), 3);
        r.release();
        assertThatThrownBy(r::confirm).isInstanceOf(IllegalStateException.class);
    }
}
