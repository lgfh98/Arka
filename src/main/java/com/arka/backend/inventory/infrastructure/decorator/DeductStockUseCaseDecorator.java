package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.DeductStockUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class DeductStockUseCaseDecorator implements DeductStockUseCase {

    private final DeductStockUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public InventoryItem deductStock(ProductId productId, int quantity) {
        InventoryItem item = delegate.deductStock(productId, quantity);
        item.pullDomainEvents().forEach(outboxPublisher::publish);
        return item;
    }
}
