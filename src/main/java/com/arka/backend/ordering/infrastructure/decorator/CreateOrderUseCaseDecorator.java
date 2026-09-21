package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.CreateOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class CreateOrderUseCaseDecorator implements CreateOrderUseCase {

    private final CreateOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder createOrder(CreateOrderCommand command) {
        PurchaseOrder order = delegate.createOrder(command);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
