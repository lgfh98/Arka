package com.arka.backend.analytics.infrastructure.event;

import com.arka.backend.analytics.infrastructure.persistence.entity.CustomerSalesProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.entity.ProductSalesProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.entity.ReplenishmentProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.repository.CustomerSalesProjectionJpaRepository;
import com.arka.backend.analytics.infrastructure.persistence.repository.ProductSalesProjectionJpaRepository;
import com.arka.backend.analytics.infrastructure.persistence.repository.ReplenishmentProjectionJpaRepository;
import com.arka.backend.inventory.domain.model.event.LowStockDetectedEvent;
import com.arka.backend.ordering.domain.model.event.OrderConfirmedEvent;
import com.arka.backend.ordering.domain.model.event.OrderItemPayload;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaEntity;
import com.arka.backend.shared.infrastructure.idempotency.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private static final String CONSUMER_CONTEXT = "analytics";

    private final ProductSalesProjectionJpaRepository productSalesRepo;
    private final CustomerSalesProjectionJpaRepository customerSalesRepo;
    private final ReplenishmentProjectionJpaRepository replenishmentRepo;
    private final ProcessedEventJpaRepository processedEventRepository;

    @Transactional
    @EventListener
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Analytics] Proyectando ventas para orden confirmada: {}", event.orderId().value());

        // Proyectar ventas por producto
        for (OrderItemPayload item : event.items()) {
            ProductSalesProjectionJpaEntity productProjection = productSalesRepo.findById(item.productId())
                    .orElseGet(() -> ProductSalesProjectionJpaEntity.builder()
                            .productId(item.productId())
                            .totalUnitsSold(0)
                            .totalRevenue(BigDecimal.ZERO)
                            .build());

            productProjection.setTotalUnitsSold(productProjection.getTotalUnitsSold() + item.quantity());
            BigDecimal itemRevenue = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            productProjection.setTotalRevenue(productProjection.getTotalRevenue().add(itemRevenue));
            productSalesRepo.save(productProjection);
        }

        // Proyectar compras por cliente
        CustomerSalesProjectionJpaEntity customerProjection = customerSalesRepo.findById(event.customerId().value())
                .orElseGet(() -> CustomerSalesProjectionJpaEntity.builder()
                        .customerId(event.customerId().value())
                        .totalOrdersCount(0)
                        .totalSpent(BigDecimal.ZERO)
                        .build());

        customerProjection.setTotalOrdersCount(customerProjection.getTotalOrdersCount() + 1);
        customerProjection.setTotalSpent(customerProjection.getTotalSpent().add(event.totalAmount()));
        customerSalesRepo.save(customerProjection);

        markAsProcessed(event.eventId());
    }

    @Transactional
    @EventListener
    public void onLowStockDetected(LowStockDetectedEvent event) {
        if (isAlreadyProcessed(event.eventId())) return;

        log.info("[Analytics] Proyectando producto en riesgo de abastecimiento: {}", event.productId().value());

        var projection = ReplenishmentProjectionJpaEntity.builder()
                .productId(event.productId().value())
                .currentStock(event.currentStock())
                .minimumThreshold(event.threshold())
                .updatedAt(Instant.now())
                .build();

        replenishmentRepo.save(projection);
        markAsProcessed(event.eventId());
    }

    private boolean isAlreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsByEventIdAndConsumerContext(eventId, CONSUMER_CONTEXT)) {
            log.warn("[Analytics] Evento duplicado ignorado: {}", eventId);
            return true;
        }
        return false;
    }

    private void markAsProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEventJpaEntity(eventId, CONSUMER_CONTEXT, Instant.now()));
    }
}
