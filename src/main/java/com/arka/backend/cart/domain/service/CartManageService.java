package com.arka.backend.cart.domain.service;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.port.in.AddCartItemUseCase;
import com.arka.backend.cart.domain.port.in.RemoveCartItemUseCase;
import com.arka.backend.cart.domain.port.out.CartRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class CartManageService implements AddCartItemUseCase, RemoveCartItemUseCase {

    private final CartRepository cartRepository;

    @Override
    public Cart addItem(UUID customerId, UUID productId, int quantity, BigDecimal unitPrice) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> Cart.create(CartId.generate(), customerId));

        cart.addItem(productId, quantity, unitPrice);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(UUID customerId, UUID productId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> Cart.create(CartId.generate(), customerId));

        cart.removeItem(productId);
        return cartRepository.save(cart);
    }
}
