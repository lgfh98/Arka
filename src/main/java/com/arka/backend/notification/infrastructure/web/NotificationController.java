package com.arka.backend.notification.infrastructure.web;

import com.arka.backend.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.arka.backend.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationJpaRepository notificationRepository;

    @Operation(summary = "Consultar historial de notificaciones", description = "Lista las notificaciones multicanal despachadas por eventos de orden o carrito (HU6, HU8)")
    @GetMapping
    public ResponseEntity<List<NotificationJpaEntity>> getNotifications(
            @RequestParam(name = "recipient", required = false) String recipient) {
        List<NotificationJpaEntity> notifications = (recipient != null && !recipient.isBlank())
                ? notificationRepository.findByRecipientOrderBySentAtDesc(recipient)
                : notificationRepository.findAllByOrderBySentAtDesc();

        return ResponseEntity.ok(notifications);
    }
}
