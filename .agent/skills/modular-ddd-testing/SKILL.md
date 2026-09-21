---
name: modular-ddd-testing
description: >-
  Use this skill when designing, writing, reviewing, or executing tests across the
  Minimal DDD testing pyramid in Java 25: pure domain unit tests (mutation-ready POJOs),
  persistence & Transactional Outbox integration tests, and Bounded Context contract
  & idempotent consumer tests.
---

# Modular DDD Testing Skill (Java 25 & Minimal DDD)

Este skill guía al agente para concebir, estructurar y escribir pruebas automatizadas rigurosas
adaptadas a la arquitectura de **Monolito Modular con Minimal DDD, Transactional Outbox e Idempotent Consumer**.

---

## 🏛️ La Pirámide de Pruebas en Minimal DDD

| Nivel de Test | Qué prueba en la arquitectura | Enfoque y Restricciones Técnicas | Velocidad Objetivo |
| :--- | :--- | :--- | :---: |
| **1. Pruebas de Dominio (Unitarias Puras)** | Agregados, Value Objects, invariantes duras y Domain Services. | **Cero mocks de frameworks** (sin Spring, sin Mockito en agregados). POJOs puros. Pruebas de mutación (PITest / análisis de límites). | `< 10 ms` por test |
| **2. Pruebas de Integración (Tácticas)** | Repositorios JPA, adaptadores de BD, Decoradores `@Transactional` y `OutboxDomainEventPublisher`. | Validar que el agregado y el evento en `outbox_events` se persistan en la **misma transacción atómica**. Validar rollback y esquema H2/JPA. | `< 500 ms` |
| **3. Pruebas de Bounded Context (Contratos & Idempotencia)** | Serialización de eventos en Outbox y consumo en `@EventListener` entre módulos. | Verificar que el payload JSON del evento sea compatible con el consumidor y que la entrega duplicada (At-Least-Once) sea ignorada gracias a `ProcessedEventJpaEntity`. | `< 800 ms` |
| **4. Pruebas de Arquitectura (Gobernanza)** | Reglas estructurales de paquetes y dependencias. | Validar que `*.domain.*` jamás importe Spring ni Jakarta Persistence, y que no existan `@ManyToOne` entre contextos distintos. | `< 1 s` |
| **5. Pruebas de Aceptación E2E (Black-Box & BDD Nativo)** | Flujos completos de negocio desde la API REST (Happy Path, Invariantes RFC 9457 y Outbox). | **BDD Nativo con JUnit 5 + MockMvc** (sin Cucumber ni dependencias frágiles). Enfoque Caja Negra (Black-Box). Peticiones HTTP reales contra DispatcherServlet. | `< 1 s` por suite |

---

## 🧪 Nivel 1: Pruebas de Dominio (Unitarias Puras & Mutation-Ready)

### Reglas Inquebrantables:
1. **Prohibido levantar Spring**: Cero `@SpringBootTest`, `@ExtendWith(SpringExtension.class)` o ApplicationContext.
2. **Prohibido mockear el Agregado o los Value Objects**: Se instancian directamente mediante sus constructores o fábricas estáticas (`Loan.agree(...)`, `LoanPact`, `TrustScore`).
3. **Mocks solo para Puertos de Salida en Domain Services**: Si se prueba un `DomainService` (ej. `AgreeLoanService`), los puertos (`LoanRepository`, `BookAvailabilityPort`) se simulan con mocks simples o fakes en memoria sin magia reflexiva compleja.
4. **Análisis de Valores Límite (Boundary Value Testing)**:
    - Toda condición (`<`, `<=`, `>`, `>=`, `==`) debe tener pruebas en el límite exacto.
    - *Ejemplo*: Si el umbral de observación es `50`, se deben probar explícitamente `49`, `50` y `51`. Esto garantiza que los mutadores de PITest mueran inmediatamente.
5. **Inspección de Domain Events**:
    - Todo comando de negocio exitoso debe comprobar los eventos emitidos mediante `aggregate.pullDomainEvents()`.
    - Verificar que el evento tenga los IDs correctos, timestamp razonable y que la lista interna del agregado quede limpia tras el pull.

