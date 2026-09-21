package com.arka.backend.cart.domain.port.in;

import com.arka.backend.cart.domain.model.aggregate.Cart;

import java.util.List;
import java.util.UUID;

public interface GetCartQuery {
    Cart getByCustomerId(UUID customerId);
    List<Cart> getAbandonedCarts();
}
