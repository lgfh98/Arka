package com.arka.backend.cart.infrastructure.web.dto;

import com.arka.backend.cart.domain.model.valueobject.CartStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        CartStatus status,
        BigDecimal total,
        Instant lastActivityAt,
        List<CartItemResponse> items
) {
    public record CartItemResponse(
            UUID productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