### Plantilla de Agregado y Boundary Test (JUnit 5 + AssertJ):
```java
@DisplayName("Aggregate: Loan (Dominio Puro)")
class LoanDomainTest {

    @Test
    @DisplayName("Debe acordar préstamo exitosamente y emitir LoanAgreedEvent")
    void shouldAgreeLoanAndRecordEvent() {
        // Arrange
        LoanId loanId = LoanId.generate();
        BookId bookId = BookId.generate();
        BorrowerId borrowerId = BorrowerId.generate();
        LoanPact pact = new LoanPact(Instant.now().plus(7, ChronoUnit.DAYS), "Cuidar cubierta");

        // Act
        Loan loan = Loan.agree(loanId, bookId, borrowerId, pact);

        // Assert
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getPact()).isEqualTo(pact);

        // Event assertion
        List<DomainEvent> events = loan.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(LoanAgreedEvent.class);
        
        // El pull debe vaciar los eventos pendientes
        assertThat(loan.pullDomainEvents()).isEmpty();
    }

    @ParameterizedTest(name = "Score {0} debe clasificarse como {1}")
    @CsvSource({
        "0, RESTRICTED",
        "29, RESTRICTED",
        "30, UNDER_OBSERVATION",
        "49, UNDER_OBSERVATION",
        "50, TRUSTWORTHY",
        "100, TRUSTWORTHY"
    })
    @DisplayName("Valores frontera para cálculo de nivel de reputación (Mutation-Ready)")
    void shouldCalculateReputationLevelAtExactBoundaries(int scoreValue, ReputationLevel expectedLevel) {
        TrustScore score = new TrustScore(scoreValue);
        assertThat(score.calculateLevel()).isEqualTo(expectedLevel);
    }
}
```

---

## 🗄️ Nivel 2: Pruebas de Integración (Persistencia y Transactional Outbox)

### Qué se debe verificar:
1. **Atomicidad ACID Agregado + Outbox**:
    - Al ejecutar un caso de uso decorado con `@Transactional`, la entidad del agregado y el registro en la tabla `outbox_events` deben quedar confirmados en la BD en la misma transacción.
2. **Prueba de Rollback**:
    - Si ocurre una excepción no comprobada o falla una invariante de persistencia, tanto el agregado como el evento de outbox deben descartarse (rollback completo).
3. **Mapeo y Restricciones de Esquema**:
    - Respeto de claves primarias UUID, tipos enum, nulos y nombres de columnas.

### Plantilla de Integración con Transactional Outbox:
```java
@SpringBootTest
@Transactional
@DisplayName("Integración: Atomicidad de Agregado y Transactional Outbox")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private AgreeLoanUseCase agreeLoanUseCase; // UseCase con Decorador @Transactional

    @Autowired
    private LoanJpaRepository loanJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxRepository;

    @Test
    @DisplayName("Debe persistir el agregado Loan y el evento Outbox en la misma transacción")
    void shouldPersistLoanAndOutboxEventAtomically() {
        // Arrange
        var command = new AgreeLoanUseCase.AgreeLoanCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plus(10, ChronoUnit.DAYS),
                "Pacto de prueba"
        );

        // Act
        Loan loan = agreeLoanUseCase.agreeLoan(command);

        // Assert Persistencia Agregado
        assertThat(loanJpaRepository.findById(loan.getId().value())).isPresent();

        // Assert Persistencia Outbox
        var outboxEvents = outboxRepository.findByStatus(OutboxStatus.PENDING);
        assertThat(outboxEvents)
                .extracting("aggregateId")
                .contains(loan.getId().value().toString());
    }
}
```

---

## 🔗 Nivel 3: Pruebas de Bounded Context (Contratos e Idempotencia)

### 1. Prueba de Contrato y Serialización del Evento
- Valida que el `DomainEvent` serializado a JSON por `OutboxDomainEventPublisher` sea exactamente deserializable por el `OutboxEventProcessor` usando Jackson 3.
- Asegura que los campos de tipo `record`, `Instant`, `UUID` y Value Objects mantengan fidelidad tipográfica.

