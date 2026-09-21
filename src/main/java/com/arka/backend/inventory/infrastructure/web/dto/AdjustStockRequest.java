package com.arka.backend.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdjustStockRequest(
        @Min(value = 0, message = "El stock físico no puede ser negativo")
        int newStock,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        String reason
) {}
