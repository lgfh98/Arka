package com.arka.backend.ordering.infrastructure.web.mapper;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.infrastructure.web.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderWebMapper {

    public OrderResponse toResponse(PurchaseOrder order) {
        if (order == null) return null;

        var itemResponses = order.getItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId().value(),
                order.getCustomerId().value(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }
}