```java
@Test
@DisplayName("Contrato: El payload del evento Outbox debe deserializarse limpiamente en el evento de destino")
void shouldSerializeAndDeserializeOutboxPayload() throws Exception {
    LoanAgreedEvent originalEvent = new LoanAgreedEvent(
            LoanId.generate(), BookId.generate(), BorrowerId.generate(),
            Instant.now().plus(5, ChronoUnit.DAYS), "Notas"
    );

    String jsonPayload = objectMapper.writeValueAsString(originalEvent);
    Class<?> eventClass = Class.forName(originalEvent.getClass().getName());
    Object deserialized = objectMapper.readValue(jsonPayload, eventClass);

    assertThat(deserialized).isEqualTo(originalEvent);
}
```

### 2. Prueba de Consumidor Idempotente (At-Least-Once Resilience)
- Simula la entrega duplicada del mismo evento.
- Ejecuta el `@EventListener` del contexto consumidor dos veces consecutivas con el mismo `eventId`.
- **Verificación**:
    - En la primera invocación: el caso de uso consumidor se ejecuta y se inserta un registro en la tabla de deduplicación (`*ProcessedEventJpaEntity`).
    - En la segunda invocación: el listener detecta que ya fue procesado (`existsById`) y **no** vuelve a aplicar el efecto secundario.

```java
@Test
@DisplayName("Idempotencia: Evento duplicado no debe re-aplicar cambios en el consumidor")
void shouldIgnoreDuplicateEventDeliveries() {
    UUID eventId = UUID.randomUUID();
    BookId bookId = BookId.generate();
    // Preparar libro en estado AVAILABLE
    ...

    LoanAgreedEvent duplicateEvent = new LoanAgreedEvent(
            LoanId.generate(), bookId, BorrowerId.generate(), Instant.now().plus(5, ChronoUnit.DAYS), "Nota"
    );
    // Asignar el mismo eventId
    
    // 1er Despacho
    bookshelfEventListener.onLoanAgreed(duplicateEvent);
    assertThat(getBook(bookId).getStatus()).isEqualTo(BookStatus.BORROWED);

    // Modificar manualmente o verificar llamadas al caso de uso
    // 2do Despacho (Reintento de red / Outbox relay)
    bookshelfEventListener.onLoanAgreed(duplicateEvent);

    // Debe permanecer consistente sin duplicar registros de auditoría
    assertThat(processedEventRepository.existsById(duplicateEvent.eventId())).isTrue();
}
```

---

## 🎯 Nivel 5: Pruebas de Aceptación E2E (Black-Box & BDD Nativo)

Las pruebas de aceptación validan que el sistema resuelva los flujos de negocio requeridos desde la perspectiva del consumidor final (Caja Negra / Black-Box). Se enfocan en criterios de aceptación de punta a punta, automatizando los escenarios documentados en `requests.http`.

### 💡 ¿Por qué BDD Nativo con JUnit 5 (sin Cucumber ni frameworks externos)?
- **Cero intermediarios**: En Minimal DDD y Java 25, frameworks como Cucumber introducen parsing de archivos `.feature`, pegamento de expresiones regulares frágil y sobrecarga en CI/CD.
- **Legibilidad pura con `@Nested` y `@DisplayName`**: JUnit 5 permite estructurar las pruebas en lenguaje de negocio ubicuo con Given / When / Then nativo, tipado fuerte y autocompletado en el IDE.
- **Rendimiento superior**: Usando `@SpringBootTest` + `@AutoConfigureMockMvc`, los tests se ejecutan contra el `DispatcherServlet` real en milisegundos sin levantar puertos de red innecesarios.

### 🛡️ Reglas Inquebrantables para Tests de Aceptación:
1. **PROHIBIDO anotar la clase de test con `@Transactional`**:
    - En pruebas de aceptación contra la API Web, cada petición HTTP debe ejecutarse en su propia transacción aislada (provista por los *Transactional Decorators*).
    - Anotar `@Transactional` a nivel de test provoca bloqueos de conexión (deadlocks en H2 con `REQUIRES_NEW` en locks pesimistas) y previene la ejecución de listeners `@TransactionalEventListener(phase = AFTER_COMMIT)` del Transactional Outbox.
2. **Aislamiento de Datos (Evitar mutar datos semilla compartidos)**:
    - Para operaciones mutables (cancelar, reprogramar, completar), el test debe crear su propia entidad dentro del flujo antes de modificarla, evitando ensuciar registros globales de `data.sql` que otros tests de integración necesitan.
