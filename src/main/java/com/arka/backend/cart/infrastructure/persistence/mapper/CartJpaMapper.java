package com.arka.backend.cart.infrastructure.persistence.mapper;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.entity.CartItem;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.infrastructure.persistence.entity.CartItemJpaEntity;
import com.arka.backend.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CartJpaMapper {

    public CartJpaEntity toEntity(Cart domain) {
        if (domain == null) return null;

        CartJpaEntity entity = CartJpaEntity.builder()
                .id(domain.getId().value())
                .customerId(domain.getCustomerId())
                .status(domain.getStatus())
                .lastActivityAt(domain.getLastActivityAt())
                .items(new ArrayList<>())
                .build();

        for (CartItem item : domain.getItems()) {
            CartItemJpaEntity itemEntity = CartItemJpaEntity.builder()
                    .cart(entity)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            entity.addItem(itemEntity);
        }

        return entity;
    }

    public Cart toDomain(CartJpaEntity entity) {
        if (entity == null) return null;

        List<CartItem> items = entity.getItems().stream()
                .map(itemEntity -> new CartItem(
                        itemEntity.getProductId(),
                        itemEntity.getQuantity(),
                        itemEntity.getUnitPrice()
                ))
                .toList();

        return new Cart(
                new CartId(entity.getId()),
                entity.getCustomerId(),
                entity.getStatus(),
                items,
                entity.getLastActivityAt()
        );
    }
}
