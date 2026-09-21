## 🌪️ Protocolo Estricto de Facilitación de Event Storming (Alberto Brandolini)

Cuando el usuario pida modelar un nuevo flujo, feature o proceso de negocio:

1. **PROHIBIDO el orden procedural clásico (Actor ➡️ Comando ➡️ Agregado ➡️ Evento)**:
    - En Event Storming el descubrimiento es **estrictamente guiado por el tiempo y los eventos**.
    - **El punto de partida OBLIGATORIO son los Domain Events (🟧 Naranja en tiempo pasado)**.

2. **Secuencia de Exploración Obligatoria**:
    - **Paso 1 (Timeline de Eventos)**: Identificar la secuencia cronológica de hechos consumados en pasado (ej. `LibroAlquilado`, `PlazoVencido`, `MultaGenerada`).
    - **Paso 2 (Comandos y Actores)**: Mapear qué intención imperativa (🟦 Comando: `AlquilarLibro`) y qué 👤 Actor detonaron cada evento.
    - **Paso 3 (Invariantes)**: Identificar qué reglas de negocio infranqueables (🟥 Invariantes: `Monto > 0`, `Máximo 3 libros`) se deben validar antes de emitir el evento.
    - **Paso 4 (Agregados y Bounded Contexts emergentes)**: Deducir qué 🟨 Aggregate Root encapsula y protege esas invariantes, y proponer los límites de Bounded Context resultantes.
    - **Paso 5 (Políticas y Efectos Secundarios)**: Identificar qué reacciones asíncronas (🟪 Políticas: *"Cuando ocurre [Domain Event 🟧], entonces dispara [Comando 🟦] vía Outbox"*) conectan con otros módulos. Toda política siempre tiene como causa indispensable un Domain Event previo.

---

### 🎨 Tabla Canónica de Nomenclatura, Emojis y Colores

| Elemento | Emoji Canónico | Color Físico (Brandolini) | Definición y Gramática | Estilo Mermaid (Alto Contraste) |
| :--- | :---: | :--- | :--- | :--- |
| **Domain Event** | 🟧 | Naranja | **Hecho consumado en pasado** (`OrderPlaced`). Punto de inicio obligatorio. | `fill:#D84315,stroke:#BF360C,color:#FFFFFF,font-weight:bold` |
| **Command** | 🟦 | Azul | **Intención en imperativo** (`PlaceOrder`). | `fill:#1565C0,stroke:#0D47A1,color:#FFFFFF,font-weight:bold` |
| **Actor / Trigger** | 👤 | Gris / Púrpura | **Rol o disparador externo** que inicia el comando. | `fill:#455A64,stroke:#263238,color:#FFFFFF,font-weight:bold` |
| **Invariante** | 🟥 | Rojo | **Regla dura de negocio** que nunca puede violarse. | `fill:#C62828,stroke:#8E0000,color:#FFFFFF,font-weight:bold` |
| **Aggregate Root** | 🟨 | Amarillo / Dorado | **Raíz transaccional** que protege las invariantes. | `fill:#B78103,stroke:#7F5600,color:#FFFFFF,font-weight:bold` |
| **Policy / Outbox** | 🟪 | Lila / Morado | **Reacción asíncrona** (*"Cuando [Evento], entonces [Comando]"*). | `fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF,font-weight:bold` |
| **Read Model** | 🟩 | Verde | **Vista o proyección de consulta** requerida por el actor. | `fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF,font-weight:bold` |

---

### 🗺️ Reglas Estrictas para Diagramas de Arquitectura, Event Storming y Mapas de Contexto (Mermaid)

#### A. Diagramas Tácticos y de Proceso (`feature-storming.mmd` y `<context>/event-storming.mmd`):
1. **Aislamiento por Casos de Uso (Subgraphs de Flujo Anidados)**:
    - PROHIBIDO mezclar todos los comandos, políticas e invariantes de un Bounded Context en un contenedor plano sin división.
    - Cada caso de uso, intención o reacción debe encapsularse en su propio subgrafo: `subgraph F_<Identificador> ["Flujo: <Nombre Descriptivo>"]`.

2. **Dirección Híbrida Anti-Espagueti (`flowchart LR` + `direction TB`)**:
    - El grafo principal se define como `flowchart LR` para mantener la lectura cronológica/inter-contextos de izquierda a derecha.
    - Todo `subgraph BC_<Nombre>` DEBE declarar `direction TB` al inicio para apilar los flujos de negocio verticalmente de forma compacta y evitar que las aristas se crucen caóticamente.

