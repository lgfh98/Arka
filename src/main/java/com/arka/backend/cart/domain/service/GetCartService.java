package com.arka.backend.cart.domain.service;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.domain.port.in.GetCartQuery;
import com.arka.backend.cart.domain.port.out.CartRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GetCartService implements GetCartQuery {

    private final CartRepository cartRepository;

    @Override
    public Cart getByCustomerId(UUID customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> Cart.create(CartId.generate(), customerId));
    }

    @Override
    public List<Cart> getAbandonedCarts() {
        return cartRepository.findByStatus(CartStatus.ABANDONED);
    }
}
