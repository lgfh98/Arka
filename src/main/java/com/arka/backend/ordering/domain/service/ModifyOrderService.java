package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.ModifyOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ModifyOrderService implements ModifyOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder modifyOrder(ModifyOrderCommand command) {
        OrderId orderId = new OrderId(command.orderId());
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + command.orderId()));

        List<OrderItem> newItems = command.items().stream()
                .map(item -> new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        order.modifyItems(newItems);
        return orderRepository.save(order);
    }
}
