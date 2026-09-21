package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.entity.OrderItem;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.CreateOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder createOrder(CreateOrderCommand command) {
        OrderId orderId = OrderId.generate();
        CustomerId customerId = new CustomerId(command.customerId());

        List<OrderItem> items = command.items().stream()
                .map(item -> new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        PurchaseOrder order = PurchaseOrder.create(orderId, customerId, items);
        return orderRepository.save(order);
    }
}
