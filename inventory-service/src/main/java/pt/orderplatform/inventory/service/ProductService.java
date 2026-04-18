package pt.orderplatform.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.inventory.domain.Product;
import pt.orderplatform.inventory.dto.CreateProductRequest;
import pt.orderplatform.inventory.dto.ProductResponse;
import pt.orderplatform.inventory.dto.RestockRequest;
import pt.orderplatform.inventory.exception.ProductNotFoundException;
import pt.orderplatform.inventory.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

// =============================================================================
// PRODUCT SERVICE — CRUD de produtos e reposição de stock
// =============================================================================
// A lógica de reserva/libertação vive em ReservationService (Phase 6).
// Este service só trata do catálogo e da reposição manual.
// =============================================================================
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        log.info("Creating product sku={} initialQuantity={}", request.sku(), request.initialQuantity());
        Product product = Product.of(request.name(), request.sku(), request.initialQuantity());
        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse restock(UUID id, RestockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.restock(request.quantity());
        Product saved = productRepository.save(product);
        log.info("Restocked product {} with {} units (new available={})",
                id, request.quantity(), saved.getAvailableQuantity());
        return ProductResponse.from(saved);
    }
}
