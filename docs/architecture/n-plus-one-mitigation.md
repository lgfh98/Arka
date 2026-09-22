# ⚡ Guía de Mitigación del Problema N+1 y Rendimiento de Persistencia

En la arquitectura de **Arka** (Monolito Modular con Minimal DDD en Java 25), el acceso a datos debe garantizar tanto la **pureza de los límites transaccionales del Dominio** como la **eficiencia en el consumo de recursos de red y base de datos (I/O)**.

Esta guía documenta cómo se aborda y resuelve el problema de consultas **N+1**, la distinción entre el diseño del Agregado DDD y la ejecución SQL en JPA, y las directrices para futuros módulos.

---

## 1. El Problema N+1 en Arquitecturas DDD con JPA

El problema de consultas **N+1** surge cuando un Object-Relational Mapper (ORM como Hibernate) ejecuta una consulta inicial para obtener una lista de $N$ registros padre, y a continuación ejecuta automáticamente $N$ consultas secundarias independientes para traer los datos asociados a cada registro hijo.

```text
Query 1 (Inicial): SELECT * FROM ordering.purchase_orders WHERE customer_id = ?;  --> Retorna N órdenes
Query 2 .. N+1:    SELECT * FROM ordering.order_items WHERE order_id = ?;          --> 1 query por cada orden
```

En entornos de producción, esto genera una degradación severa del rendimiento debido a la **latencia de red acumulada (roundtrips)** y la saturación del pool de conexiones JDBC.

---

## 2. Los Dos Niveles Arquitectónicos en Arka

