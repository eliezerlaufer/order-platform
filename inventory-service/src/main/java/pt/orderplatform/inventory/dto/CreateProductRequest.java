package pt.orderplatform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// =============================================================================
// CREATE PRODUCT REQUEST — payload de criação de produto
// =============================================================================
public record CreateProductRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "sku is required")
        String sku,

        @NotNull(message = "initialQuantity is required")
        @Min(value = 0, message = "initialQuantity must be >= 0")
        Integer initialQuantity
) {}
