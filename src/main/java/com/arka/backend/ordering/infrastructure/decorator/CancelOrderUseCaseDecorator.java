package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.CancelOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class CancelOrderUseCaseDecorator implements CancelOrderUseCase {

    private final CancelOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder cancelOrder(UUID orderId) {
        PurchaseOrder order = delegate.cancelOrder(orderId);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
