package com.arka.backend.analytics.infrastructure.persistence.repository;

import com.arka.backend.analytics.infrastructure.persistence.entity.ReplenishmentProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReplenishmentProjectionJpaRepository extends JpaRepository<ReplenishmentProjectionJpaEntity, UUID> {
    List<ReplenishmentProjectionJpaEntity> findAllByOrderByCurrentStockAsc();
}
