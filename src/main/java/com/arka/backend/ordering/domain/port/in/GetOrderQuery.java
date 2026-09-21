package com.arka.backend.ordering.domain.port.in;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;

import java.util.List;

public interface GetOrderQuery {
    PurchaseOrder getById(OrderId id);
    List<PurchaseOrder> getByCustomerId(CustomerId customerId);
    List<PurchaseOrder> getAll();
}
