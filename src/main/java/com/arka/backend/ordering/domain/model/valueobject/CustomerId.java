package com.arka.backend.ordering.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId no puede ser nulo");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }
}
