package com.arka.backend.cart.infrastructure.decorator;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.port.in.AddCartItemUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class AddCartItemUseCaseDecorator implements AddCartItemUseCase {

    private final AddCartItemUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public Cart addItem(UUID customerId, UUID productId, int quantity, BigDecimal unitPrice) {
        Cart cart = delegate.addItem(customerId, productId, quantity, unitPrice);
        cart.pullDomainEvents().forEach(outboxPublisher::publish);
        return cart;
    }
}
