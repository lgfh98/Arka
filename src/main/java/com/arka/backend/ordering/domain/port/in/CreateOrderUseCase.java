package com.arka.backend.ordering.domain.port.in;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CreateOrderUseCase {

    PurchaseOrder createOrder(CreateOrderCommand command);

    record CreateOrderCommand(
            UUID customerId,
            List<OrderItemCommand> items
    ) {}

    record OrderItemCommand(
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
