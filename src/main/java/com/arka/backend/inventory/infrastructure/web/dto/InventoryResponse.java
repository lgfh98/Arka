package com.arka.backend.inventory.infrastructure.web.dto;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        int physicalStock,
        int reservedStock,
        int availableStock,
        int minimumThreshold,
        boolean lowStock
) {}
