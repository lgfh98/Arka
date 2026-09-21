# 📦 Guía de Flujo Transactional Outbox & Idempotent Consumer (Arka Monolith)

Este documento describe el mecanismo de resiliencia implementado en el backend de Arka para la comunicación inter-módulos desacoplada y confiable (*At-Least-Once Delivery* con *Exactly-Once Processing* semántico).

---

## 🏛️ Por qué Transactional Outbox en Monolito Modular

En una arquitectura de Bounded Contexts desacoplados dentro de un monolito:
1. **Evitar el "Dual-Write Problem"**: Si un caso de uso modifica la base de datos y publica un evento a un broker de mensajería (o bus en memoria) en pasos separados, una caída del sistema entre ambas operaciones deja al sistema en un estado inconsistente.
2. **Atomicidad ACID**: El agregado del dominio y el evento de dominio se persisten en la **misma transacción atómica de base de datos**. Si la transacción hace commit, el evento está 100% garantizado en disco. Si hace rollback, el evento se descarta automáticamente.
3. **Preparado para Microservicios (AWS SNS/SQS)**: El patrón Transactional Outbox permite que la arquitectura evolucione a microservicios distribuidos sin cambiar la semántica del dominio.

---

## 🗄️ Esquema de Persistencia del Kernel

### 1. Tabla de Eventos Salientes (`outbox_events`)

```sql
CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'PROCESSED', 'FAILED'
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
```

### 2. Tabla de Deduplicación en Consumidores (`processed_events`)

Cada Bounded Context consumidor mantiene un registro de idempotencia para descartar reintentos:

```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    consumer_context VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

---

## 🛠️ Guía para Desarrolladores

### A. Rol Productor (Cómo emitir un evento)

1. **En el Aggregate Root (Dominio Puro)**:
   El agregado acumula eventos en una lista interna durante la ejecución de sus métodos de negocio:
   ```java
   public void confirm() {
       if (this.status != OrderStatus.PENDING) {
           throw new OrderNotModifiableException("Solo se pueden confirmar órdenes en estado PENDIENTE");
       }
       this.status = OrderStatus.CONFIRMED;
       recordEvent(new OrderConfirmedEvent(UUID.randomUUID(), this.id, Instant.now()));
   }
   ```

2. **En el Decorador Transaccional (`infrastructure/decorator`)**:
   El decorador `@Transactional` envuelve al caso de uso de dominio. Tras el guardado del agregado, extrae los eventos mediante `aggregate.pullDomainEvents()` y los delega a `OutboxDomainEventPublisher`:
   ```java
   @Transactional
   @Override
   public PurchaseOrder confirmOrder(ConfirmOrderCommand command) {
       PurchaseOrder order = targetService.confirmOrder(command);
       order.pullDomainEvents().forEach(outboxPublisher::publish);
       return order;
   }
   ```

### B. Rol Despachador (Outbox Event Relay)

El worker `OutboxEventRelay` opera bajo dos estrategias complementarias:
1. **Síncrona Inmediata**: `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` dispara el envío en milisegundos tras confirmarse la transacción del productor.
2. **Reintento Periódico**: `@Scheduled(fixedDelay = 5000)` escanea registros en estado `PENDING` para recuperarse de caídas o reintentos pendientes.
3. Cada evento se procesa en una transacción aislada (`@Transactional(propagation = Propagation.REQUIRES_NEW)`).

### C. Rol Consumidor Idempotente

En cada Bounded Context receptor:
1. Escucha eventos mediante `@EventListener`.
2. Verifica si el `eventId` ya existe en `processed_events`.
3. Si existe, lo ignora (log de duplicado).
4. Si no existe, ejecuta el caso de uso y registra el `eventId` en la misma transacción.

```java
@Component
public class InventoryEventListener {

    private final ReserveStockUseCase reserveStockUseCase;
    private final ProcessedEventJpaRepository processedEventRepo;

    @Transactional
    @EventListener
    public void onOrderRegistered(OrderRegisteredEvent event) {
        if (processedEventRepo.existsById(event.eventId())) {
            log.info("Evento ya procesado previamente: {}", event.eventId());
            return;
        }

        // Ejecución de la lógica de negocio
        reserveStockUseCase.reserveStock(new ReserveStockCommand(event.orderId(), event.items()));

        // Registro de idempotencia
        processedEventRepo.save(new ProcessedEventJpaEntity(event.eventId(), "inventory", Instant.now()));
    }
}
```
