# 🛒 Arka Frontend - Portal B2B de Distribución Mayorista (Angular)

Aplicación cliente de alta fidelidad para la plataforma **Arka**, diseñada bajo los principios del **Monolito Modular** y **Minimal DDD**, conectada a la API de backend desarrollada en Spring Boot 3.4 / Java 25.

---

## 🏛️ 1. Mapeo de Carpetas con Bounded Contexts

El código fuente del frontend refleja directamente los límites transaccionales (*Bounded Contexts*) del dominio:

```
frontend/src/app/
├── core/                                   # Kernel compartido y fontanería técnica
│   ├── interceptors/
│   │   └── error.interceptor.ts            # Tratamiento RFC 9457 (ProblemDetail) & alertas
│   ├── models/
│   │   ├── product.model.ts                # Modelos de Catálogo e Inventario
│   │   ├── order.model.ts                  # Modelos de Órdenes de Compra y Ciclo de Vida
│   │   ├── cart.model.ts                   # Modelos de Carrito y Detección de Abandono
│   │   ├── notification.model.ts           # Modelos de Notificaciones Asíncronas
│   │   ├── analytics.model.ts              # Modelos de Proyecciones CQRS y Reportes
│   │   ├── customer.model.ts               # Semillas y sesión de Clientes Mayoristas
│   │   └── problem-detail.model.ts         # Contrato RFC 7807/9457 para excepciones de dominio
│   └── services/
│       ├── theme.service.ts                # Sistema de Temas (Dark/Light) con Signals
│       ├── customer-session.service.ts     # Control reactivo del cliente B2B activo
│       └── toast.service.ts                # Notificaciones flotantes y feedback visual
│
├── features/                               # Bounded Contexts de Negocio (Minimal DDD)
│   ├── inventory/                          # 🏭 Bounded Context: Inventory & Catalog
│   │   ├── inventory.service.ts            # Servicio de catálogo, stock físico y reservas
│   │   └── product-catalog.component.ts    # Vista de catálogo, filtrado, alta HU1 y ajuste HU2
│   │
│   ├── ordering/                           # 📦 Bounded Context: Purchase Orders
│   │   ├── order.service.ts                # Gestión de estados (PENDING ➡️ DELIVERED)
│   │   └── order-dashboard.component.ts    # Tablero de pedidos, radicación HU4, modificación HU5 y confirmación HU6
│   │
│   ├── cart/                               # 🛒 Bounded Context: Cart & Abandonment
│   │   ├── cart.service.ts                 # Operaciones de carrito y abandono
│   │   └── cart-view.component.ts          # Carrito activo, conversión a pedido y recuperación HU8
│   │
│   ├── notification/                       # 🔔 Bounded Context: Notifications
│   │   ├── notification.service.ts         # Consulta de mensajes generados por eventos
│   │   └── notification-center.component.ts# Feed de correos y alertas multicanal despachadas
│   │
│   └── analytics/                          # 📊 Bounded Context: Analytics CQRS
│       ├── analytics.service.ts            # Métricas CQRS y descarga de reportes CSV
│       └── analytics-dashboard.component.ts# Dashboard de ventas, reposición crítica HU3 y HU7
│
├── app.ts / app.html / app.css             # Shell B2B, barra de navegación, switcher de cliente y toasts
├── app.routes.ts                           # Enrutamiento modular con Standalone Components (Lazy Loading)
└── app.config.ts                           # Configuración con provideHttpClient + Fetch + Interceptor
```

---

## ⚡ 2. Reactividad con Signals de Angular

El proyecto utiliza la reactividad de **Angular Signals** en lugar de suscripciones imperativas complejas:

* **Estado Puro con `signal()`**:
  * `theme = signal<'light' | 'dark'>('dark')` en `ThemeService`.
  * `activeCustomer = signal<CustomerSeed>(...)` en `CustomerSessionService`.
  * `products = signal<Product[]>([])`, `inventoryMap = signal<Record<string, InventoryItem>>({})` en `InventoryService`.
  * `cart = signal<Cart | null>(null)` en `CartService`.
  * `orders = signal<PurchaseOrder[]>([])` en `OrderService`.
* **Cálculos Derivados con `computed()`**:
  * `cartItemCount = computed(() => ...)` calcula automáticamente la cantidad total de unidades en el carrito para el badge superior.
  * `cartTotal = computed(() => ...)` sincroniza el importe a facturar en tiempo real.
  * `productsWithInventory = computed(() => ...)` cruza en memoria la metadata del catálogo con el inventario físico/reservado y los filtros de búsqueda sin refetching innecesario.
* **Efectos Secundarios con `effect()`**:
  * Sincronización automática de `localStorage` y atributos DOM (`data-theme`) ante cambios de tema.
  * Recarga reactiva del carrito del cliente cuando el usuario conmuta de empresa mayorista en la barra superior.

---

## 🎨 3. Sistema de Temas (Dark / Light)

* **Arquitectura de Variables CSS**:
  * Ubicadas en `frontend/src/styles.css`.
  * Tema claro por defecto (`:root`) y tema oscuro de alto contraste empresarial (`[data-theme="dark"]` / `.dark`).
  * Paleta corporativa B2B de alta legibilidad:
    * `PENDING`: Ámbar/Amarillo (`--warning`)
    * `CONFIRMED`: Azul zafiro (`--primary`)
    * `DISPATCHED`: Púrpura/Índigo (`--accent-purple`)
    * `DELIVERED`: Verde esmeralda (`--success`)
    * `CANCELLED` / `LOW_STOCK`: Carmesí (`--danger`)
* **Conmutación en Caliente**:
  * Botón selector en la esquina superior derecha (`☀️ Claro` / `🌙 Oscuro`).
  * Persistencia automática en el navegador (`localStorage`).

---

## 🔌 4. Proxy de Desarrollo (`proxy.conf.json`)

Para evitar problemas de CORS (*Cross-Origin Resource Sharing*) durante el desarrollo local, el frontend redirige todas las llamadas `/api/*` al backend de Spring Boot:

**Archivo `frontend/proxy.conf.json`**:
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

Configurado en `angular.json` y en el script de arranque:
```bash
npm start
# Ejecuta: ng serve --proxy-config proxy.conf.json
```

---

## 🛡️ 5. Manejo de Invariantes RFC 9457 `ProblemDetail`

El interceptor HTTP `error.interceptor.ts` captura de forma transparente cualquier rechazo de invariante de dominio emitido por el backend (HTTP 400, 404, 422):
* Extrae el `title`, `detail` y mapa de campos `errors`.
* Despliega una alerta flotante (*Toast*) con el mensaje de negocio exacto (ej. *"Stock insuficiente para reservar 999 unidades"*, *"El pedido ya no puede modificarse porque no está en estado PENDIENTE"*).

---

## 🚀 6. Guía de Ejecución Rápida

### Prerrequisitos
* Backend de Arka levantado en el puerto `8080` (ver instrucciones en raíz del repositorio):
  ```bash
  ./gradlew bootRun
  ```

### Arrancar el Servidor Frontend de Desarrollo
```bash
cd frontend
npm install
npm start
```
Abrir el navegador en: **[http://localhost:4200](http://localhost:4200)**

### Ejecutar Pruebas Unitarias
```bash
cd frontend
npm test -- --watch=false
```

### Compilar para Producción
```bash
cd frontend
npm run build
```
Los artefactos compilados y optimizados se generarán en `frontend/dist/frontend`.
