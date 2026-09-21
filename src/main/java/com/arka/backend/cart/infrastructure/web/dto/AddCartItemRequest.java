package com.arka.backend.cart.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull(message = "El productId es obligatorio")
        UUID productId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity,

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal unitPrice
) {}
