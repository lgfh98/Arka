package com.arka.backend.cart.infrastructure.scheduler;

import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CartInactivityScheduler (Actor ⏱️ Scheduler)")
class CartInactivitySchedulerTest {

    @Mock
    private DetectAbandonedCartsUseCase detectAbandonedCartsUseCase;

    @InjectMocks
    private CartInactivityScheduler scheduler;

    @Test
    @DisplayName("Debe invocar el caso de uso con el umbral de inactividad configurado")
    void shouldInvokeDetectAbandonedCartsUseCase() {
        ReflectionTestUtils.setField(scheduler, "inactivityThresholdMinutes", 120);
        when(detectAbandonedCartsUseCase.detectAndMarkAbandonedCarts(Duration.ofMinutes(120))).thenReturn(2);

        scheduler.executeInactivityCheck();

        verify(detectAbandonedCartsUseCase, times(1)).detectAndMarkAbandonedCarts(Duration.ofMinutes(120));
    }

    @Test
    @DisplayName("Debe capturar y tolerar excepciones sin propagar fallo fatal")
    void shouldHandleExceptionsGracefully() {
        ReflectionTestUtils.setField(scheduler, "inactivityThresholdMinutes", 120);
        when(detectAbandonedCartsUseCase.detectAndMarkAbandonedCarts(any())).thenThrow(new RuntimeException("DB Connection Timeout"));

        // No debe lanzar excepción
        scheduler.executeInactivityCheck();

        verify(detectAbandonedCartsUseCase, times(1)).detectAndMarkAbandonedCarts(any());
    }
}
