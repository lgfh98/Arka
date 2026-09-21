package com.arka.backend.inventory.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Map<String, String> attributes
) {}
