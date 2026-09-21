package com.arka.backend.ordering.domain.model.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemPayload(
        UUID productId,
        int quantity,
        BigDecimal unitPrice
) {}
