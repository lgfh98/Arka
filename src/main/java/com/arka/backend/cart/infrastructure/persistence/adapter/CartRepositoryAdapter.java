package com.arka.backend.cart.infrastructure.persistence.adapter;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartId;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.domain.port.out.CartRepository;
import com.arka.backend.cart.infrastructure.persistence.mapper.CartJpaMapper;
import com.arka.backend.cart.infrastructure.persistence.repository.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;
    private final CartJpaMapper cartJpaMapper;

    @Override
    public Cart save(Cart cart) {
        var entity = cartJpaMapper.toEntity(cart);
        cartJpaRepository.save(entity);
        return cart;
    }

    @Override
    public Optional<Cart> findById(CartId id) {
        return cartJpaRepository.findById(id.value())
                .map(cartJpaMapper::toDomain);
    }

    @Override
    public Optional<Cart> findByCustomerId(UUID customerId) {
        return cartJpaRepository.findByCustomerId(customerId)
                .map(cartJpaMapper::toDomain);
    }

    @Override
    public List<Cart> findByStatus(CartStatus status) {
        return cartJpaRepository.findByStatus(status).stream()
                .map(cartJpaMapper::toDomain)
                .toList();
    }
}