3. **Verificación Semántica de Códigos HTTP y RFC 9457**:
    - Happy path: `200 OK` o `201 Created` con headers de ubicación (`Location`).
    - Violación de invariantes: `422 Unprocessable Entity` con `ProblemDetail` estandarizado.
    - Fallos de autenticación o validación: `400 Bad Request` / `401 Unauthorized` / `409 Conflict`.
4. **Verificación de Efectos Colaterales Event-Driven**:
    - Tras confirmar una acción (ej. agendar turno), disparar el worker del Outbox (`outboxEventRelay.relayPendingEvents()`) y comprobar mediante la API HTTP del contexto consumidor que las proyecciones o notificaciones se hayan generado.

### Plantilla de Prueba de Aceptación E2E (Spring Boot + MockMvc):
```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Nivel 5: Pruebas de Aceptación E2E - Contexto de Préstamos")
class LoanAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Criterio de Aceptación: Ciclo de Préstamo y Devolución")
    class LoanLifecycle {

        @Test
        @DisplayName("Dado un lector habilitado, debe acordar préstamo y luego devolverlo en fecha")
        void shouldAgreeAndReturnLoanSuccessfully() throws Exception {
            // GIVEN: Solicitud de préstamo válida
            String requestPayload = """
                    {
                        "bookId": "book-123",
                        "borrowerId": "user-456",
                        "dueDate": "2026-11-20T10:00:00"
                    }
                    """;

            // WHEN: Se realiza la petición HTTP POST
            MvcResult result = mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andReturn();

            String loanId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asText();

            // THEN: Puede consultarse el préstamo y devolverse
            mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RETURNED"));
        }
    }
}
```

---

## 🛡️ Quality Gate & Cobertura con JaCoCo (Targeted Coverage)

En la industria (CI/CD, SonarQube, GitHub Actions), el **Quality Gate del 80%** es el estándar de aprobación para merge a ramas principales.

### ⚠️ El Dilema de la Cobertura Ciega vs DDD (Vanity Metrics):
- **Error habitual en la industria**: Exigir un 80% plano a todo el proyecto provoca que los equipos escriban tests triviales e inútiles para DTOs, entidades con getters/setters o clases `@Configuration`, creando una falsa sensación de seguridad.
- **Enfoque Estratégico en Minimal DDD**:
    1. **Dominio Puro (`*.domain.*`) >= 85% - 90%+**:
        - Al ser POJOs puros sin dependencias de frameworks que corren en milisegundos, la cobertura de Agregados, Value Objects y Domain Services debe ser rigurosa y exhaustiva.
        - Combinado con análisis de límites para resistir pruebas de mutación (PITest).
    2. **Exclusiones Explícitas de Boilerplate**:
        - En `build.gradle` (JaCoCo / SonarQube), se deben excluir del gate de cobertura el pegamento tecnológico:
            - `**/dto/**` (Data Transfer Objects planos).
            - `**/*Config.*` (Configuraciones de Spring).
            - `**/*Application.*` (Bootstrap de Spring Boot).
            - `**/exception/**` (Constructores de excepciones estándar).
            - `**/*Controller.*` (Se verifican funcionalmente vía `requests.http`).
    3. **Infraestructura Crítica**:
        - Centrar el esfuerzo en la **Atomicidad del Outbox** y el **Consumo Idempotente**.

### Comando de Verificación del Quality Gate:
```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```
Si la cobertura de líneas del código de negocio filtrado cae por debajo del 80% (o del umbral definido), Gradle abortará el build con:
```text
> Task :jacocoTestCoverageVerification FAILED
Rule violated for bundle ...: lines covered ratio is X.XX, but expected minimum is 0.80
```

---

## 🧬 Pruebas de Mutación con PITest (Domain Mutation Testing)

Las pruebas de mutación evalúan la **calidad real** de los tests inyectando fallos intencionales en el bytecode (mutantes: invertir condiciones `<` por `<=`, cambiar `+` por `-`, retornar `null`, omitir llamadas a métodos).

