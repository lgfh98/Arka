package com.arka.backend.ordering.domain.port.in;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ModifyOrderUseCase {

    PurchaseOrder modifyOrder(ModifyOrderCommand command);

    record ModifyOrderCommand(
            UUID orderId,
            List<OrderItemCommand> items
    ) {}

    record OrderItemCommand(
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
