package com.arka.backend.ordering.infrastructure.config;

import com.arka.backend.ordering.domain.port.in.CancelOrderUseCase;
import com.arka.backend.ordering.domain.port.in.ConfirmOrderUseCase;
import com.arka.backend.ordering.domain.port.in.CreateOrderUseCase;
import com.arka.backend.ordering.domain.port.in.DeliverOrderUseCase;
import com.arka.backend.ordering.domain.port.in.DispatchOrderUseCase;
import com.arka.backend.ordering.domain.port.in.GetOrderQuery;
import com.arka.backend.ordering.domain.port.in.ModifyOrderUseCase;
import com.arka.backend.ordering.domain.port.out.OrderRepository;
import com.arka.backend.ordering.domain.service.CancelOrderService;
import com.arka.backend.ordering.domain.service.ConfirmOrderService;
import com.arka.backend.ordering.domain.service.CreateOrderService;
import com.arka.backend.ordering.domain.service.DeliverOrderService;
import com.arka.backend.ordering.domain.service.DispatchOrderService;
import com.arka.backend.ordering.domain.service.GetOrderService;
import com.arka.backend.ordering.domain.service.ModifyOrderService;
import com.arka.backend.ordering.infrastructure.decorator.CancelOrderUseCaseDecorator;
import com.arka.backend.ordering.infrastructure.decorator.ConfirmOrderUseCaseDecorator;
import com.arka.backend.ordering.infrastructure.decorator.CreateOrderUseCaseDecorator;
import com.arka.backend.ordering.infrastructure.decorator.DeliverOrderUseCaseDecorator;
import com.arka.backend.ordering.infrastructure.decorator.DispatchOrderUseCaseDecorator;
import com.arka.backend.ordering.infrastructure.decorator.ModifyOrderUseCaseDecorator;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderingBeanConfig {

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new CreateOrderService(orderRepository);
        return new CreateOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public ModifyOrderUseCase modifyOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new ModifyOrderService(orderRepository);
        return new ModifyOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public ConfirmOrderUseCase confirmOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new ConfirmOrderService(orderRepository);
        return new ConfirmOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public DispatchOrderUseCase dispatchOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new DispatchOrderService(orderRepository);
        return new DispatchOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public DeliverOrderUseCase deliverOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new DeliverOrderService(orderRepository);
        return new DeliverOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(OrderRepository orderRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new CancelOrderService(orderRepository);
        return new CancelOrderUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public GetOrderQuery getOrderQuery(OrderRepository orderRepository) {
        return new GetOrderService(orderRepository);
    }
}