### ⚡ Estrategia Minimal DDD: Mutar Exclusivamente el Dominio
- **El problema en la industria**: Ejecutar PITest en todo un proyecto Spring Boot es inviable (puede tardar 20-30 minutos por reinicios de ApplicationContext y caídas de infraestructura).
- **La solución Minimal DDD**: Como la capa `*.domain.*` está compuesta por POJOs puros libres de Spring, PITest ejecuta en **menos de 5 segundos** sobre las invariantes del negocio.

### Configuración en `build.gradle` (Java 25):
```groovy
plugins {
    id 'info.solidsoft.pitest' version '1.19.0'
}

pitest {
    junit5PluginVersion = '1.2.1'
    pitestVersion = '1.30.0' // Soporte nativo para bytecode Java 25
    targetClasses = ['com.example.bibliotecav1personalscaffolding.*.domain.*'] // Solo Dominio
    targetTests = ['com.example.bibliotecav1personalscaffolding.*.domain.*']
    threads = 4
    outputFormats = ['XML', 'HTML']
    timestampedReports = false
}
```

### Ejecución y Reporte:
```bash
./gradlew pitest
```
- Reporte HTML interactivo generado en: `build/reports/pitest/index.html`.
- **Métricas Clave**:
    - `ConditionalsBoundaryMutator`: 100% de mutantes eliminados (gracias al Boundary Testing de VOs y Agregados).
    - `Test Strength`: Ratio de mutantes eliminados sobre mutantes alcanzados por tests (objetivo `>= 85%`).

---

## 📁 Estructura de Directorios y Convención de Nombres de Tests

```text
src/test/java/com/example/.../
├── acceptance/                                 # NIVEL 5: Aceptación E2E (Black-Box & BDD)
│   ├── *AcceptanceTest.java                    # Flujos de negocio de punta a punta (HTTP)
├── <context>/
│   ├── domain/                                 # NIVEL 1: Unitarias Puras (POJO)
│   │   ├── model/
│   │   │   ├── aggregate/*DomainTest.java      # Test de agregados e invariantes
│   │   │   └── valueobject/*DomainTest.java    # Boundary tests para mutación
│   │   └── service/*ServiceTest.java           # Domain Services con mocks de puertos
│   └── infrastructure/
│       ├── persistence/                        # NIVEL 2: Integración
│       │   └── *PersistenceIntegrationTest.java# Repositorios JPA y esquemas
│       ├── event/                              # NIVEL 3: Contratos e Idempotencia
│       │   └── *EventListenerIdempotencyTest.java
│       └── web/
│           └── *ControllerTest.java            # WebMvc slice testing
└── shared/infrastructure/outbox/
    └── OutboxAtomicityIntegrationTest.java     # Atomicidad Outbox en misma TX
```

---

## 📋 Checklist de Calidad para el Agente al Generar Tests

Antes de dar por completada la suite de pruebas de un nuevo módulo o feature:

- [ ] **Pureza POJO**: ¿Los tests de `domain/` corren sin anotar `@SpringBootTest` ni `@ExtendWith(SpringExtension.class)`?
- [ ] **Resistencia a Mutaciones (PITest)**: ¿Los Value Objects y Agregados tienen tests para valores límite exactos (`<` vs `<=`, `0`, valores máximos)?
- [ ] **Domain Events Vaciados**: ¿Se verificó que `aggregate.pullDomainEvents()` retorne los eventos esperados y luego quede vacía?
- [ ] **Atomicidad Outbox**: ¿Se verificó que `outbox_events` reciba el evento en la misma transacción que el aggregate?
- [ ] **Idempotencia Comprobada**: ¿Se simuló un evento duplicado verificando que la tabla de deduplicación evite re-ejecuciones?
- [ ] **Aceptación E2E Validada (Nivel 5)**: ¿Los casos de negocio principales (Happy Path, invariantes RFC 9457 y efectos Outbox) tienen tests de aceptación automatizados que cubren los flujos de `requests.http`?
- [ ] **Quality Gate JaCoCo Superado**: ¿Se ejecutó `./gradlew jacocoTestCoverageVerification` y la cobertura efectiva supera el 80% (con Dominio > 85%)?
- [ ] **Velocidad de Ejecución**: ¿El comando `./gradlew test` completa la suite en pocos segundos gracias al aislamiento de capas?
