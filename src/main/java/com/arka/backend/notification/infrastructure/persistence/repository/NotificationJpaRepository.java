package com.arka.backend.notification.infrastructure.persistence.repository;

import com.arka.backend.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByRecipientOrderBySentAtDesc(String recipient);
    List<NotificationJpaEntity> findAllByOrderBySentAtDesc();
}
