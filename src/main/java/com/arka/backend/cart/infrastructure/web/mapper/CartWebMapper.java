package com.arka.backend.cart.infrastructure.web.mapper;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.infrastructure.web.dto.CartResponse;
import org.springframework.stereotype.Component;

@Component
public class CartWebMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) return null;

        var items = cart.getItems().stream()
                .map(item -> new CartResponse.CartItemResponse(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new CartResponse(
                cart.getId().value(),
                cart.getCustomerId(),
                cart.getStatus(),
                cart.getTotal(),
                cart.getLastActivityAt(),
                items
        );
    }
}
