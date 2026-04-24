package pt.orderplatform.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.orderplatform.inventory.BaseIntegrationTest;
import pt.orderplatform.inventory.dto.CreateProductRequest;
import pt.orderplatform.inventory.dto.ProductResponse;
import pt.orderplatform.inventory.dto.RestockRequest;
import pt.orderplatform.inventory.exception.ProductNotFoundException;
import pt.orderplatform.inventory.repository.ProductRepository;
import pt.orderplatform.inventory.repository.StockReservationRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// =============================================================================
// INVENTORY SERVICE (ProductService) — integration tests
// =============================================================================
// Testa o ProductService contra PostgreSQL real via Testcontainers.
// Cobre: criação, listagem, busca por ID, reposição de stock e erros.
// =============================================================================
class InventoryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @BeforeEach
    void cleanUp() {
        stockReservationRepository.deleteAll();
        productRepository.deleteAll();
    }

    // =========================================================================
    // CRIAR PRODUTO
    // =========================================================================
    @Nested
    @DisplayName("Criar produto")
    class CreateProduct {

        @Test
        @DisplayName("deve criar produto com stock inicial correcto")
        void shouldCreateProductWithInitialStock() {
            CreateProductRequest request = new CreateProductRequest("Teclado Mecânico", "SKU-001", 100);

            ProductResponse response = productService.create(request);

            assertThat(response.id()).isNotNull();
            assertThat(response.name()).isEqualTo("Teclado Mecânico");
            assertThat(response.sku()).isEqualTo("SKU-001");
            assertThat(response.availableQuantity()).isEqualTo(100);
            assertThat(response.reservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("deve persistir produto na base de dados")
        void shouldPersistProduct() {
            CreateProductRequest request = new CreateProductRequest("Monitor 4K", "SKU-002", 20);
            ProductResponse created = productService.create(request);

            assertThat(productRepository.findById(created.id())).isPresent();
        }
    }

    // =========================================================================
    // LISTAR PRODUTOS
    // =========================================================================
    @Nested
    @DisplayName("Listar produtos")
    class FindAll {

        @Test
        @DisplayName("deve devolver todos os produtos existentes")
        void shouldReturnAllProducts() {
            productService.create(new CreateProductRequest("Produto A", "SKU-A", 10));
            productService.create(new CreateProductRequest("Produto B", "SKU-B", 20));

            List<ProductResponse> products = productService.findAll();

            assertThat(products).hasSize(2);
            assertThat(products).extracting("sku")
                    .containsExactlyInAnyOrder("SKU-A", "SKU-B");
        }

        @Test
        @DisplayName("deve devolver lista vazia quando não há produtos")
        void shouldReturnEmptyListWhenNoProducts() {
            List<ProductResponse> products = productService.findAll();
            assertThat(products).isEmpty();
        }
    }

    // =========================================================================
    // BUSCAR POR ID
    // =========================================================================
    @Nested
    @DisplayName("Buscar produto por ID")
    class FindById {

        @Test
        @DisplayName("deve devolver produto existente")
        void shouldReturnExistingProduct() {
            ProductResponse created = productService.create(
                    new CreateProductRequest("Rato Gaming", "SKU-003", 50));

            ProductResponse found = productService.findById(created.id());

            assertThat(found.id()).isEqualTo(created.id());
            assertThat(found.sku()).isEqualTo("SKU-003");
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException para ID desconhecido")
        void shouldThrowNotFoundForUnknownId() {
            UUID unknownId = UUID.randomUUID();

            assertThatThrownBy(() -> productService.findById(unknownId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    // =========================================================================
    // REPOSIÇÃO DE STOCK
    // =========================================================================
    @Nested
    @DisplayName("Reposição de stock")
    class Restock {

        @Test
        @DisplayName("deve aumentar availableQuantity após reposição")
        void shouldIncreaseAvailableQuantityOnRestock() {
            ProductResponse created = productService.create(
                    new CreateProductRequest("Cadeira Ergonómica", "SKU-004", 5));

            ProductResponse restocked = productService.restock(created.id(), new RestockRequest(10));

            assertThat(restocked.availableQuantity()).isEqualTo(15);
            assertThat(restocked.reservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao repor stock de produto inexistente")
        void shouldThrowNotFoundWhenRestockingUnknownProduct() {
            assertThatThrownBy(() -> productService.restock(UUID.randomUUID(), new RestockRequest(10)))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
