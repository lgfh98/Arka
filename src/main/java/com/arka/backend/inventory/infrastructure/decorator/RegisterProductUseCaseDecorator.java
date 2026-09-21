package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.port.in.RegisterProductUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class RegisterProductUseCaseDecorator implements RegisterProductUseCase {

    private final RegisterProductUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public Product registerProduct(RegisterProductCommand command) {
        Product product = delegate.registerProduct(command);
        product.pullDomainEvents().forEach(outboxPublisher::publish);
        return product;
    }
}
