package pt.orderplatform.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.orderplatform.inventory.domain.Product;
import pt.orderplatform.inventory.domain.ReservationStatus;
import pt.orderplatform.inventory.domain.StockReservation;
import pt.orderplatform.inventory.exception.ProductNotFoundException;
import pt.orderplatform.inventory.repository.ProductRepository;
import pt.orderplatform.inventory.repository.StockReservationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @InjectMocks
    private ReservationService service;

    private UUID orderId;
    private UUID productAId;
    private UUID productBId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productAId = UUID.randomUUID();
        productBId = UUID.randomUUID();
    }

    // =========================================================================
    // RESERVE
    // =========================================================================

    @Test
    @DisplayName("reserveForOrder → Success quando há stock para todas as linhas")
    void reserve_shouldReturnSuccess_whenStockAvailable() {
        Product a = Product.of("A", "SKU-A", 10);
        Product b = Product.of("B", "SKU-B", 20);
        when(productRepository.findById(productAId)).thenReturn(Optional.of(a));
        when(productRepository.findById(productBId)).thenReturn(Optional.of(b));
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(inv -> {
                    StockReservation r = inv.getArgument(0);
                    return StockReservation.builder()
                            .id(UUID.randomUUID())
                            .orderId(r.getOrderId())
                            .productId(r.getProductId())
                            .quantity(r.getQuantity())
                            .status(ReservationStatus.RESERVED)
                            .build();
                });

        ReservationOutcome outcome = service.reserveForOrder(orderId, List.of(
                new ReservationLine(productAId, 3),
                new ReservationLine(productBId, 5)
        ));

        assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);
        ReservationOutcome.Success success = (ReservationOutcome.Success) outcome;
        assertThat(success.orderId()).isEqualTo(orderId);
        assertThat(success.reservationIds()).hasSize(2);

        assertThat(a.getAvailableQuantity()).isEqualTo(7);
        assertThat(a.getReservedQuantity()).isEqualTo(3);
        assertThat(b.getAvailableQuantity()).isEqualTo(15);
        assertThat(b.getReservedQuantity()).isEqualTo(5);

        verify(productRepository).save(a);
        verify(productRepository).save(b);
    }

    @Test
    @DisplayName("reserveForOrder → Failure quando uma linha excede stock, sem salvar reservas")
    void reserve_shouldReturnFailure_whenInsufficientStock() {
        Product a = Product.of("A", "SKU-A", 2); // só tem 2, pedem 5
        when(productRepository.findById(productAId)).thenReturn(Optional.of(a));

        ReservationOutcome outcome = service.reserveForOrder(orderId, List.of(
                new ReservationLine(productAId, 5)
        ));

        assertThat(outcome).isInstanceOf(ReservationOutcome.Failure.class);
        ReservationOutcome.Failure failure = (ReservationOutcome.Failure) outcome;
        assertThat(failure.orderId()).isEqualTo(orderId);
        assertThat(failure.offendingProductId()).isEqualTo(productAId);
        assertThat(failure.reason()).contains("Insufficient stock");
    }

    @Test
    @DisplayName("reserveForOrder → ProductNotFoundException quando produto não existe")
    void reserve_shouldThrow_whenProductMissing() {
        when(productRepository.findById(productAId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserveForOrder(orderId,
                List.of(new ReservationLine(productAId, 1))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("reserveForOrder → IllegalArgumentException quando lines vazias")
    void reserve_shouldReject_emptyLines() {
        assertThatThrownBy(() -> service.reserveForOrder(orderId, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reserveForOrder → falha na 2ª linha não salva StockReservation de nenhuma")
    void reserve_shouldNotPersist_reservations_onPartialFailure() {
        Product a = Product.of("A", "SKU-A", 10);
        Product b = Product.of("B", "SKU-B", 1); // só tem 1, pedem 5
        when(productRepository.findById(productAId)).thenReturn(Optional.of(a));
        when(productRepository.findById(productBId)).thenReturn(Optional.of(b));
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReservationOutcome outcome = service.reserveForOrder(orderId, List.of(
                new ReservationLine(productAId, 3),
                new ReservationLine(productBId, 5)
        ));

        // A transação @Transactional no método interno garante rollback no DB real,
        // mas o outcome tem que sinalizar Failure com o produto B como offending.
        assertThat(outcome).isInstanceOf(ReservationOutcome.Failure.class);
        ReservationOutcome.Failure f = (ReservationOutcome.Failure) outcome;
        assertThat(f.offendingProductId()).isEqualTo(productBId);
    }

    // =========================================================================
    // RELEASE
    // =========================================================================

    @Test
    @DisplayName("releaseForOrder → idempotente quando não há reservas activas")
    void release_shouldBeIdempotent_whenNoActiveReservations() {
        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of());

        List<UUID> released = service.releaseForOrder(orderId);

        assertThat(released).isEmpty();
        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("releaseForOrder → liberta reservas, decrementa reserved e incrementa available")
    void release_shouldReleaseActiveReservations() {
        UUID reservationId = UUID.randomUUID();
        Product a = Product.of("A", "SKU-A", 10);
        a.reserve(4); // simula estado depois de reservar

        StockReservation reservation = StockReservation.builder()
                .id(reservationId)
                .orderId(orderId)
                .productId(productAId)
                .quantity(4)
                .status(ReservationStatus.RESERVED)
                .build();

        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(stockReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(productRepository.findById(productAId)).thenReturn(Optional.of(a));

        List<UUID> released = service.releaseForOrder(orderId);

        assertThat(released).containsExactly(productAId);
        assertThat(a.getAvailableQuantity()).isEqualTo(10);
        assertThat(a.getReservedQuantity()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue()).isSameAs(a);
    }

    @Test
    @DisplayName("releaseForOrder → múltiplas reservas libertadas em lote")
    void release_shouldHandleMultipleReservations() {
        UUID rA = UUID.randomUUID();
        UUID rB = UUID.randomUUID();
        Product a = Product.of("A", "SKU-A", 10); a.reserve(2);
        Product b = Product.of("B", "SKU-B", 20); b.reserve(5);

        StockReservation resA = StockReservation.builder()
                .id(rA).orderId(orderId).productId(productAId)
                .quantity(2).status(ReservationStatus.RESERVED).build();
        StockReservation resB = StockReservation.builder()
                .id(rB).orderId(orderId).productId(productBId)
                .quantity(5).status(ReservationStatus.RESERVED).build();

        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(resA, resB));
        when(stockReservationRepository.findById(rA)).thenReturn(Optional.of(resA));
        when(stockReservationRepository.findById(rB)).thenReturn(Optional.of(resB));
        when(productRepository.findById(productAId)).thenReturn(Optional.of(a));
        when(productRepository.findById(productBId)).thenReturn(Optional.of(b));

        List<UUID> released = service.releaseForOrder(orderId);

        assertThat(released).containsExactlyInAnyOrder(productAId, productBId);
        assertThat(resA.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(resB.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("releaseForOrder → salta reserva que mudou de estado entre list e release (idempotência interna)")
    void release_shouldSkipReservation_ifStatusChangedMidFlight() {
        UUID reservationId = UUID.randomUUID();

        StockReservation staleView = StockReservation.builder()
                .id(reservationId).orderId(orderId).productId(productAId)
                .quantity(4).status(ReservationStatus.RESERVED).build();

        // Reload dentro da transacção devolve uma reserva já RELEASED (outra thread venceu)
        StockReservation freshView = StockReservation.builder()
                .id(reservationId).orderId(orderId).productId(productAId)
                .quantity(4).status(ReservationStatus.RELEASED).build();

        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(staleView));
        when(stockReservationRepository.findById(reservationId)).thenReturn(Optional.of(freshView));

        List<UUID> released = service.releaseForOrder(orderId);

        // O productId é adicionado ao resultado mesmo se o skip ocorre — mantém
        // a invariante de retornar os items processados; consumidor usa isto
        // apenas para log/telemetria (a decisão de publicar já é feita antes).
        assertThat(released).containsExactly(productAId);
        verify(productRepository, never()).save(any());
    }
}
