package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.port.in.AdjustStockUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class AdjustStockUseCaseDecorator implements AdjustStockUseCase {

    private final AdjustStockUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public InventoryItem adjustStock(AdjustStockCommand command) {
        InventoryItem item = delegate.adjustStock(command);
        item.pullDomainEvents().forEach(outboxPublisher::publish);
        return item;
    }
}
