package com.arka.backend.cart.domain.service;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import com.arka.backend.cart.domain.port.out.CartRepository;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
public class DetectAbandonedCartsService implements DetectAbandonedCartsUseCase {

    private final CartRepository cartRepository;

    @Override
    public int detectAndMarkAbandonedCarts(Duration inactiveThreshold) {
        List<Cart> activeCarts = cartRepository.findByStatus(CartStatus.ACTIVE);
        int count = 0;

        for (Cart cart : activeCarts) {
            if (cart.isInactiveFor(inactiveThreshold)) {
                cart.markAsAbandoned();
                cartRepository.save(cart);
                count++;
            }
        }

        return count;
    }
}
