package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.DispatchOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DispatchOrderService implements DispatchOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder dispatchOrder(UUID orderId) {
        PurchaseOrder order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + orderId));

        order.dispatch();
        return orderRepository.save(order);
    }
}
