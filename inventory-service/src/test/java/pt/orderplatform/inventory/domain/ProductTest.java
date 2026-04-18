package pt.orderplatform.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pt.orderplatform.inventory.exception.InsufficientStockException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// =============================================================================
// PRODUCT — unit tests do modelo de domínio (sem Spring, sem DB)
// =============================================================================
class ProductTest {

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("reserva decrementa o disponível e incrementa o reservado")
        void reservesUnitsFromAvailable() {
            Product p = Product.of("Teclado", "KBD-001", 10);

            p.reserve(3);

            assertThat(p.getAvailableQuantity()).isEqualTo(7);
            assertThat(p.getReservedQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("reservar mais do que disponível lança InsufficientStockException")
        void throwsWhenInsufficientStock() {
            Product p = Product.of("Teclado", "KBD-001", 2);

            assertThatThrownBy(() -> p.reserve(5))
                    .isInstanceOf(InsufficientStockException.class);

            // Estado não é alterado após falha
            assertThat(p.getAvailableQuantity()).isEqualTo(2);
            assertThat(p.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("reservar quantidade zero ou negativa é inválido")
        void throwsOnNonPositiveQuantity() {
            Product p = Product.of("Teclado", "KBD-001", 10);

            assertThatThrownBy(() -> p.reserve(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> p.reserve(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("release devolve unidades ao disponível")
        void releaseReturnsUnits() {
            Product p = Product.of("Teclado", "KBD-001", 10);
            p.reserve(4);

            p.release(4);

            assertThat(p.getAvailableQuantity()).isEqualTo(10);
            assertThat(p.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("release de mais do que reservado lança IllegalStateException")
        void throwsWhenReleasingMoreThanReserved() {
            Product p = Product.of("Teclado", "KBD-001", 10);
            p.reserve(2);

            assertThatThrownBy(() -> p.release(5))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("confirm retira unidades de reserved (stock sai do armazém)")
        void confirmRemovesFromReserved() {
            Product p = Product.of("Teclado", "KBD-001", 10);
            p.reserve(3);

            p.confirm(3);

            assertThat(p.getReservedQuantity()).isZero();
            // Available NÃO muda — o stock foi consumido, não devolvido
            assertThat(p.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("confirm de mais do que reservado lança IllegalStateException")
        void throwsWhenConfirmingMoreThanReserved() {
            Product p = Product.of("Teclado", "KBD-001", 10);
            p.reserve(2);

            assertThatThrownBy(() -> p.confirm(3))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("restock")
    class Restock {

        @Test
        @DisplayName("restock aumenta apenas o disponível")
        void restockIncreasesAvailable() {
            Product p = Product.of("Teclado", "KBD-001", 5);
            p.reserve(2);

            p.restock(10);

            assertThat(p.getAvailableQuantity()).isEqualTo(13); // 5 - 2 + 10
            assertThat(p.getReservedQuantity()).isEqualTo(2);   // inalterado
        }

        @Test
        @DisplayName("restock com quantidade não positiva é inválido")
        void throwsOnNonPositiveRestock() {
            Product p = Product.of("Teclado", "KBD-001", 5);

            assertThatThrownBy(() -> p.restock(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("factory rejeita quantidade inicial negativa")
    void factoryRejectsNegativeInitialQuantity() {
        assertThatThrownBy(() -> Product.of("X", "SKU", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
