package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.ReserveStockUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class ReserveStockUseCaseDecorator implements ReserveStockUseCase {

    private final ReserveStockUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public InventoryItem reserveStock(UUID orderId, ProductId productId, int quantity) {
        InventoryItem item = delegate.reserveStock(orderId, productId, quantity);
        item.pullDomainEvents().forEach(outboxPublisher::publish);
        return item;
    }
}
