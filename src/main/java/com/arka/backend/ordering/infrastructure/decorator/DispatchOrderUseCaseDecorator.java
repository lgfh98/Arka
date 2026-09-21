package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.DispatchOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class DispatchOrderUseCaseDecorator implements DispatchOrderUseCase {

    private final DispatchOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder dispatchOrder(UUID orderId) {
        PurchaseOrder order = delegate.dispatchOrder(orderId);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
