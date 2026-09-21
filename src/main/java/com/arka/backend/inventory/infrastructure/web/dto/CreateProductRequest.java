package com.arka.backend.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        String description,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a cero")
        BigDecimal price,

        @NotBlank(message = "La categoría es obligatoria")
        String category,

        Map<String, String> attributes,

        @Min(value = 0, message = "El stock inicial no puede ser negativo")
        int initialStock,

        @Min(value = 0, message = "El umbral mínimo no puede ser negativo")
        int minimumThreshold
) {}
