package com.arka.backend.cart.infrastructure.scheduler;

import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Scheduler autónomo para detección y marcado de carritos abandonados (HU8).
 * Opera como el disparador del actor de negocio: ⏱️ Scheduler de Inactividad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "arka.scheduler.cart-abandonment.enabled", havingValue = "true", matchIfMissing = true)
public class CartInactivityScheduler {

    private final DetectAbandonedCartsUseCase detectAbandonedCartsUseCase;

    @Value("${arka.cart.inactivity-threshold-minutes:120}")
    private int inactivityThresholdMinutes;

    @Scheduled(fixedDelayString = "${arka.cart.abandonment-check-delay-ms:60000}")
    public void executeInactivityCheck() {
        log.debug("⏱️ [Scheduler] Ejecutando detección autónoma de carritos abandonados...");
        try {
            int marked = detectAbandonedCartsUseCase.detectAndMarkAbandonedCarts(Duration.ofMinutes(inactivityThresholdMinutes));
            if (marked > 0) {
                log.info("⏱️ [Scheduler] Se detectaron y marcaron {} carritos como abandonados (umbral: {} minutos)",
                        marked, inactivityThresholdMinutes);
            }
        } catch (Exception e) {
            log.error("❌ [Scheduler] Error durante la evaluación de carritos abandonados: {}", e.getMessage(), e);
        }
    }
}