### A. Nivel Inter-Contexto (DDD Estratégico): Blindado por Diseño
La regla fundamental del proyecto estipulada en [`AGENTS.md`](file:///home/lgfh98/personal/Arka/Arka/AGENTS.md):
> *"Los Agregados de diferentes Bounded Contexts se referencian exclusivamente mediante Value Objects de Identidad (ej. `CustomerId`, `ProductId`). PROHIBIDO el uso de `@ManyToOne` entre tablas de diferentes Bounded Contexts."*

Gracias a esto:
* No existen relaciones foráneas JPA navegables entre módulos distintos (`ordering` no conoce la entidad `Product` ni `Customer`).
* Es **imposible** que una consulta a un contexto dispare cargas perezosas o consultas N+1 en cascada hacia otros Bounded Contexts.

### B. Nivel Intra-Contexto (Persistencia Táctica del Agregado): El Reto de las Colecciones
Dentro de un mismo Bounded Context, un Agregado como [`PurchaseOrder`](file:///home/lgfh98/personal/Arka/Arka/src/main/java/com/arka/backend/ordering/domain/model/aggregate/PurchaseOrder.java) contiene una lista de [`OrderItem`](file:///home/lgfh98/personal/Arka/Arka/src/main/java/com/arka/backend/ordering/domain/model/entity/OrderItem.java). 

Aquí es donde se presentaba el riesgo N+1 cuando se consultaban listas o estados masivos con `FetchType.EAGER` o mapeadores perezosos sin `JOIN`.

---

## 3. Aclaración Clave: "Uno a Pocos" (DDD) vs. Consultas SQL (JPA)

Existe una confusión habitual en la industria respecto a la relación "Uno a Pocos":

* **En el Dominio (DDD - Eric Evans / Vaughn Vernon)**:
  Una orden normalmente contiene entre 1 y 30 ítems (*uno a pocos*). Por ello, **es correcto y recomendado** que `PurchaseOrder` sea el Aggregate Root y posea a sus `OrderItem` en memoria para gobernar las invariantes transaccionales (`INV-04`, `INV-05`, `INV-06`).
* **En la Base de Datos (JPA / SQL)**:
  El problema N+1 **no depende de cuántos ítems tenga una orden, sino de cómo se recupera una lista de órdenes**. Si un cliente consulta 50 órdenes y cada orden tiene solo 2 ítems, Hibernate lanzaba **51 consultas SQL**:
  * 1 consulta para las 50 órdenes.
  * 50 consultas individuales para traer los 2 ítems de cada orden.

**Solución**: Mantener el Agregado completo en el Dominio, pero instruir a JPA para que traiga la orden y sus ítems en **1 sola consulta SQL mediante un `JOIN`**.

---

## 4. Estrategia de Mitigación Implementada

### Pilar 1: Mapeo Perezoso por Defecto (`FetchType.LAZY`)
En todas las entidades JPA que representan Agregados con colecciones, la relación se declara como `LAZY` para impedir que Hibernate ejecute subconsultas ansiosas no planificadas:

```java
// OrderJpaEntity.java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@Builder.Default
private List<OrderItemJpaEntity> items = new ArrayList<>();

// CartJpaEntity.java
@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@Builder.Default
private List<CartItemJpaEntity> items = new ArrayList<>();
```

---

### Pilar 2: Carga Ansiosa Dinámica con `@EntityGraph`
En los repositorios Spring Data JPA, se anota `@EntityGraph(attributePaths = {"items"})` en todos los métodos que deben retornar el Agregado de dominio completamente hidratado.

Spring Data JPA traduce esto a un **`LEFT OUTER JOIN`** en SQL estándar:

```java
// OrderJpaRepository.java
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<OrderJpaEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findAll();

    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    List<OrderJpaEntity> findByStatus(OrderStatus status);
}
```

```java
// CartJpaRepository.java
@Repository
public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<CartJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"items"})
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    List<CartJpaEntity> findByStatus(CartStatus status);
}
```

#### Comparación de Consultas Generadas:
| Escenario | Antes (`EAGER` sin Join) | Ahora (`LAZY` + `@EntityGraph`) |
| :--- | :--- | :--- |
| **`findByCustomerId` (50 órdenes)** | 1 query + 50 queries = **51 queries SQL** | **1 query SQL** con `LEFT OUTER JOIN` |
| **Scheduler Carritos Abandonados (100 carritos)** | 1 query + 100 queries = **101 queries SQL** | **1 query SQL** con `LEFT OUTER JOIN` |
| **`findById` (1 orden)** | 1 query + 1 subquery secundaria | **1 query SQL** directa |

---

### Pilar 3: Procesamiento por Lote JDBC (Batching)
Para bucles de inserción o actualización (por ejemplo, el scheduler marcando carritos como abandonados o el procesamiento de ítems de órdenes en el listener de inventario), se habilitó el procesamiento batch en [`application.yml`](file:///home/lgfh98/personal/Arka/Arka/src/main/resources/application.yml):

```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
```
* **`batch_size: 25`**: Agrupa sentencias DML en bloques de hasta 25 operaciones antes de enviarlas al socket de BD.
* **`order_inserts` / `order_updates`**: Ordena las sentencias en memoria para maximizar el agrupamiento de batching de Hibernate.

---

### Pilar 4: Consultas de Lectura Masiva vía CQRS (Read Models)
En el Bounded Context de `analytics`, para reportes y tablas de consulta masiva (ventas semanales, abastecimiento), **no se cargan entidades relacionales con colecciones**. Se consultan proyecciones planas desnormalizadas:
* [`CustomerSalesProjectionJpaEntity`](file:///home/lgfh98/personal/Arka/Arka/src/main/java/com/arka/backend/analytics/infrastructure/persistence/entity/CustomerSalesProjectionJpaEntity.java)
* [`ProductSalesProjectionJpaEntity`](file:///home/lgfh98/personal/Arka/Arka/src/main/java/com/arka/backend/analytics/infrastructure/persistence/entity/ProductSalesProjectionJpaEntity.java)
* [`ReplenishmentProjectionJpaEntity`](file:///home/lgfh98/personal/Arka/Arka/src/main/java/com/arka/backend/analytics/infrastructure/persistence/entity/ReplenishmentProjectionJpaEntity.java)

Estas tablas se alimentan asíncronamente desde los **Domain Events** y eliminan por completo la necesidad de joins o colecciones en consultas de reportería.

---

## 5. Verificación Automatizada (Pruebas de Integración Nivel 2)

Para asegurar que las consultas no provoquen `LazyInitializationException` y confirmen la hidratación completa de los agregados en un solo viaje, se mantienen los siguientes tests de persistencia:

* [`OrderPersistenceIntegrationTest.java`](file:///home/lgfh98/personal/Arka/Arka/src/test/java/com/arka/backend/ordering/infrastructure/persistence/OrderPersistenceIntegrationTest.java):
  * Verifica que consultar órdenes por cliente cargue todos los `items` correctamente.
  * Verifica la consulta por `OrderId` individual.
* [`CartPersistenceIntegrationTest.java`](file:///home/lgfh98/personal/Arka/Arka/src/test/java/com/arka/backend/cart/infrastructure/persistence/CartPersistenceIntegrationTest.java):
  * Verifica que consultar carritos por estado (`ACTIVE`) cargue todos los ítems asociados.
  * Verifica la consulta por `customerId`.

---

## 6. Guía para Nuevos Bounded Contexts y Agregados

Al diseñar un nuevo módulo que contenga una colección interna en un Aggregate Root:

1. **Definir el límite de consistencia**: Si la colección es "uno a pocos" (ej. detalles de factura, ítems de cotización), ubicarla dentro del Agregado.
2. **Entidad JPA**: Marcar la relación con `fetch = FetchType.LAZY`. **PROHIBIDO usar `FetchType.EAGER`**.
3. **Repositorio Spring Data JPA**: Anotar con `@EntityGraph(attributePaths = {"<nombre_coleccion>"})` en las firmas de consulta de listas que requieran el agregado hidratado.
4. **Pruebas de Persistencia**: Crear un `*PersistenceIntegrationTest` que valide el guardado y recuperación de la entidad con su colección.
