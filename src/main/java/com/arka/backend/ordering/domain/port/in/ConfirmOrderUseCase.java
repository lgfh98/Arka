package com.arka.backend.ordering.domain.port.in;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;

import java.util.UUID;

public interface ConfirmOrderUseCase {
    PurchaseOrder confirmOrder(UUID orderId);
}
