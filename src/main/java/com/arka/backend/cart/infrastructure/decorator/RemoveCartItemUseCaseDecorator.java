package com.arka.backend.cart.infrastructure.decorator;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.port.in.RemoveCartItemUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class RemoveCartItemUseCaseDecorator implements RemoveCartItemUseCase {

    private final RemoveCartItemUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public Cart removeItem(UUID customerId, UUID productId) {
        Cart cart = delegate.removeItem(customerId, productId);
        cart.pullDomainEvents().forEach(outboxPublisher::publish);
        return cart;
    }
}
