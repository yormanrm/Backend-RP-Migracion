# Instrucciones de Arquitectura Frontend — Marketplace (Angular 21)

> Documento generado por auditoría exhaustiva del backend real (`graphify` + lectura directa de código fuente) en el commit `000beb27` y siguientes. Todo lo aquí descrito refleja el estado **actual** del backend Spring Boot, sin asunciones. Este archivo es el System Prompt / Guía de Ejecución para el agente LLM que construirá el frontend desde cero.

## 0. Contexto técnico del backend (para que el frontend apunte correctamente)

- Base URL: **sin prefijo** `/api/v1`. Los controllers usan rutas planas: `/auth`, `/profile`, `/items`, `/categories`, `/cart`, `/checkout`, `/orders`, `/associate/items`. Puerto por defecto de Spring Boot: `8080` (no hay `server.port` custom en `application.yaml`).
- Autenticación: JWT stateless (OAuth2 Resource Server, HS256). El login devuelve un `accessToken` que debe enviarse como `Authorization: Bearer <token>` en cada request protegido.
- El claim `role` del JWT determina la autoridad Spring Security como `ROLE_<role>` (ej. `ROLE_CUSTOMER`, `ROLE_ASSOCIATE`).
- Envoltorio de respuesta **universal**: toda respuesta exitosa viene envuelta en `ApiResponse<T>`. Todo el manejo de datos en el frontend debe desestructurar `response.data`, nunca asumir que el body es el objeto de negocio directamente.
- Paginación: usa `PageResponse<T>` (Spring `Page` serializado a forma simple, no HATEOAS).

---

## 1. Mapeo de Módulos y Endpoints (LA VERDAD ABSOLUTA)

### Módulo 1 — Storefront Público (sin autenticación)