3. **Descomposición Visual de Agregados por Flujo (PROHIBIDO el "Nodo Central Único")**:
    - PROHIBIDO hacer converger todos los comandos de un Bounded Context hacia un único nodo compartido (`AG1`, `AG2`), pues genera un antipatrón visual de cuello de botella en estrella donde colisionan todas las flechas (efecto espagueti).
    - Se DEBE instanciar un nodo de Agregado único y contextualizado por cada flujo o fase del ciclo de vida (ej: `AG2_Acordar["🟨 Prestamo (Creación)"]:::aggregate`, `AG2_Vencer["🟨 Prestamo (Vencimiento)"]:::aggregate`, `AG2_Devolver["🟨 Prestamo (Devolución)"]:::aggregate`).

4. **Proyecciones de Read Model desde Eventos (Pureza CQRS)**:
    - PROHIBIDO conectar modelos de lectura (`:::readModel`) directamente al Agregado (`AG -.-> RM`).
    - En arquitectura reactiva/CQRS, las vistas de consulta son proyecciones alimentadas por hechos consumados: DEBEN conectarse siempre desde los Domain Events (ej: `E6 -. Proyecta .-> RM1["🟩 ReadModel: Ranking"]:::readModel`).

5. **Materialización de Eventos de Dominio (PROHIBIDO eventos solo en aristas)**:
    - En Mermaid, `classDef` **nunca** aplica estilos a los textos de las aristas o conectores (`-->|"label"|`).
    - Todo Domain Event transmitido entre contextos **DEBE** materializarse como nodo explícito con clase `:::event` (🟧 Naranja `#D84315`).

6. **Reacciones Asíncronas e Inter-Contexto vía Outbox**:
    - Las reacciones entre contextos desacoplados deben especificarse al final del diagrama con aristas punteadas: `EventoOrigen -. Outbox .-> PoliticaDestino`.

7. **Ciclo Causal Completo de Políticas y Eventos Detonantes (PROHIBIDO saltarse Eventos Detonantes, Comandos o Eventos Resultantes)**:
    - Una **Política** (🟪) representa una reacción causal: *"Cuando [Evento X], ENTONCES ejecuta [Comando Y]"*.
    - **PROHIBIDO dejar Políticas huérfanas sin evento detonante**: Ninguna Política puede figurar como nodo de inicio sin estímulo. Toda Política (🟪) DEBE estar precedida obligatoriamente por el **Domain Event** (🟧) que la desencadena (`EventoDetonante --> Politica`).
    - **Eventos Externos en Contextos Consumidores (`<context>/event-storming.mmd`)**: Cuando un Bounded Context actúa como consumidor (Downstream) y reacciona a eventos de otro contexto, el diagrama individual del contexto DEBE materializar el Domain Event externo como nodo inicial del flujo indicando su procedencia (ej. `EV_Ext_TurnoReservado["🟧 TurnoReservado\n(Ext: Agendamiento)"]:::event --> POL_Notif_Reserva`). PROHIBIDO iniciar el flujo directamente en la Política lila sin el evento naranja de entrada.
    - **Autonomía en Contextos Productores (`<context>/event-storming.mmd`)**: Los diagramas individuales de contexto emisor (Upstream) deben preservar la autonomía de límites (Bounded Context). Solo modelan los eventos locales que emiten. PROHIBIDO incluir políticas, comandos o agregados de otros contextos consumidores dentro de su propio diagrama aislado (dichas interacciones pertenecen exclusivamente a `feature-storming.mmd` o al `<context>/event-storming.mmd` del contexto receptor).
    - **PROHIBIDO conectar una Política directamente al Agregado (`P --> AG`)**: Toda Política DEBE invocar un **Comando** (`P --> C`), el cual se procesa en el Agregado (`C --> AG`) y culmina emitiendo su respectivo **Domain Event** (`AG --> E`).
    - Los Read Models del contexto consumidor deben proyectarse conectando todos los eventos relevantes (`E1 & E2 -. Proyecta .-> RM`).

#### B. Mapa de Contextos Estratégico (`system-context-map.mmd` - DDD Estratégico Puro):
1. **Nivel Estratégico vs. Táctico (Separación Estricta)**:
    - `system-context-map.mmd` modela **únicamente la relación estratégica y organizacional entre Bounded Contexts** (Eric Evans).
    - **PROHIBIDO** pintar tablas físicas (`outbox_events`), workers de relay, listeners de infraestructura, agregados internos o listas de eventos individuales (estos pertenecen a `feature-storming.mmd` y `docs/architecture/transactional-outbox-workflow.md`).
2. **Relaciones Upstream (U) / Downstream (D) y Patrones Canónicos**:
    - Cada nodo representa un Bounded Context con su rol: `[Upstream / OHS / PL]` o `[Downstream / Customer / ACL]`.
    - Las aristas definen el contrato de integración: ej. `-->|"Published Language\n(Domain Events v1)"|`.
    - Patrones soportados: **OHS** (Open Host Service), **PL** (Published Language), **ACL** (Anti-Corruption Layer), **Customer / Supplier**, **Conformist**, **Shared Kernel**.
    - Incluir nota de glosario al pie: `note1[/"OHS = Open Host Service\nPL = Published Language\nACL = Anti-Corruption Layer"/]:::note`.

