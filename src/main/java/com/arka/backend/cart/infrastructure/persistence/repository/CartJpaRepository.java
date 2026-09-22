package com.arka.backend.cart.infrastructure.persistence.repository;

import com.arka.backend.cart.domain.model.valueobject.CartStatus;
import com.arka.backend.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<CartJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"items"})
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    List<CartJpaEntity> findByStatus(CartStatus status);
}