| Método | Endpoint | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/items` | Público | Búsqueda/listado paginado de productos. Query params: `priceMin`, `priceMax`, `categorySlug`, `sortBy` (`recent`\|`bestsellers`\|`price_asc`\|`price_desc`, default `recent`), `page` (default `0`), `size` (default `20`) |
| GET | `/items/{slug}` | Público | Detalle de un producto por slug |
| GET | `/categories` | Público | Listado completo de categorías (sin paginar) |

Nota de seguridad: `SecurityConfig` permite explícitamente `GET /categories/**` y `GET /items/**` sin token. Cualquier otro verbo sobre esas rutas requeriría autenticación (no existe hoy, pero el frontend no debe asumir POST/PUT/DELETE públicos en catálogo).

### Módulo 2 — Autenticación y Perfiles

| Método | Endpoint | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/auth/register/customer` | Público | Registro de cliente. Devuelve `LoginResponse` (login automático post-registro), HTTP 201 |
| POST | `/auth/register/associate` | Público | Registro de asociado/vendedor. Devuelve `LoginResponse`, HTTP 201 |
| POST | `/auth/login` | Público | Login. Devuelve `LoginResponse`, HTTP 200 |
| GET | `/profile/me` | ROLE_CUSTOMER o ROLE_ASSOCIATE (cualquier autenticado) | Perfil propio. El tipo de respuesta varía según rol (`CustomerProfileResponse` o `AssociateProfileResponse`) — el backend no discrimina el tipo en el DTO (`ApiResponse<?>`), el frontend debe inspeccionar el shape o guardar el rol del JWT decodificado para saber qué interfaz aplicar |
| PUT | `/profile/me/customer` | ROLE_CUSTOMER (`@PreAuthorize("hasRole('CUSTOMER')")`) | Actualiza perfil de cliente |
| PUT | `/profile/me/associate` | ROLE_ASSOCIATE (`@PreAuthorize("hasRole('ASSOCIATE')")`) | Actualiza perfil de asociado |

Todos los endpoints de `/auth/**` son públicos (`SecurityConfig`: `.requestMatchers("/auth/**").permitAll()`). Todo lo demás no listado explícitamente como público requiere `.anyRequest().authenticated()`.

### Módulo 3 — Checkout (requiere autenticación, cualquier rol autenticado)

| Método | Endpoint | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/cart` | Autenticado | Obtener carrito propio |
| POST | `/cart` | Autenticado | Agregar ítem al carrito |
| PUT | `/cart/items/{cartItemId}` | Autenticado | Actualizar cantidad de una línea del carrito |
| DELETE | `/cart` | Autenticado | Vaciar carrito completo |
| POST | `/checkout` | Autenticado | Confirmar compra del carrito actual, genera `Order` |
| GET | `/orders` | Autenticado | Listar órdenes propias |
| GET | `/orders/{orderId}` | Autenticado | Detalle de una orden propia |

Ninguno de estos endpoints tiene `@PreAuthorize` de rol específico — cualquier usuario autenticado (CUSTOMER o ASSOCIATE) puede usarlos.

### Módulo 4 — Dashboard Privado de Asociados

| Método | Endpoint | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/associate/items` | ROLE_ASSOCIATE (`@PreAuthorize("hasRole('ASSOCIATE')")`) | Crear producto en el inventario propio |
| PUT | `/associate/items/{itemId}` | ROLE_ASSOCIATE | Actualizar producto propio |
| DELETE | `/associate/items/{itemId}` | ROLE_ASSOCIATE | Eliminar producto propio |

El `userId` en estos tres endpoints se extrae del JWT (`jwt.getSubject()`), no se envía en el body — el backend infiere el dueño del inventario por el token.

### Roles del sistema

Solo existen dos roles (`enum Role`): `CUSTOMER`, `ASSOCIATE`. **No existe `ROLE_ADMIN`** en el backend actual — no construir pantallas ni guards para un rol admin que no existe.

---

## 2. Política de Diseño UI/UX (REGLA CRÍTICA INQUEBRANTABLE)

> **ESTRICTAMENTE PROHIBIDO INVERTIR TIEMPO EN DISEÑO VISUAL. No se implementará nada UI, no se usarán utilidades complejas de Tailwind CSS, ni se intentará hacer la aplicación estéticamente agradable. El objetivo de esta fase es 100% FUNCIONAL. La UI será cruda y básica. Se permite el uso de PrimeNG únicamente para montar tablas de datos o formularios funcionales rápidos. Prioriza el Data-Binding, las llamadas HTTP y el enrutamiento.**

---

## 3. Estándares de Desarrollo en Angular 21

- **Standalone Components únicamente.** Cero `NgModule`. Bootstrap vía `bootstrapApplication`.
- **Nuevo Control Flow obligatorio**: `@if`, `@for` (con `track`), `@switch`. Prohibido `*ngIf`/`*ngFor`/`*ngSwitch`.
- **Signals para estado local**: `signal()`, `computed()`, `effect()`. Nada de `BehaviorSubject` para estado de componente simple (RxJS solo donde el HTTP/Router lo exija).
- **`inject()` en vez de constructor** para toda inyección de dependencias (servicios, `Router`, `ActivatedRoute`, etc.).

---

## 4. Tipado Fuerte (Contratos de Datos)

Convenciones de conversión aplicadas: `UUID` → `string`, `Long`/`int`/`BigDecimal` → `number`, `Instant`/`LocalDateTime` → `string` (ISO 8601, parsear a `Date` solo si se necesita manipular), `enum` Java → union type de strings literales en TS.

```typescript
// ── common ────────────────────────────────────────────────
export interface ApiResponse<T> {
  code: number;
  error: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ── auth ──────────────────────────────────────────────────
export type Role = 'CUSTOMER' | 'ASSOCIATE';

export interface RegisterCustomerRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface RegisterAssociateRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  storeName: string;
  storeSlug: string;
  taxId?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface CustomerProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  street: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
}

export interface CustomerProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface AssociateProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  storeName: string;
  storeSlug: string;
  taxId: string | null;
  publicBio: string | null;
  publicContactEmail: string | null;
  publicContactPhone: string | null;
}

export interface AssociateProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  storeName: string;
  taxId?: string;
  publicBio?: string;
  publicContactEmail?: string;
  publicContactPhone?: string;
}

// ── catalog ───────────────────────────────────────────────
export interface CategoryResponse {
  id: string;
  name: string;
  slug: string;
}

export interface AssociateInfoResponse {
  id: string;
  storeName: string;
}

export interface ItemResponse {
  id: string;
  title: string;
  slug: string;
  price: number;
  description: string | null;
  category: CategoryResponse;
  associateInfo: AssociateInfoResponse;
  images: string[];
}

export interface ItemSummaryResponse {
  id: string;
  title: string;
  slug: string;
  price: number;
  category: CategoryResponse;
  images: string[];
}

export type ItemSortBy = 'recent' | 'bestsellers' | 'price_asc' | 'price_desc';

export interface ItemSearchParams {
  priceMin?: number;
  priceMax?: number;
  categorySlug?: string;
  sortBy?: ItemSortBy;
  page?: number;
  size?: number;
}

// ── associate / inventory ────────────────────────────────
export interface ItemCreateRequest {
  title: string;
  slug: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: string;
  images?: string[];
}

export interface ItemUpdateRequest {
  title: string;
  slug: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: string;
  images?: string[];
}

// ── purchase: cart ────────────────────────────────────────
export interface CartLineResponse {
  cartItemId: string;
  itemId: string;
  title: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface CartResponse {
  id: string;
  items: CartLineResponse[];
  total: number;
}

export interface AddToCartRequest {
  itemId: string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

// ── purchase: orders ──────────────────────────────────────
export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED';

export interface OrderItemResponse {
  itemId: string;
  title: string;
  unitPriceAtPurchase: number;
  quantity: number;
  subtotal: number;
}

export interface OrderResponse {
  id: string;
  status: OrderStatus;
  items: OrderItemResponse[];
  total: number;
  createdAt: string; // Instant ISO-8601
}
```

Campos sin `@NotBlank`/`@NotNull` en el DTO Java del backend se marcan opcionales (`?`) o `| null` en TS según sea request u response respectivamente.

---

## 5. Arquitectura de Carpetas y Seguridad

### Árbol de carpetas (Feature-Based)

```
src/app/
├── core/
│   ├── interceptors/
│   │   └── auth.interceptor.ts       # adjunta Bearer token
│   ├── guards/
│   │   ├── auth.guard.ts             # canActivateFn: requiere sesión
│   │   ├── customer.guard.ts         # canActivateFn: requiere ROLE_CUSTOMER
│   │   └── associate.guard.ts        # canActivateFn: requiere ROLE_ASSOCIATE
│   ├── models/
│   │   ├── api-response.model.ts
│   │   └── page-response.model.ts
│   └── services/
│       └── auth.service.ts           # login, register, logout, signal de sesión actual
├── features/
│   ├── auth/
│   │   ├── login/
│   │   ├── register-customer/
│   │   ├── register-associate/
│   │   └── models/auth.models.ts
│   ├── profile/
│   │   ├── customer-profile/
│   │   ├── associate-profile/
│   │   └── models/profile.models.ts
│   ├── storefront/
│   │   ├── item-list/
│   │   ├── item-detail/
│   │   ├── services/item.service.ts
│   │   ├── services/category.service.ts
│   │   └── models/catalog.models.ts
│   ├── cart/
│   │   ├── cart-view/
│   │   ├── services/cart.service.ts
│   │   └── models/cart.models.ts
│   ├── checkout/
│   │   ├── checkout-confirm/
│   │   ├── order-list/
│   │   ├── order-detail/
│   │   ├── services/order.service.ts
│   │   └── models/order.models.ts
│   └── associate-dashboard/
│       ├── inventory-list/
│       ├── inventory-form/
│       ├── services/inventory.service.ts
│       └── models/inventory.models.ts
├── shared/
│   └── components/                   # widgets reusables mínimos (paginación, etc.)
├── app.routes.ts
└── app.config.ts
```

### Almacenamiento del JWT

`localStorage` bajo la clave `access_token`. Se lee en `AuthService` al bootstrap para restaurar sesión (decodificar el JWT — sin librería externa, `atob` sobre el payload base64 basta para leer `role` y `sub`).

### HttpInterceptor

`auth.interceptor.ts` (función `HttpInterceptorFn`, registrado en `app.config.ts` vía `provideHttpClient(withInterceptors([authInterceptor]))`): si existe token en `localStorage`, clona el request y agrega `Authorization: Bearer <token>`. No adjuntar en `/auth/register/**` ni `/auth/login` (no lo necesitan, es inofensivo igual pero evitarlo es más limpio).

### Guards funcionales (`canActivateFn`)

- `authGuard`: bloquea rutas si no hay token válido → redirige a `/login`. Protege `cart`, `checkout`, `orders`, `profile`.
- `associateGuard`: además de `authGuard`, verifica `role === 'ASSOCIATE'` del JWT decodificado → protege todo `associate-dashboard/**`. Sin este rol, redirige (ej. a `/`).
- No se requiere `customerGuard` para checkout: cualquier autenticado puede comprar. Sí aplica a `PUT /profile/me/customer` en el formulario de perfil de cliente.

---

## 6. Roadmap de Ejecución (Plan Atómico para el LLM)

### Fase 1 — Setup y Core
1. Generar proyecto Angular 21 standalone (`ng new --standalone --routing`).
2. Configurar `app.config.ts` con `provideHttpClient(withInterceptors([...]))` y `provideRouter(routes)`.
3. Crear `core/models/api-response.model.ts` y `core/models/page-response.model.ts` (interfaces de la sección 4).
4. Crear `core/services/auth.service.ts` con signal `currentUser` (null | `{ id, role }` decodificado del JWT) y métodos `login()`, `registerCustomer()`, `registerAssociate()`, `logout()`.
5. Crear `core/interceptors/auth.interceptor.ts`.
6. Crear `core/guards/auth.guard.ts` y `core/guards/associate.guard.ts`.
7. Definir `app.routes.ts` vacío con estructura de features (rutas lazy por `loadComponent`).

### Fase 2 — Auth Feature
1. Crear `features/auth/models/auth.models.ts` (interfaces de auth de la sección 4).
2. Componente `login/login.component.ts`: formulario reactivo (email, password) → `authService.login()` → redirige a `/`.
3. Componente `register-customer/register-customer.component.ts`: formulario reactivo con todos los campos de `RegisterCustomerRequest`.
4. Componente `register-associate/register-associate.component.ts`: formulario reactivo con todos los campos de `RegisterAssociateRequest`.
5. Rutas `/login`, `/register/customer`, `/register/associate` en `app.routes.ts`.

### Fase 3 — Storefront (Público)
1. Crear `features/storefront/models/catalog.models.ts`.
2. Crear `features/storefront/services/category.service.ts` (`GET /categories`).
3. Crear `features/storefront/services/item.service.ts` (`GET /items` con query params, `GET /items/{slug}`).
4. Componente `item-list/item-list.component.ts`: tabla PrimeNG con filtros (precio min/max, categoría, sort) y paginación server-side usando `PageResponse`.
5. Componente `item-detail/item-detail.component.ts`: detalle por slug, botón "agregar al carrito" (deshabilitado si no autenticado, redirige a login).
6. Ruta raíz `/` → `item-list`, ruta `/items/:slug` → `item-detail`.

### Fase 4 — Cart Feature (requiere auth)
1. Crear `features/cart/models/cart.models.ts`.
2. Crear `features/cart/services/cart.service.ts` (`GET/POST/DELETE /cart`, `PUT /cart/items/{cartItemId}`), estado en `signal<CartResponse | null>`.
3. Componente `cart-view/cart-view.component.ts`: tabla de líneas, control de cantidad por línea, botón vaciar carrito, botón "ir a checkout".
4. Ruta `/cart` protegida con `authGuard`.

### Fase 5 — Checkout Feature (requiere auth)
1. Crear `features/checkout/models/order.models.ts`.
2. Crear `features/checkout/services/order.service.ts` (`POST /checkout`, `GET /orders`, `GET /orders/{orderId}`).
3. Componente `checkout-confirm/checkout-confirm.component.ts`: botón confirmar compra → `POST /checkout` → redirige a detalle de la orden creada.
4. Componente `order-list/order-list.component.ts`: lista de órdenes propias.
5. Componente `order-detail/order-detail.component.ts`: detalle de una orden (status, items, total).
6. Rutas `/checkout`, `/orders`, `/orders/:orderId`, todas con `authGuard`.

### Fase 6 — Profile Feature (requiere auth)
1. Crear `features/profile/models/profile.models.ts`.
2. Crear servicio `profile.service.ts` (`GET /profile/me`, `PUT /profile/me/customer`, `PUT /profile/me/associate`).
3. Componente `customer-profile/customer-profile.component.ts`: formulario reactivo, visible solo si `role === 'CUSTOMER'`.
4. Componente `associate-profile/associate-profile.component.ts`: formulario reactivo, visible solo si `role === 'ASSOCIATE'`.
5. Ruta `/profile` con `authGuard`, renderiza uno u otro componente según rol del signal `currentUser`.

### Fase 7 — Associate Dashboard (requiere ROLE_ASSOCIATE)
1. Crear `features/associate-dashboard/models/inventory.models.ts`.
2. Crear `features/associate-dashboard/services/inventory.service.ts` (`POST/PUT/DELETE /associate/items/{itemId}`).
3. Componente `inventory-list/inventory-list.component.ts`: tabla PrimeNG de productos propios con acciones editar/eliminar.
4. Componente `inventory-form/inventory-form.component.ts`: formulario reactivo compartido para crear/editar (`ItemCreateRequest`/`ItemUpdateRequest`).
5. Rutas `/associate/items`, `/associate/items/new`, `/associate/items/:id/edit`, todas con `associateGuard`.

### Fase 8 — Integración final
1. Navbar mínima con enlaces condicionales por sesión/rol (usar `@if` sobre `authService.currentUser()`).
2. Verificar manualmente cada flujo: registro → login → navegación catálogo → agregar a carrito → checkout → ver orden → (si asociado) crear/editar/eliminar producto.
3. Manejo de errores HTTP genérico: interceptor o `catchError` por servicio que lea `ApiResponse.message` para mostrar el error (sin diseño, un `alert()` o texto plano basta).