#### C. Coloreado Canónico Completo:
- Agregados: `:::aggregate` (🟨 Dorado `#B78103`).
- Contextos Estratégicos: `:::context` (Fondo blanco `#FFFFFF` con borde Slate `#334155`).
- Outbox Engine y Workers: `:::outbox` (🟪 Morado `#6A1B9A` - solo en diagramas de infraestructura).
- Listeners / Event Handlers: `:::listener` (🎧 Slate `#F1F5F9` con borde `#475569`).
- Read Models: `:::readModel` (🟩 Verde `#2E7D32`).

---

## 🏛️ Reglas Arquitectónicas: Monolito Modular con Minimal DDD (Java 25)

1. **Pureza del Dominio**:
    - El paquete `*.domain.*` está estrictamente restringido a Java estándar.
    - PROHIBIDO importar `org.springframework.*`, `jakarta.persistence.*`, o Hibernate en el dominio. Está permitido el uso de `Lombok`

2. **Límites de Agregados y Bounded Contexts**:
    - Los Agregados de diferentes Bounded Contexts se referencian exclusivamente mediante Value Objects de Identidad (e.g., `CustomerId`, `ProductId`).
    - PROHIBIDO el uso de `@ManyToOne` entre tablas de diferentes Bounded Contexts.

3. **Manejo de Transacciones (Patrón Decorator)**:
    - Los servicios en `domain/service/` son POJOs puros sin anotaciones `@Service` ni `@Transactional`.
    - La transaccionalidad se maneja en `infrastructure/decorator/*UseCaseDecorator.java` usando el Patrón Decorator.

4. **Resiliencia de Eventos (Transactional Outbox & Idempotent Consumer)**:
    - La comunicación inter-módulos para efectos secundarios debe usar el patrón Transactional Outbox.
    - PROHIBIDO despachar eventos críticos a memoria volátil sin persistencia previa en la misma transacción ACID.
    - Los consumidores de eventos deben ser idempotentes registrando el `eventId` procesado para tolerar reintentos (At-Least-Once Delivery).
    - Para el flujo de trabajo del desarrollador y plantillas de código, consultar [docs/architecture/transactional-outbox-workflow.md](docs/architecture/transactional-outbox-workflow.md).

5. **Verificación Viva con `requests.http` y H2 en Memoria**:
    - Al finalizar la construcción táctica de cualquier endpoint o Bounded Context, el agente DEBE actualizar o generar el archivo `requests.http` (o `request.http`) en la raíz del proyecto con solicitudes listas para ejecutar.
    - Se deben incluir:
        - **Happy Path**: Creación y consulta exitosa.
        - **Violación de Invariantes**: Casos de error de negocio esperando RFC 9457 `ProblemDetail` (400 / 422).
        - **Comprobación de Eventos / Outbox**: Consultas de cambio de estado o disparos de efectos secundarios.
    - Se deben sembrar los datos iniciales necesarios en `src/main/resources/data.sql` (compatible con H2) para que la base de datos en memoria arranque con datos de prueba listos para usar.
    - El reporte final debe guiar al desarrollador con instrucciones claras para levantar la aplicación (`./gradlew bootRun`), ejecutar las peticiones en `requests.http` y consultar la consola H2 (`http://localhost:8080/h2-console`).

6. **Documentación Viva Obligatoria del MVP (Living Documentation Portal)**:
    - Al culminar la implementación táctica de cualquier módulo o feature, el agente DEBE mantener al día el paquete documental del proyecto:
        - **Diagramas de Secuencia** (`docs/architecture/sequence-diagrams.md`): Diagramas Mermaid `sequenceDiagram` para los casos de uso principales (Happy Path con Outbox, efectos secundarios asíncronos Fan-Out, y rechazo de invariantes RFC 9457 `ProblemDetail`).
        - **Guía de Flujo Transactional Outbox** (`docs/architecture/transactional-outbox-workflow.md`): Mantener explicada la fontanería de resiliencia y la guía para productores y consumidores.
        - **Portal Central `README.md`**: Diagrama de arquitectura global, desglose de Bounded Contexts, tabla de datos semilla y catálogo navegable de enlaces a toda la documentación.
        - **Documentación de Frontend** (`frontend/README.md`, si el proyecto incluye UI): Mapeo de carpetas con Bounded Contexts, reactividad (Signals), sistema de temas (Dark/Light) y proxy de desarrollo.