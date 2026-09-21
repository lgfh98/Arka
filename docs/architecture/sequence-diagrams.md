# 🔄 Diagramas de Secuencia: Arka Modular Monolith (Minimal DDD & Java 25)

Este documento detalla las interacciones dinámicas de los principales flujos del sistema, ilustrando la transaccionalidad atómica, el desacoplamiento mediante **Transactional Outbox**, la idempotencia y el manejo de errores estandarizado con **RFC 9457 ProblemDetail**.

---

## 1. Happy Path: Creación y Confirmación de Orden B2B con Transactional Outbox

Ilustra la radicación de la orden por parte del cliente mayorista, la persistencia atómica en la misma transacción ACID de la orden y del evento en `outbox_events`, y el relay asíncrono hacia el inventario para reservar stock preventivo.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as 👤 Cliente Mayorista
    participant Controller as 🌐 OrderController
    participant Decorator as 🛡️ CreateOrderUseCaseDecorator<br/>(@Transactional)
    participant Service as ⚙️ CreateOrderService<br/>(POJO Dominio)
    participant OrderAgg as 🟨 PurchaseOrder
    participant OrderRepo as 🗄️ OrderJpaRepository
    participant OutboxPub as 🟪 OutboxDomainEventPublisher
    participant OutboxTable as 🗄️ outbox_events
    participant Relay as ⚙️ OutboxEventRelay<br/>(Scheduled / AfterCommit)
    participant InvListener as 🎧 InventoryEventListener<br/>(@EventListener Idempotente)
    participant InvItem as 🟨 InventoryItem

    Cliente->>Controller: POST /api/orders (customerId, items)
    Controller->>Decorator: execute(CreateOrderCommand)
    activate Decorator
    Decorator->>Service: execute(command)
    Service->>OrderAgg: create(orderId, customerId, items)
    OrderAgg-->>OrderAgg: Validar INV-06 (items > 0)
    OrderAgg-->>OrderAgg: Registrar OrderRegisteredEvent
    OrderAgg-->>Service: PurchaseOrder (PENDIENTE)
    Service->>OrderRepo: save(order)
    Service-->>Decorator: PurchaseOrder
    Decorator->>OrderAgg: pullDomainEvents()
    OrderAgg-->>Decorator: List[OrderRegisteredEvent]
    Decorator->>OutboxPub: publish(OrderRegisteredEvent)
    OutboxPub->>OutboxTable: INSERT outbox_events (status = PENDING)
    deactivate Decorator
    %% Fin de la transacción ACID del módulo Ordering
    Controller-->>Cliente: 201 Created (PurchaseOrder DTO)

    %% Transmisión asíncrona / After-Commit
    Relay->>OutboxTable: SELECT WHERE status = 'PENDING'
    Relay->>InvListener: onOrderRegistered(OrderRegisteredEvent)
    activate InvListener
    InvListener->>InvListener: Verificar idempotencia (existsByEventId)
    InvListener->>InvItem: reserveStock(productId, quantity)
    InvItem-->>InvItem: Validar INV-03 (disponible >= solicitado)
    InvItem-->>InvListener: StockReservadoEvent
    InvListener->>OutboxTable: INSERT processed_events (eventId)
    deactivate InvListener
    Relay->>OutboxTable: UPDATE outbox_events SET status = 'PROCESSED'
