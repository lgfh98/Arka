package com.arka.backend.cart.domain.port.in;

import com.arka.backend.cart.domain.model.aggregate.Cart;

import java.math.BigDecimal;
import java.util.UUID;

public interface AddCartItemUseCase {
    Cart addItem(UUID customerId, UUID productId, int quantity, BigDecimal unitPrice);
}
