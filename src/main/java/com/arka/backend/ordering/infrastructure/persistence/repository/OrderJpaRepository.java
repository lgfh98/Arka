package com.arka.backend.ordering.infrastructure.persistence.repository;

import com.arka.backend.ordering.domain.model.valueobject.OrderStatus;
import com.arka.backend.ordering.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<OrderJpaEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findAll();

    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findByStatus(OrderStatus status);
}
