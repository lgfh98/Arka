package com.arka.backend.inventory.infrastructure.decorator;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.AdjustStockReservationUseCase;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class AdjustStockReservationUseCaseDecorator implements AdjustStockReservationUseCase {

    private final AdjustStockReservationUseCase delegate;
    private final OutboxDomainEventPublisher outboxPublisher;

    @Transactional
    @Override
    public InventoryItem adjustReservation(UUID orderId, ProductId productId, int oldQuantity, int newQuantity) {
        InventoryItem item = delegate.adjustReservation(orderId, productId, oldQuantity, newQuantity);
        item.pullDomainEvents().forEach(outboxPublisher::publish);
        return item;
    }
}
