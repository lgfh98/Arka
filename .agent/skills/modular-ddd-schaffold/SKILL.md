---
name: modular-ddd-scaffold
description: >-
  Use this skill when designing, validating, or scaffolding new features or modules
  following the Minimal DDD (Domain + Infrastructure) and Modular Monolith pattern
  with pure Java 25, Transactional Decorators, and Transactional Outbox EDA.
---

# Modular Monolith DDD Scaffolding Skill

Este skill guía al agente para diseñar y generar código siguiendo la arquitectura de referencia
de Monolito Modular con Minimal DDD y Transactional Outbox (Paso 5).

## Estructura de Salida por Bounded Context:
```text
<context_name>/
├── domain/                                    # 100% Java Estándar (Cero Spring/JPA)
│   ├── model/
│   │   ├── aggregate/                         # Aggregate Root con métodos de negocio y fábrica
│   │   ├── entity/                            # Entidades internas del agregado (si aplica)
│   │   ├── valueobject/                       # Records inmutables (Id, Money, Quantity, Email)
│   │   └── event/                             # DomainEvent inmutables (con eventId)
│   ├── port/
│   │   ├── in/                                # UseCase interfaces y Commands/Queries
│   │   └── out/                               # Repository interfaces y Publishers
│   ├── service/                               # POJOs puros que implementan los UseCases
│   └── exception/                             # DomainException y errores de regla de negocio
└── infrastructure/                            # Adaptadores Tecnológicos
    ├── config/                                # Spring @Configuration cableando Decorators
    ├── decorator/                             # Decoradores @Transactional de los UseCases
    ├── persistence/                           # Spring Data JPA (Entity, Repository, Mapper, Adapter)
    │   ├── entity/ProcessedEventJpaEntity.java# Control de Idempotencia del consumidor
    │   └── repository/ProcessedEventJpaRepository.java
    ├── event/                                 # @EventListener idempotente que reacciona a eventos
    └── web/                                   # Spring MVC (@RestController, DTOs, WebMapper)

shared/infrastructure/outbox/                  # KERNEL DE RESILIENCIA (At-Least-Once Delivery)
├── OutboxStatus.java                          # PENDING, PROCESSED, FAILED
├── OutboxEventJpaEntity.java                  # Tabla outbox_events
├── OutboxEventJpaRepository.java              # Spring Data JPA
├── OutboxDomainEventPublisher.java            # Persiste el evento en la misma tx de BD
├── OutboxEventProcessor.java                  # Despacho aislado en tx REQUIRES_NEW
└── OutboxEventRelay.java                      # Worker @Scheduled + AfterCommit sync
```

## Nomenclatura Canónica de Event Storming (Alberto Brandolini):

| Elemento | Emoji | Color Físico | Definición y Gramática | Estilo Mermaid (Alto Contraste) |
| :--- | :---: | :--- | :--- | :--- |
| **Domain Event** | 🟧 | Naranja | **Hecho consumado en pasado** (`OrderPlaced`). Punto de inicio obligatorio. | `fill:#D84315,stroke:#BF360C,color:#FFFFFF,font-weight:bold` |
| **Command** | 🟦 | Azul | **Intención en imperativo** (`PlaceOrder`). | `fill:#1565C0,stroke:#0D47A1,color:#FFFFFF,font-weight:bold` |
| **Actor / Trigger** | 👤 | Gris / Slate | **Rol o sistema externo** que inicia el comando. | `fill:#455A64,stroke:#263238,color:#FFFFFF,font-weight:bold` |
| **Invariante** | 🟥 | Rojo | **Regla dura de negocio** que nunca puede violarse. | `fill:#C62828,stroke:#8E0000,color:#FFFFFF,font-weight:bold` |
| **Aggregate Root** | 🟨 | Amarillo / Dorado | **Raíz transaccional** que protege las invariantes. | `fill:#B78103,stroke:#7F5600,color:#FFFFFF,font-weight:bold` |
| **Policy / Outbox** | 🟪 | Lila / Morado | **Reacción asíncrona** (*"Cuando [Evento], entonces [Comando]"*). | `fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF,font-weight:bold` |
| **Read Model** | 🟩 | Verde | **Proyección o vista de consulta** requerida por el actor. | `fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF,font-weight:bold` |

### Estilos Visuales y Reglas de Diagramación en Mermaid:

```mermaid
flowchart LR
    classDef event fill:#D84315,stroke:#BF360C,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef command fill:#1565C0,stroke:#0D47A1,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef actor fill:#455A64,stroke:#263238,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef invariant fill:#C62828,stroke:#8E0000,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef aggregate fill:#B78103,stroke:#7F5600,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef policy fill:#6A1B9A,stroke:#4A148C,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef readModel fill:#2E7D32,stroke:#1B5E20,stroke-width:2px,color:#FFFFFF,font-weight:bold;
```