```

---

## 2. Modificación de Pedido Pendiente y Reajuste de Reserva

El cliente actualiza cantidades antes de la confirmación formal del pedido (`INV-04`).

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as 👤 Cliente Mayorista
    participant Controller as 🌐 OrderController
    participant Decorator as 🛡️ ModifyOrderUseCaseDecorator<br/>(@Transactional)
    participant OrderAgg as 🟨 PurchaseOrder
    participant OutboxPub as 🟪 OutboxDomainEventPublisher
    participant Relay as ⚙️ OutboxEventRelay
    participant InvListener as 🎧 InventoryEventListener
    participant InvItem as 🟨 InventoryItem

    Cliente->>Controller: PUT /api/orders/{id}/items (newItems)
    Controller->>Decorator: execute(ModifyOrderCommand)
    activate Decorator
    Decorator->>OrderAgg: modifyItems(newItems)
    OrderAgg-->>OrderAgg: Validar INV-04 (status == PENDING)
    OrderAgg-->>OrderAgg: Registrar OrderModifiedEvent
    Decorator->>OutboxPub: publish(OrderModifiedEvent)
    deactivate Decorator
    Controller-->>Cliente: 200 OK (PurchaseOrder Modificado)

    Relay->>InvListener: onOrderModified(OrderModifiedEvent)
    activate InvListener
    InvListener->>InvItem: adjustReservation(productId, deltaQuantity)
    InvItem-->>InvItem: Recalcular reservedStock
    InvListener->>InvListener: Registrar en processed_events
    deactivate InvListener
```

---

## 3. Rechazo de Invariante de Dominio: RFC 9457 ProblemDetail (422 Unprocessable)

Cuando una operación viola reglas duras de negocio (ej. modificar una orden que ya fue confirmada o despachada).

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as 👤 Cliente Mayorista
    participant Controller as 🌐 OrderController
    participant Decorator as 🛡️ ModifyOrderUseCaseDecorator
    participant Service as ⚙️ ModifyOrderService
    participant OrderAgg as 🟨 PurchaseOrder
    participant Advice as 🌐 GlobalExceptionHandler<br/>(@RestControllerAdvice)

    Cliente->>Controller: PUT /api/orders/{id}/items (newItems)
    Controller->>Decorator: execute(ModifyOrderCommand)
    activate Decorator
    Decorator->>Service: execute(command)
    Service->>OrderAgg: modifyItems(newItems)
    Note over OrderAgg: Estado actual = CONFIRMED.<br/>Violación de INV-04!
    OrderAgg-->>Service: throw OrderNotModifiableException("Solo se pueden modificar pedidos en estado PENDIENTE")
    Service-->>Decorator: propagar excepción (Rollback automático)
    deactivate Decorator
    Decorator-->>Advice: Captura OrderNotModifiableException
    Advice-->>Cliente: 422 Unprocessable Entity<br/>Content-Type: application/problem+json<br/>{ "type": "about:blank", "title": "Invariante Violada", "status": 422, "detail": "..." }
```

---

## 4. Fan-Out Asíncrono hacia Notificaciones y Proyecciones Analíticas

Al confirmarse una orden, el evento `OrderConfirmedEvent` es consumido concurrentemente por los contextos `notification` y `analytics`.

```mermaid
sequenceDiagram
    autonumber
    participant Ordering as 🟨 Contexto Ordering
    participant OutboxTable as 🗄️ outbox_events
    participant Relay as ⚙️ OutboxEventRelay
    participant NotifListener as 🎧 NotificationEventListener
    participant NotifAgg as 🟨 NotificationRecord
    participant AnalyticsListener as 🎧 AnalyticsEventListener
    participant SalesRM as 🟩 WeeklySalesReport (Read Model)

    Ordering->>OutboxTable: INSERT OrderConfirmedEvent (status = PENDING)
    
    par Despacho Fan-Out vía Outbox Relay
        Relay->>NotifListener: handle(OrderConfirmedEvent)
        activate NotifListener
        NotifListener->>NotifListener: Verificar idempotencia
        NotifListener->>NotifAgg: createNotification(email, "Pedido Confirmado", detalles)
        NotifListener-->>Relay: ACK
        deactivate NotifListener
    and
        Relay->>AnalyticsListener: handle(OrderConfirmedEvent)
        activate AnalyticsListener
        AnalyticsListener->>AnalyticsListener: Verificar idempotencia
        AnalyticsListener->>SalesRM: updateWeeklyTotals(amount, items, customerId)
        AnalyticsListener-->>Relay: ACK
        deactivate AnalyticsListener
    end

    Relay->>OutboxTable: UPDATE outbox_events SET status = 'PROCESSED'
```
