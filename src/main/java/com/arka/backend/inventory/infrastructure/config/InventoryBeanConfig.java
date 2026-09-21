package com.arka.backend.inventory.infrastructure.config;

import com.arka.backend.inventory.domain.port.in.AdjustStockReservationUseCase;
import com.arka.backend.inventory.domain.port.in.AdjustStockUseCase;
import com.arka.backend.inventory.domain.port.in.DeductStockUseCase;
import com.arka.backend.inventory.domain.port.in.GetInventoryQuery;
import com.arka.backend.inventory.domain.port.in.GetProductQuery;
import com.arka.backend.inventory.domain.port.in.RegisterProductUseCase;
import com.arka.backend.inventory.domain.port.in.ReleaseStockReservationUseCase;
import com.arka.backend.inventory.domain.port.in.ReserveStockUseCase;
import com.arka.backend.inventory.domain.port.out.InventoryRepository;
import com.arka.backend.inventory.domain.port.out.ProductRepository;
import com.arka.backend.inventory.domain.service.AdjustStockReservationService;
import com.arka.backend.inventory.domain.service.AdjustStockService;
import com.arka.backend.inventory.domain.service.DeductStockService;
import com.arka.backend.inventory.domain.service.GetInventoryService;
import com.arka.backend.inventory.domain.service.GetProductService;
import com.arka.backend.inventory.domain.service.RegisterProductService;
import com.arka.backend.inventory.domain.service.ReleaseStockReservationService;
import com.arka.backend.inventory.domain.service.ReserveStockService;
import com.arka.backend.inventory.infrastructure.decorator.AdjustStockReservationUseCaseDecorator;
import com.arka.backend.inventory.infrastructure.decorator.AdjustStockUseCaseDecorator;
import com.arka.backend.inventory.infrastructure.decorator.DeductStockUseCaseDecorator;
import com.arka.backend.inventory.infrastructure.decorator.RegisterProductUseCaseDecorator;
import com.arka.backend.inventory.infrastructure.decorator.ReleaseStockReservationUseCaseDecorator;
import com.arka.backend.inventory.infrastructure.decorator.ReserveStockUseCaseDecorator;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryBeanConfig {

    @Bean
    public RegisterProductUseCase registerProductUseCase(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new RegisterProductService(productRepository, inventoryRepository);
        return new RegisterProductUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public AdjustStockUseCase adjustStockUseCase(
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new AdjustStockService(inventoryRepository);
        return new AdjustStockUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public ReserveStockUseCase reserveStockUseCase(
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new ReserveStockService(inventoryRepository);
        return new ReserveStockUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public AdjustStockReservationUseCase adjustStockReservationUseCase(
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new AdjustStockReservationService(inventoryRepository);
        return new AdjustStockReservationUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public DeductStockUseCase deductStockUseCase(
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new DeductStockService(inventoryRepository);
        return new DeductStockUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public ReleaseStockReservationUseCase releaseStockReservationUseCase(
            InventoryRepository inventoryRepository,
            OutboxDomainEventPublisher outboxPublisher) {
        var service = new ReleaseStockReservationService(inventoryRepository);
        return new ReleaseStockReservationUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public GetProductQuery getProductQuery(ProductRepository productRepository) {
        return new GetProductService(productRepository);
    }

    @Bean
    public GetInventoryQuery getInventoryQuery(InventoryRepository inventoryRepository) {
        return new GetInventoryService(inventoryRepository);
    }
}
