package com.arka.backend.ordering.infrastructure.persistence.adapter;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import com.arka.backend.ordering.infrastructure.persistence.mapper.OrderJpaMapper;
import com.arka.backend.ordering.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderJpaMapper orderJpaMapper;

    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        var entity = orderJpaMapper.toEntity(order);
        orderJpaRepository.save(entity);
        return order;
    }

    @Override
    public Optional<PurchaseOrder> findById(OrderId id) {
        return orderJpaRepository.findById(id.value())
                .map(orderJpaMapper::toDomain);
    }

    @Override
    public List<PurchaseOrder> findByCustomerId(CustomerId customerId) {
        return orderJpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId.value()).stream()
                .map(orderJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<PurchaseOrder> findAll() {
        return orderJpaRepository.findAll().stream()
                .map(orderJpaMapper::toDomain)
                .toList();
    }
}
