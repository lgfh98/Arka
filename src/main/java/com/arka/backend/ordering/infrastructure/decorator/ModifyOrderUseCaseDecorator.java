package com.arka.backend.ordering.infrastructure.decorator;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.port.in.ModifyOrderUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ModifyOrderUseCaseDecorator implements ModifyOrderUseCase {

    private final ModifyOrderUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public PurchaseOrder modifyOrder(ModifyOrderCommand command) {
        PurchaseOrder order = delegate.modifyOrder(command);
        order.pullDomainEvents().forEach(outboxPublisher::publish);
        return order;
    }
}
