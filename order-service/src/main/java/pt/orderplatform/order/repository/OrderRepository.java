package pt.orderplatform.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.orderplatform.order.domain.Order;
import pt.orderplatform.order.domain.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// =============================================================================
// ORDER REPOSITORY
// =============================================================================
// JpaRepository<Order, UUID> dá-nos gratuitamente:
//   save(), findById(), findAll(), delete(), count(), existsById(), ...
//
// Spring Data JPA gera as queries automaticamente a partir do nome do método:
//   findByCustomerId → SELECT * FROM orders WHERE customer_id = ?
//   findByStatus     → SELECT * FROM orders WHERE status = ?
//
// Quando a query é mais complexa, usamos @Query com JPQL (Java Persistence Query Language)
// JPQL é como SQL mas usa nomes de classes/campos Java, não tabelas/colunas SQL.
//   "FROM Order o WHERE o.customerId = :customerId" → Hibernate traduz para SQL
// =============================================================================
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Buscar todos os pedidos de um cliente
    // Spring Data gera: SELECT * FROM orders WHERE customer_id = ?
    List<Order> findByCustomerId(UUID customerId);

    // Buscar pedidos por estado (ex: listar todos os PENDING para processar)
    List<Order> findByStatus(OrderStatus status);

    // Buscar pedidos de um cliente com um estado específico
    List<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    // -------------------------------------------------------------------------
    // FETCH JOIN — carrega Order + Items numa só query
    // -------------------------------------------------------------------------
    // Problema sem isto (N+1 Query Problem):
    //   1 query para buscar o Order
    //   N queries para buscar cada OrderItem (uma por item)
    //   → Para 100 pedidos = 101 queries! Lento.
    //
    // Com JOIN FETCH:
    //   1 query que faz JOIN entre orders e order_items
    //   → Sempre 1 query, independente do número de itens
    //
    // Usar DISTINCT porque o JOIN pode duplicar o Order se tiver vários itens
    // -------------------------------------------------------------------------
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);
}
