package pt.orderplatform.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.orderplatform.inventory.domain.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}
