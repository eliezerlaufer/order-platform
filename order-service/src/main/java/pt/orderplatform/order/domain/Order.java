package pt.orderplatform.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// =============================================================================
// ENTIDADE ORDER
// =============================================================================
// @Entity diz ao Hibernate: "esta classe corresponde a uma tabela na DB"
// @Table(name="orders") diz qual é o nome da tabela (por convenção ou explícito)
//
// Por que Lombok aqui?
//   @Getter        → gera todos os getters automaticamente
//   @Builder       → permite criar objetos: Order.builder().customerId(...).build()
//   @NoArgsConstructor → construtor vazio, obrigatório para o JPA/Hibernate
//   @AllArgsConstructor → construtor com todos os campos, usado pelo @Builder
//
// Não usamos @Data (que inclui @Setter) porque entidades JPA devem ser
// modificadas através de métodos de negócio, não via setters livres.
// Imutabilidade controlada = menos bugs.
// =============================================================================
@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    // -------------------------------------------------------------------------
    // IDENTIDADE
    // -------------------------------------------------------------------------
    // @Id → chave primária
    // @GeneratedValue → o valor é gerado automaticamente
    // UuidGenerator → usa gen_random_uuid() do PostgreSQL (ou gera em Java)
    // -------------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ID do cliente que fez o pedido — vem do token JWT (não é uma FK para outro serviço)
    // Em microserviços, cada serviço é independente: não temos FK para tabelas de outros serviços
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // -------------------------------------------------------------------------
    // ESTADO DO PEDIDO
    // -------------------------------------------------------------------------
    // @Enumerated(EnumType.STRING) → guarda "PENDING", "CONFIRMED", etc. na DB
    // Nunca usar EnumType.ORDINAL (guarda 0, 1, 2...) — frágil se a ordem mudar
    // -------------------------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    // -------------------------------------------------------------------------
    // TIMESTAMPS
    // -------------------------------------------------------------------------
    // OffsetDateTime inclui timezone — preferível a LocalDateTime em sistemas distribuídos
    // @Column(updatable = false) → o created_at nunca é alterado após INSERT
    // -------------------------------------------------------------------------
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // -------------------------------------------------------------------------
    // OPTIMISTIC LOCKING
    // -------------------------------------------------------------------------
    // @Version → Hibernate gere automaticamente este campo
    // Se dois pedidos tentarem modificar o mesmo registo ao mesmo tempo,
    // o segundo vai receber uma OptimisticLockException em vez de sobrescrever.
    // Evita "lost updates" em ambientes com concorrência.
    // -------------------------------------------------------------------------
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // -------------------------------------------------------------------------
    // RELACIONAMENTO COM ORDER ITEMS
    // -------------------------------------------------------------------------
    // @OneToMany → um pedido tem muitos itens
    // mappedBy = "order" → o lado dono da relação é OrderItem.order (a FK fica lá)
    // cascade = ALL → operações (persist, remove) propagam para os itens
    // orphanRemoval = true → se removermos um item da lista, ele é apagado da DB
    // FetchType.LAZY → os itens só são carregados quando acedidos (performance)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // =========================================================================
    // MÉTODOS DE NEGÓCIO
    // =========================================================================
    // Em vez de setters livres, expõe operações com significado de negócio.
    // Isto é Domain-Driven Design (DDD) básico — a entidade protege os seus invariantes.
    // =========================================================================

    // Confirmar o pedido (stock reservado + pagamento aprovado)
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm order in status: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = OffsetDateTime.now();
    }

    // Cancelar o pedido
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in status: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    // Marcar como enviado
    public void ship() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot ship order in status: " + this.status);
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = OffsetDateTime.now();
    }
}
