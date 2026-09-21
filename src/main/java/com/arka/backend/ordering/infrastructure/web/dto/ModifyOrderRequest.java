package com.arka.backend.ordering.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ModifyOrderRequest(
        @NotEmpty(message = "La orden modificada debe contener al menos un producto")
        @Valid
        List<OrderItemRequest> items
) {}
