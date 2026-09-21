package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.ConfirmOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder confirmOrder(UUID orderId) {
        PurchaseOrder order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + orderId));

        order.confirm();
        return orderRepository.save(order);
    }
}
