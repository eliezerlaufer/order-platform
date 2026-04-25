package pt.orderplatform.order.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// =============================================================================
// ORDER DOMAIN UNIT TEST
// =============================================================================
// Testa os métodos de negócio da entidade Order em isolamento total.
// Sem Spring, sem DB — apenas lógica de domínio pura.
// =============================================================================
class OrderTest {

    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = Order.builder()
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("99.99"))
                .build();
    }

    // =========================================================================
    // confirm()
    // =========================================================================

    @Test
    @DisplayName("confirm() deve transitar PENDING → CONFIRMED")
    void confirm_shouldTransitionFromPendingToConfirmed() {
        pendingOrder.confirm();
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirm() deve lançar IllegalStateException quando já CONFIRMED")
    void confirm_shouldThrowWhenAlreadyConfirmed() {
        pendingOrder.confirm();
        assertThatThrownBy(() -> pendingOrder.confirm())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("confirm() deve lançar IllegalStateException quando CANCELLED")
    void confirm_shouldThrowWhenCancelled() {
        pendingOrder.cancel();
        assertThatThrownBy(() -> pendingOrder.confirm())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    // =========================================================================
    // cancel()
    // =========================================================================

    @Test
    @DisplayName("cancel() deve transitar PENDING → CANCELLED")
    void cancel_shouldTransitionFromPendingToCancelled() {
        pendingOrder.cancel();
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel() deve transitar CONFIRMED → CANCELLED")
    void cancel_shouldTransitionFromConfirmedToCancelled() {
        pendingOrder.confirm();
        pendingOrder.cancel();
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel() deve lançar IllegalStateException quando já CANCELLED")
    void cancel_shouldThrowWhenAlreadyCancelled() {
        pendingOrder.cancel();
        assertThatThrownBy(() -> pendingOrder.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("cancel() deve lançar IllegalStateException quando SHIPPED")
    void cancel_shouldThrowWhenShipped() {
        pendingOrder.confirm();
        pendingOrder.ship();
        assertThatThrownBy(() -> pendingOrder.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHIPPED");
    }

    // =========================================================================
    // ship()
    // =========================================================================

    @Test
    @DisplayName("ship() deve transitar CONFIRMED → SHIPPED")
    void ship_shouldTransitionFromConfirmedToShipped() {
        pendingOrder.confirm();
        pendingOrder.ship();
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("ship() deve lançar IllegalStateException quando PENDING")
    void ship_shouldThrowWhenPending() {
        assertThatThrownBy(() -> pendingOrder.ship())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("ship() deve lançar IllegalStateException quando CANCELLED")
    void ship_shouldThrowWhenCancelled() {
        pendingOrder.cancel();
        assertThatThrownBy(() -> pendingOrder.ship())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }
}
