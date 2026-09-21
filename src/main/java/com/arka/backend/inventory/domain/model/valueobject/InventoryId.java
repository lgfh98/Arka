package com.arka.backend.inventory.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record InventoryId(UUID value) {
    public InventoryId {
        Objects.requireNonNull(value, "InventoryId no puede ser nulo");
    }

    public static InventoryId generate() {
        return new InventoryId(UUID.randomUUID());
    }

    public static InventoryId of(String value) {
        return new InventoryId(UUID.fromString(value));
    }
}
