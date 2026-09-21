package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.ReleaseStockReservationUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ReleaseStockReservationUseCaseDecorator implements ReleaseStockReservationUseCase {

    private final ReleaseStockReservationUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public InventoryItem releaseReservation(ProductId productId, int quantity) {
        InventoryItem item = delegate.releaseReservation(productId, quantity);
        item.pullDomainEvents().forEach(outboxPublisher::publish);
        return item;
    }
}
