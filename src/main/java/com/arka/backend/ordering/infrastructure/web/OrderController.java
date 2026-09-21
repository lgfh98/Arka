package com.arka.backend.ordering.infrastructure.web;

import com.arka.backend.ordering.domain.model.aggregate.PurchaseOrder;
import com.arka.backend.ordering.domain.model.valueobject.CustomerId;
import com.arka.backend.ordering.domain.model.valueobject.OrderId;
import com.arka.backend.ordering.domain.port.in.CancelOrderUseCase;
import com.arka.backend.ordering.domain.port.in.ConfirmOrderUseCase;
import com.arka.backend.ordering.domain.port.in.CreateOrderUseCase;
import com.arka.backend.ordering.domain.port.in.DeliverOrderUseCase;
import com.arka.backend.ordering.domain.port.in.DispatchOrderUseCase;
import com.arka.backend.ordering.domain.port.in.GetOrderQuery;
import com.arka.backend.ordering.domain.port.in.ModifyOrderUseCase;
import com.arka.backend.ordering.infrastructure.web.dto.CreateOrderRequest;
import com.arka.backend.ordering.infrastructure.web.dto.ModifyOrderRequest;
import com.arka.backend.ordering.infrastructure.web.dto.OrderResponse;
import com.arka.backend.ordering.infrastructure.web.mapper.OrderWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ModifyOrderUseCase modifyOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final DispatchOrderUseCase dispatchOrderUseCase;
    private final DeliverOrderUseCase deliverOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderQuery getOrderQuery;
    private final OrderWebMapper orderWebMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var items = request.items().stream()
                .map(item -> new CreateOrderUseCase.OrderItemCommand(
                        item.productId(), item.quantity(), item.unitPrice()
                ))
                .toList();

        var command = new CreateOrderUseCase.CreateOrderCommand(request.customerId(), items);
        PurchaseOrder created = createOrderUseCase.createOrder(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId().value())
                .toUri();

        return ResponseEntity.created(location).body(orderWebMapper.toResponse(created));
    }

    @PutMapping("/{id}/items")
    public ResponseEntity<OrderResponse> modifyOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ModifyOrderRequest request) {
        var items = request.items().stream()
                .map(item -> new ModifyOrderUseCase.OrderItemCommand(
                        item.productId(), item.quantity(), item.unitPrice()
                ))
                .toList();

        var command = new ModifyOrderUseCase.ModifyOrderCommand(id, items);
        PurchaseOrder modified = modifyOrderUseCase.modifyOrder(command);

        return ResponseEntity.ok(orderWebMapper.toResponse(modified));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        PurchaseOrder confirmed = confirmOrderUseCase.confirmOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(confirmed));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<OrderResponse> dispatchOrder(@PathVariable UUID id) {
        PurchaseOrder dispatched = dispatchOrderUseCase.dispatchOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(dispatched));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable UUID id) {
        PurchaseOrder delivered = deliverOrderUseCase.deliverOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(delivered));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        PurchaseOrder cancelled = cancelOrderUseCase.cancelOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(cancelled));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        PurchaseOrder order = getOrderQuery.getById(new OrderId(id));
        return ResponseEntity.ok(orderWebMapper.toResponse(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(name = "customerId", required = false) UUID customerId) {
        List<PurchaseOrder> orders = (customerId != null)
                ? getOrderQuery.getByCustomerId(new CustomerId(customerId))
                : getOrderQuery.getAll();

        return ResponseEntity.ok(orders.stream().map(orderWebMapper::toResponse).toList());
    }
}
