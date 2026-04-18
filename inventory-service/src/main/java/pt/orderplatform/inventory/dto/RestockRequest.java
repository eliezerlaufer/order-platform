package pt.orderplatform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockRequest(

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be > 0")
        Integer quantity
) {}
