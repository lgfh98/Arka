package com.arka.backend.ordering.domain.service;

import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.GetOrderQuery;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetOrderService implements GetOrderQuery {

    private final OrderRepository orderRepository;

    @Override
    public PurchaseOrder getById(OrderId id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + id.value()));
    }

    @Override
    public List<PurchaseOrder> getByCustomerId(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public List<PurchaseOrder> getAll() {
        return orderRepository.findAll();
    }
}