#### 📐 Reglas Estructurales Obligatorias para `feature-storming.mmd` y `<context>/event-storming.mmd`:
1. **Subgraphs Anidados por Flujo de Negocio**: Cada Bounded Context debe subdividirse en subgrafos por caso de uso (`subgraph F_<Identificador> ["Flujo: <Descripción>"]`). PROHIBIDO mezclar comandos y eventos en una única caja plana.
2. **Dirección Híbrida (`flowchart LR` + `direction TB`)**: Declarar `direction TB` dentro de cada Bounded Context para que los flujos se apilen ordenadamente en vertical, manteniendo la lectura general y las conexiones Outbox de izquierda a derecha.
3. **Descomposición Visual de Agregados**: PROHIBIDO un único nodo central compartido de Agregado (`AG1`). Debe instanciarse un nodo de Agregado contextualizado por cada flujo/fase (`AG_<Ctx>_<Accion>["🟨 Agregado (Fase)"]`) para evitar marañas de flechas cruzadas (antipatrón de cuello de botella).
4. **Read Models desde Eventos (CQRS)**: Los `:::readModel` se proyectan SIEMPRE desde Domain Events (`E -. Proyecta .-> RM`), NUNCA directamente desde el Agregado.
5. **Reacciones Outbox al Pie (Diagramas Cross-Context / Feature)**: Las conexiones asíncronas entre contextos se ubican al final: `E -. Outbox .-> P`.
6. **Ciclo Causal Completo de Políticas y Eventos Detonantes**:
    - Toda Política (🟪) DEBE estar precedida por el **Domain Event** (🟧) que la desencadena (`E --> P`). PROHIBIDO dejar políticas huérfanas sin evento detonante.
    - En diagramas individuales de contexto consumidor (`<context>/event-storming.mmd`), materializar explícitamente el Domain Event externo como nodo inicial del flujo (ej. `EV_Ext_*["🟧 Evento (Ext: Origen)"]:::event --> POL_*`).
    - En diagramas individuales de contexto emisor (`<context>/event-storming.mmd`), mantener la autonomía: solo emiten sus eventos propios, sin incluir políticas de otros contextos.
    - Toda Política (🟪) debe detonar un Comando (🟦), el cual se ejecuta en el Agregado (🟨) y emite su respectivo Domain Event (🟧). PROHIBIDO saltarse el comando o el evento resultante. Los Read Models se proyectan desde todos los eventos resultantes del contexto.

#### 🗺️ Reglas Obligatorias para `system-context-map.mmd` (Strategic DDD Puro):
1. **Nivel Estratégico (Eric Evans)**: Modela exclusivamente los Bounded Contexts y sus relaciones estratégicas/organizacionales. PROHIBIDO incluir tablas de BD, workers de outbox, agregados o eventos individuales (eso pertenece a `feature-storming.mmd` y `docs/architecture/transactional-outbox-workflow.md`).
2. **Relaciones Upstream / Downstream**: Declarar explícitamente en cada nodo si el contexto es `[Upstream / OHS / PL]` o `[Downstream / Customer / ACL]`.
3. **Contratos en Aristas**: Conectar contextos con su lenguaje de publicación: ej. `-->|"Published Language\n(Domain Events v1)"|`.
4. **Glosario en Nota**: Incluir nota explicativa con los acrónimos (`OHS = Open Host Service`, `PL = Published Language`, `ACL = Anti-Corruption Layer`).

## Procedimiento Paso a Paso (Modo Pipeline Autónomo):

### Paso 1: Sesión Interactiva de Event Storming (Descubrimiento del Dominio)
> ⚠️ **REGLA METODOLÓGICA CRÍTICA**: PROHIBIDO empezar por Actores o Comandos. El pensamiento procedural (Actor -> Comando -> Agregado -> Evento) viola Event Storming. La sesión se conduce estrictamente en este orden cronológico:
1. **Línea Temporal de Domain Events (🟧 Naranja)**: Identificar primero todos los hechos consumados en tiempo pasado (`LibroAlquilado`, `PlazoVencido`).
2. **Commands (🟦 Azul) y Actores (👤 Gris)**: Mapear en retrospectiva qué comandos detonaron los eventos y qué actor o timer los originó.
3. **Invariantes (🟥 Rojo)**: Identificar qué reglas de consistencia de negocio debieron validarse antes de que el evento ocurriera.
4. **Agregados (🟨 Dorado) y Bounded Contexts**: Agrupar los comandos e invariantes para descubrir el Aggregate Root responsable y proponer los Bounded Contexts emergentes.
5. **Políticas / Outbox (🟪 Morado)**: Mapear reacciones asíncronas (*"Cuando ocurre [Domain Event 🟧], entonces dispara [Comando 🟦] vía Outbox"*) entre contextos desacoplados. Toda política tiene siempre identificado su evento detonante previo.

### Paso 2: Ejecución Autónoma del Pipeline Táctico (Ante un simple "Aprobado / Constrúyelo")
Cuando el usuario aprueba el Event Storming (ej. *"Aprobado"*, *"Constrúyelo"*, *"Adelante"*), **el agente toma control total y ejecuta de forma autónoma la siguiente secuencia completa sin pedir prompts intermedios**:

1. **Persistencia Automática de Arquitectura (Living Documentation)**:
    - Crea `docs/architecture/features/<ticket>/feature-storming.mmd` siguiendo estrictamente las **Reglas Estructurales Obligatorias** (subgraphs por flujo `subgraph F_*`, `direction TB` por contexto, agregados descompuestos por flujo, proyecciones CQRS desde eventos y reacciones Outbox inter-contextos).
    - Crea o actualiza `docs/architecture/contexts/<context>/event-storming.mmd` para cada Bounded Context identificado aplicando la misma modularidad por flujo.
    - Crea o actualiza `docs/architecture/system-context-map.mmd` como **Mapa de Contextos Estratégico Puro (Strategic DDD)**: enfocado exclusivamente en relaciones Upstream/Downstream (U/D) y patrones estratégicos (OHS, PL, ACL, Customer/Supplier), sin detalles tácticos ni tablas de BD.
    - Crea o actualiza `docs/architecture/sequence-diagrams.md`: Diagramas de secuencia Mermaid (`sequenceDiagram`) detallando los flujos end-to-end de los casos de uso (Happy Path con Transactional Outbox, efectos asíncronos Fan-Out hacia otros módulos, y rechazo de invariantes RFC 9457 `ProblemDetail`).
    - Crea o actualiza `docs/architecture/transactional-outbox-workflow.md`: Guía de referencia sobre la infraestructura de resiliencia y el manual de publicación/consumo de eventos para desarrolladores.

2. **Scaffolding Táctico Incremental (Paso 5)**:
    - **Si es 1 contexto**: Construye el dominio puro (`model`, `port`, `service`), adaptadores (`decorator`, `persistence`, `event`, `web`) y el kernel `shared/infrastructure/outbox`.
    - **Si son múltiples contextos**:
        - **Fase A (Productor + Kernel)**: Construye el contexto emisor y el kernel `shared/infrastructure/outbox`.
        - **Fase B (Consumidores)**: Construye sucesivamente cada contexto receptor generando su agregación y su `@EventListener` idempotente (`ProcessedEventJpaEntity`) conectado a los eventos del productor.

3. **Generación de Datos de Prueba en H2 y Solicitudes HTTP (`requests.http`)**:
    - Inserta o actualiza datos semilla en `src/main/resources/data.sql` (usando `MERGE INTO ... KEY(id)`) con IDs de referencia conocidos para que la base de datos en memoria H2 arranque con datos listos para probar.
    - Agrega o actualiza en el archivo raíz `requests.http` (o `request.http`) las peticiones HTTP listas para probar con comentarios explicativos:
        - **Happy Path**: POST/GET de creación y consulta exitosa.
        - **Violación de Invariantes**: Peticiones que violan reglas de negocio intencionalmente para comprobar el manejo de excepciones RFC 9457 `ProblemDetail` (400 / 422).
        - **Efectos Secundarios / Outbox**: Comprobación de cambio de estado y procesamiento asíncrono.

4. **Verificación y Calidad Automática**:
    - Ejecuta `./gradlew compileJava` en segundo plano para verificar compilación libre de errores. Si hay fallos de import o tipos, los corrige automáticamente.
    - Si se requiere implementar o robustecer la pirámide de pruebas automatizadas (unitarias de dominio, integración y contratos/idempotencia), aplica las directrices del skill complementario `modular-ddd-testing`.

5. **Consolidación del Portal MVP y Reporte Final al Desarrollador**:
    - Actualiza el `README.md` principal en la raíz transformándolo en el **Portal Central del MVP**: diagrama de arquitectura de bloques, responsabilidades por Bounded Context, tabla de datos semilla interactivos y catálogo navegable de enlaces a toda la documentación técnica generada.
    - Si el proyecto cuenta con frontend, actualiza `frontend/README.md` detallando la correspondencia con los Bounded Contexts, reactividad (Signals), sistema de temas (Dark/Light) y proxy de desarrollo.
    - Entrega un resumen conciso con enlaces clicables a todos los archivos creados.
    - **Guía de Verificación Inmediata**:
        1. Cómo levantar el servidor backend: `./gradlew bootRun`.
        2. Cómo levantar el frontend (si aplica): `cd frontend && npm start`.
        3. Cómo ejecutar las solicitudes en [`requests.http`](file:///home/lgfh98/personal/patio-de-juegos-ddd-clean-arch/patio-de-juegos-ddd-clean-arch/requests.http) con REST Client o IntelliJ HTTP Client.
        4. Cómo acceder a la consola H2 (`http://localhost:8080/h2-console`) con JDBC URL en memoria para inspeccionar tablas y `outbox_events`.
    - Comando de commit listo para Git:
      ```bash
      git add docs/ src/ frontend/ README.md requests.http
      git commit -m "feat(<context>): implementacion completa con living documentation, tests http y portal mvp"
      ```