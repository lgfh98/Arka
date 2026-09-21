package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.DeliverOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class DeliverOrderUseCaseDecorator implements DeliverOrderUseCase {

    private final DeliverOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder deliverOrder(UUID orderId) {
        PurchaseOrder order = delegate.deliverOrder(orderId);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
