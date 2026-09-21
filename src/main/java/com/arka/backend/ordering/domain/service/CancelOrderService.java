package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.CancelOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder cancelOrder(UUID orderId) {
        PurchaseOrder order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + orderId));

        order.cancel();
        return orderRepository.save(order);
    }
}
