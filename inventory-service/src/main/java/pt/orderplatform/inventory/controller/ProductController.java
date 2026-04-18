package pt.orderplatform.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.orderplatform.inventory.dto.CreateProductRequest;
import pt.orderplatform.inventory.dto.ProductResponse;
import pt.orderplatform.inventory.dto.RestockRequest;
import pt.orderplatform.inventory.service.ProductService;

import java.util.List;
import java.util.UUID;

// =============================================================================
// PRODUCT CONTROLLER — endpoints REST administrativos
// =============================================================================
// Usado para seeding de stock em dev/teste e para reposição manual.
// A reserva/libertação é feita pelos consumers Kafka, não por este controller.
// =============================================================================
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gestão do catálogo e do stock disponível")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Criar produto no catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "SKU já existe")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @Operation(summary = "Listar todos os produtos")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.findAll());
    }

    @Operation(summary = "Buscar produto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Operation(summary = "Repor stock disponível de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reposto"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @PatchMapping("/{id}/restock")
    public ResponseEntity<ProductResponse> restock(
            @PathVariable UUID id,
            @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(productService.restock(id, request));
    }
}
