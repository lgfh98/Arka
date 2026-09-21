package com.arka.backend.cart.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CartId(UUID value) {
    public CartId {
        Objects.requireNonNull(value, "CartId no puede ser nulo");
    }

    public static CartId generate() {
        return new CartId(UUID.randomUUID());
    }

    public static CartId of(String value) {
        return new CartId(UUID.fromString(value));
    }
}
