package com.arka.backend.ordering.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "El customerId es obligatorio")
        UUID customerId,

        @NotEmpty(message = "La orden debe contener al menos un producto (INV-06)")
        @Valid
        List<OrderItemRequest> items
) {}
