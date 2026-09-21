package com.arka.backend.notification.infrastructure.web;

import com.arka.backend.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.arka.backend.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationJpaRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<NotificationJpaEntity>> getNotifications(
            @RequestParam(name = "recipient", required = false) String recipient) {
        List<NotificationJpaEntity> notifications = (recipient != null && !recipient.isBlank())
                ? notificationRepository.findByRecipientOrderBySentAtDesc(recipient)
                : notificationRepository.findAllByOrderBySentAtDesc();

        return ResponseEntity.ok(notifications);
    }
}
