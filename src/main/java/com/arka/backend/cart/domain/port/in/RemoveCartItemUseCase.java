package com.arka.backend.cart.domain.port.in;

import com.arka.backend.cart.domain.model.aggregate.Cart;

import java.util.UUID;

public interface RemoveCartItemUseCase {
    Cart removeItem(UUID customerId, UUID productId);
}
