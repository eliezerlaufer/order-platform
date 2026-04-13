package pt.orderplatform.order.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

// =============================================================================
// DTO — Data Transfer Object
// =============================================================================
// DTOs são objetos simples que representam dados que entram ou saem da API.
// NUNCA expõe entidades JPA diretamente na API REST. Porquê?
//
//  1. Segurança: a entidade pode ter campos sensíveis que não devem sair na API
//  2. Evolução: podes mudar a entidade sem quebrar o contrato da API
//  3. Validação: as anotações @Valid/@NotNull ficam no DTO, não na entidade
//  4. Serialização: evita problemas de lazy loading do Hibernate em JSON
//
// Usamos Java Records (Java 16+) para DTOs:
//   - Imutáveis por definição (sem setters)
//   - Construtores, getters, equals, hashCode, toString gerados automaticamente
//   - Sintaxe muito mais limpa que classes normais
// =============================================================================
public record OrderItemRequest(

        // @NotNull → campo obrigatório; erro se vier null no JSON
        @NotNull(message = "Product ID is required")
        UUID productId,

        // @NotBlank → não pode ser null nem string vazia/só espaços
        @NotBlank(message = "Product name is required")
        String productName,

        // @Min(1) → quantidade mínima de 1
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        // @DecimalMin → preço tem de ser positivo
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        BigDecimal unitPrice
) {}
