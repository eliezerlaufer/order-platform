package pt.orderplatform.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

// =============================================================================
// ENTIDADE ORDER ITEM
// =============================================================================
// Representa uma linha do pedido: "2x Teclado Mecânico @ 89.99€ = 179.98€"
//
// SNAPSHOT DE DADOS:
// product_name e unit_price são copiados do catálogo no momento do pedido.
// Porquê? Se o preço ou nome mudar no inventory-service, o pedido histórico
// não deve mudar. Esta é a técnica "snapshot" — guarda o estado no momento.
// =============================================================================
@Entity
@Table(name = "order_items")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // -------------------------------------------------------------------------
    // RELACIONAMENTO COM ORDER
    // -------------------------------------------------------------------------
    // @ManyToOne → muitos itens pertencem a um pedido
    // @JoinColumn → define o nome da coluna FK na tabela order_items
    // FetchType.LAZY → não carrega o Order quando carregamos só o item
    // -------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Referência ao produto no inventory-service (apenas o UUID, sem FK)
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Snapshot do nome do produto no momento do pedido
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Preço unitário no momento do pedido (snapshot)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // quantity * unitPrice — calculado e guardado para evitar recalcular sempre
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // =========================================================================
    // FACTORY METHOD
    // =========================================================================
    // Garante que totalPrice é sempre calculado corretamente.
    // Nunca deixar o caller calcular isto — é invariante do domínio.
    // =========================================================================
    public static OrderItem of(Order order, UUID productId, String productName,
                                int quantity, BigDecimal unitPrice) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }
}
