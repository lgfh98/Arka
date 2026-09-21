package com.arka.backend.ordering.infrastructure.web.dto;

import com.arka.backend.ordering.domain.model.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            UUID productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
