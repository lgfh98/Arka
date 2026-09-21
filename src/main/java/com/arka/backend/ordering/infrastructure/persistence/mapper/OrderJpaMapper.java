package com.arka.backend.ordering.infrastructure.persistence.mapper;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.infrastructure.persistence.entity.OrderJpaEntity;
import com.arka.backend.ordering.infrastructure.persistence.entity.OrderItemJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderJpaMapper {

    public OrderJpaEntity toEntity(PurchaseOrder domain) {
        if (domain == null) return null;

        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(domain.getId().value())
                .customerId(domain.getCustomerId().value())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .items(new ArrayList<>())
                .build();

        for (OrderItem item : domain.getItems()) {
            OrderItemJpaEntity itemEntity = OrderItemJpaEntity.builder()
                    .order(entity)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            entity.addItem(itemEntity);
        }

        return entity;
    }

    public PurchaseOrder toDomain(OrderJpaEntity entity) {
        if (entity == null) return null;

        List<OrderItem> items = entity.getItems().stream()
                .map(itemEntity -> new OrderItem(
                        itemEntity.getProductId(),
                        itemEntity.getQuantity(),
                        itemEntity.getUnitPrice()
                ))
                .toList();

        return new PurchaseOrder(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                entity.getStatus(),
                items,
                entity.getTotalAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
