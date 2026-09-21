package com.arka.backend.analytics.infrastructure.persistence.repository;

import com.arka.backend.analytics.infrastructure.persistence.entity.ProductSalesProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductSalesProjectionJpaRepository extends JpaRepository<ProductSalesProjectionJpaEntity, UUID> {
    List<ProductSalesProjectionJpaEntity> findAllByOrderByTotalUnitsSoldDesc();
}
