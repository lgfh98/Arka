package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.ConfirmOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmOrderUseCaseDecorator implements ConfirmOrderUseCase {

    private final ConfirmOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder confirmOrder(UUID orderId) {
        PurchaseOrder order = delegate.confirmOrder(orderId);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
