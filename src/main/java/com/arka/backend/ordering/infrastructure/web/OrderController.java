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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Ordering")
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

    @Operation(summary = "Registrar orden de compra B2B", description = "Crea una orden en estado PENDIENTE y reserva stock preventivo anti-sobreventa vía Outbox (HU4)")
    @ApiResponse(responseCode = "201", description = "Orden creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de orden inválidos")
    @ApiResponse(responseCode = "422", description = "Invariante violada (INV-06: Mínimo 1 producto)")
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

    @Operation(summary = "Modificar pedido en estado PENDIENTE", description = "Ajusta cantidades de ítems y reajusta la reserva de stock preventivo (HU5)")
    @ApiResponse(responseCode = "200", description = "Orden modificada")
    @ApiResponse(responseCode = "422", description = "Invariante violada (INV-04: Solo modificable en PENDIENTE)")
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

    @Operation(summary = "Confirmar orden de compra", description = "Transiciona de PENDIENTE a CONFIRMADO, descuenta stock permanente e inicia analítica de ventas (HU6)")
    @ApiResponse(responseCode = "200", description = "Orden confirmada")
    @ApiResponse(responseCode = "422", description = "Transición de estado inválida (INV-05)")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        PurchaseOrder confirmed = confirmOrderUseCase.confirmOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(confirmed));
    }

    @Operation(summary = "Despachar orden (Operador Logístico)", description = "Transiciona de CONFIRMADO a EN_DESPACHO hacia el almacén de destino (HU6)")
    @ApiResponse(responseCode = "200", description = "Orden en despacho")
    @ApiResponse(responseCode = "422", description = "Transición de estado inválida (INV-05)")
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<OrderResponse> dispatchOrder(@PathVariable UUID id) {
        PurchaseOrder dispatched = dispatchOrderUseCase.dispatchOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(dispatched));
    }

    @Operation(summary = "Marcar orden como entregada (Operador Logístico)", description = "Transiciona de EN_DESPACHO a ENTREGADO a satisfacción (HU6)")
    @ApiResponse(responseCode = "200", description = "Orden entregada")
    @ApiResponse(responseCode = "422", description = "Transición de estado inválida (INV-05)")
    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable UUID id) {
        PurchaseOrder delivered = deliverOrderUseCase.deliverOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(delivered));
    }

    @Operation(summary = "Cancelar orden de compra", description = "Transiciona a CANCELADO y libera cualquier reserva de stock preventivo (HU6)")
    @ApiResponse(responseCode = "200", description = "Orden cancelada")
    @ApiResponse(responseCode = "422", description = "Transición de estado inválida (INV-05)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        PurchaseOrder cancelled = cancelOrderUseCase.cancelOrder(id);
        return ResponseEntity.ok(orderWebMapper.toResponse(cancelled));
    }

    @Operation(summary = "Consultar orden por ID", description = "Obtiene el detalle completo de una orden de compra")
    @ApiResponse(responseCode = "200", description = "Orden encontrada")
    @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        PurchaseOrder order = getOrderQuery.getById(new OrderId(id));
        return ResponseEntity.ok(orderWebMapper.toResponse(order));
    }

    @Operation(summary = "Consultar lista de órdenes", description = "Lista todas las órdenes o filtra por ID de cliente mayorista")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(name = "customerId", required = false) UUID customerId) {
        List<PurchaseOrder> orders = (customerId != null)
                ? getOrderQuery.getByCustomerId(new CustomerId(customerId))
                : getOrderQuery.getAll();

        return ResponseEntity.ok(orders.stream().map(orderWebMapper::toResponse).toList());
    }
}
