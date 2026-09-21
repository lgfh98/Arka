package com.arka.backend.cart.infrastructure.config;

import com.arka.backend.cart.domain.port.in.AddCartItemUseCase;
import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import com.arka.backend.cart.domain.port.in.GetCartQuery;
import com.arka.backend.cart.domain.port.in.RemoveCartItemUseCase;
import com.arka.backend.cart.domain.port.out.CartRepository;
import com.arka.backend.cart.domain.service.CartManageService;
import com.arka.backend.cart.domain.service.DetectAbandonedCartsService;
import com.arka.backend.cart.domain.service.GetCartService;
import com.arka.backend.cart.infrastructure.decorator.AddCartItemUseCaseDecorator;
import com.arka.backend.cart.infrastructure.decorator.DetectAbandonedCartsUseCaseDecorator;
import com.arka.backend.cart.infrastructure.decorator.RemoveCartItemUseCaseDecorator;
import com.arka.backend.shared.infrastructure.outbox.OutboxDomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartBeanConfig {

    @Bean
    public AddCartItemUseCase addCartItemUseCase(CartRepository cartRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new CartManageService(cartRepository);
        return new AddCartItemUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public RemoveCartItemUseCase removeCartItemUseCase(CartRepository cartRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new CartManageService(cartRepository);
        return new RemoveCartItemUseCaseDecorator(service, outboxPublisher);
    }

    @Bean
    public DetectAbandonedCartsUseCase detectAbandonedCartsUseCase(CartRepository cartRepository, OutboxDomainEventPublisher outboxPublisher) {
        var service = new DetectAbandonedCartsService(cartRepository);
        return new DetectAbandonedCartsUseCaseDecorator(service, cartRepository, outboxPublisher);
    }

    @Bean
    public GetCartQuery getCartQuery(CartRepository cartRepository) {
        return new GetCartService(cartRepository);
    }
}
