package com.arka.backend.cart.infrastructure.decorator;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import com.arka.backend.cart.domain.port.out.CartRepository;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
public class DetectAbandonedCartsUseCaseDecorator implements DetectAbandonedCartsUseCase {

    private final DetectAbandonedCartsUseCase delegate;
    private final CartRepository cartRepository;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public int detectAndMarkAbandonedCarts(Duration inactiveThreshold) {
        int count = delegate.detectAndMarkAbandonedCarts(inactiveThreshold);
        // Flush any domain events emitted by abandoned carts
        List<Cart> abandonedCarts = cartRepository.findByStatus(CartStatus.ABANDONED);
        for (Cart cart : abandonedCarts) {
            cart.pullDomainEvents().forEach(outboxPublisher::publish);
        }
        return count;
    }
}
