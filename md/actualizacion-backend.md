# Actualización del Backend — Guía para el equipo Frontend (Angular)

> Documento autosuficiente con **todos** los cambios de la reestructuración del backend. Cubre breaking changes, catálogo completo de endpoints, interfaces TypeScript y ejemplos de payloads. Base URL: `http://localhost:8080` (sin prefijo). Autenticación: `Authorization: Bearer <token>`.

---

## 1. Resumen de cambios (breaking changes destacados)

| Módulo | Cambio | Impacto |
|--------|--------|---------|
| Registro de asociado | `taxId` → **`rfc`** (obligatorio, formato RFC mexicano, único). **`storeSlug` ya no se envía** — lo genera el backend desde `storeName`. | 💥 Breaking: cambiar el formulario de registro. |
| Perfil de asociado | El RFC **no es editable** tras el registro (se quitó del update). Se agregó `storeAddress` (dirección física de la tienda). | 💥 Breaking: quitar campo RFC del formulario de edición. |
| Perfil de cliente | Los campos de dirección (`street`, `city`, ...) **salieron** del perfil. Las direcciones ahora son una **lista** con endpoints propios (`/profile/me/addresses`). | 💥 Breaking: nueva pantalla de gestión de direcciones. |
| Items | Entidad unificada producto/servicio con **`type`** (`PRODUCT` / `SERVICE`). Nuevos campos: `sku`, `model`, `brandName` (request) / `brand` (response), campos de servicio (`durationValue`, `durationUnit`, `serviceMode`, `coverageZone`). `slug` autogenerado (ya no se envía). `stock` solo aplica a productos. | 💥 Breaking: formularios de inventario y tarjetas de catálogo. |
| Clasificación | El item se clasifica por **`subcategoryId`** (antes `categoryId`). `Category` ya no tiene `slug`. Nueva jerarquía: Categoría → Subcategoría. | 💥 Breaking: selects anidados. |
| Marcas | Nueva entidad global. El asociado escribe `brandName` libre; el backend reutiliza o crea la marca ("buscar o crear", case-insensitive). `GET /brands` para selects/autocompletado. | Nuevo. |
| Búsqueda | **`POST /items/search`** con body JSON (buscador universal `q` estilo Amazon/ML). `GET /items` queda solo para "traer todo" paginado. Los query params viejos de `GET /items` desaparecieron. | 💥 Breaking: el buscador cambia de GET a POST. |
| Borrado lógico | Ningún DELETE borra físicamente (excepto direcciones). Los items/categorías/etc. inactivos desaparecen del catálogo público. | Transparente para el público; el asociado ve `active` en su inventario. |
| Órdenes | Jerarquía **padre/hijas**: una compra = 1 orden padre + 1 sub-orden por asociado. El padre trae `aggregateStatus` **calculado por el backend**. Estados ampliados: `PAID → PROCESSING → COMPLETED` (+`CANCELLED`). | 💥 Breaking: vista de órdenes del comprador. |
| Carrito | No se pueden mezclar productos y servicios (el backend responde **409**). | Manejar el error en UI. |
| Rol ADMIN | Nuevo rol con endpoints `/admin/**` y CRUD de catálogo. | Nuevas pantallas (opcional). |
| Panel asociado | Nuevos: `GET /associate/items` (inventario propio), `GET /associate/orders` + cambio de estado, `GET /associate/sales-report`. | Nuevas pantallas. |

---

## 2. Envolturas comunes

Toda respuesta viene envuelta en `ApiResponse<T>`; los listados paginados en `PageResponse<T>`. Los errores usan **Problem Details** (RFC 7807).

```ts
export interface ApiResponse<T> {
  code: number;      // p.ej. 200, 201
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

// Error (Problem Details, status HTTP correspondiente)
export interface ProblemDetail {
  type: string;      // "about:blank"
  title: string;
  status: number;
  detail: string;    // mensaje legible en español
  instance?: string;
  errors?: Record<string, string>; // solo en 400 de validación: campo -> mensaje
}
```

## 3. Enums

```ts
export type Role = 'CUSTOMER' | 'ASSOCIATE' | 'ADMIN';
export type ItemType = 'PRODUCT' | 'SERVICE';
export type DurationUnit = 'HORAS' | 'DIAS' | 'SEMANAS';
export type ServiceMode = 'PRESENCIAL' | 'REMOTO' | 'AMBOS';
export type OrderStatus = 'CREATED' | 'PAID' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';
export type AggregateOrderStatus = 'EN_PROCESO' | 'PARCIALMENTE_COMPLETADA' | 'COMPLETADA' | 'CANCELADA';
export type SortBy = 'recent' | 'bestsellers' | 'price_asc' | 'price_desc';
```

## 4. Interfaces de dominio

```ts
// ---------- Auth / Perfil ----------
export interface RegisterCustomerRequest {
  email: string;
  password: string;      // mínimo 8 caracteres
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
  rfc: string;           // NUEVO nombre (antes taxId). Formato RFC mexicano (12 o 13 caracteres). Ya NO se envía storeSlug.
}

export interface LoginRequest { email: string; password: string; }

export interface LoginResponse {
  accessToken: string;   // JWT: sub = userId, claims email y role
  tokenType: 'Bearer';
  expiresInSeconds: number;
}

export interface CustomerProfileResponse {
  id: string;            // UUID
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  // Ya NO trae dirección: usar /profile/me/addresses
}

export interface CustomerProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface Address {
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface AssociateProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  storeName: string;
  storeSlug: string;     // autogenerado por el backend, estable
  rfc: string;           // solo lectura
  storeAddress: Address | null;
  publicBio: string | null;
  publicContactEmail: string | null;
  publicContactPhone: string | null;
}

export interface AssociateProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  storeName: string;
  storeAddress?: Address;   // NUEVO. El RFC ya NO se envía (inmutable).
  publicBio?: string;
  publicContactEmail?: string;
  publicContactPhone?: string;
}

export interface UserAddressResponse extends Address {
  id: string;
  isDefault: boolean;
}

// ---------- Catálogo ----------
export interface SubcategoryResponse {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
}

export interface CategoryResponse {
  id: string;
  name: string;            // ya NO tiene slug
  subcategories: SubcategoryResponse[];
}

export interface BrandResponse { id: string; name: string; }

export interface AssociateInfoResponse { id: string; storeName: string; }

export interface ItemResponse {
  id: string;
  title: string;
  slug: string;            // autogenerado
  type: ItemType;
  price: number;           // para SERVICE interpretarlo como "Desde $"
  stock: number | null;    // null para servicios
  sku: string | null;
  model: string | null;
  description: string | null;
  active: boolean;
  subcategory: SubcategoryResponse;
  brand: BrandResponse | null;
  durationValue: number | null;   // solo servicios
  durationUnit: DurationUnit | null;
  serviceMode: ServiceMode | null;
  coverageZone: string | null;
  associateInfo: AssociateInfoResponse;
  images: string[];
}

export interface ItemSummaryResponse {
  id: string;
  title: string;
  slug: string;
  type: ItemType;
  price: number;
  subcategory: SubcategoryResponse;
  brand: BrandResponse | null;
  images: string[];
}

export interface ItemSearchRequest {
  q?: string;              // buscador universal: título, slug, modelo, SKU, tienda y marca (parcial, sin mayúsculas)
  subcategoryId?: string;
  brandId?: string;
  type?: ItemType;
  priceMin?: number;
  priceMax?: number;
  sortBy?: SortBy;         // default 'recent'
  page?: number;           // default 0
  size?: number;           // default 20
}

// Inventario del asociado (create y update usan el mismo shape)
export interface ItemCreateRequest {
  title: string;
  description?: string;
  type: ItemType;
  price: number;
  stock?: number;          // OBLIGATORIO si type=PRODUCT; NO enviar si SERVICE
  sku?: string;            // OBLIGATORIO si PRODUCT (único dentro del asociado)
  model?: string;
  brandName?: string;      // OBLIGATORIO si PRODUCT; opcional si SERVICE. El backend crea/reutiliza la marca.
  subcategoryId: string;
  durationValue?: number;  // solo SERVICE
  durationUnit?: DurationUnit;
  serviceMode?: ServiceMode;
  coverageZone?: string;
  images?: string[];
}

// ---------- Carrito / Órdenes ----------
export interface AddToCartRequest { itemId: string; quantity: number; }
export interface UpdateCartItemRequest { quantity: number; }

export interface CartLineResponse {
  cartItemId: string;
  itemId: string;
  title: string;
  price: number;
  quantity: number;
  subtotal: number;
}
export interface CartResponse { id: string | null; items: CartLineResponse[]; total: number; }

export interface CheckoutRequest { addressId?: string; } // sin addressId usa la dirección predeterminada

export interface OrderItemResponse {
  itemId: string;
  title: string;
  unitPriceAtPurchase: number;
  quantity: number;
  subtotal: number;
}

export interface SubOrderResponse {
  id: string;
  associateId: string;
  storeName: string;
  status: OrderStatus;
  total: number;
  items: OrderItemResponse[];
}

// Vista del COMPRADOR: orden padre
export interface OrderResponse {
  id: string;
  aggregateStatus: AggregateOrderStatus; // calculado por el backend desde las sub-órdenes
  total: number;
  shippingAddress: Address | null;
  createdAt: string;                     // ISO-8601
  subOrders: SubOrderResponse[];
}

// Vista del ASOCIADO: su sub-orden
export interface AssociateOrderResponse {
  id: string;
  parentOrderId: string;
  status: OrderStatus;
  createdAt: string;
  total: number;
  customer: { firstName: string; lastName: string; email: string; phone: string | null };
  shippingAddress: Address | null;
  items: OrderItemResponse[];
}

export interface OrderStatusUpdateRequest { status: OrderStatus; }
export interface StatusUpdateRequest { active: boolean; }

// ---------- Reporte de ventas ----------
export interface ItemSales { itemId: string; title: string; unitsSold: number; }
export interface ItemRef { itemId: string; title: string; }

export interface AssociateSalesReportResponse {
  totalRevenue: number;           // órdenes COMPLETED (en el rango si se envió)
  completedOrdersCount: number;
  bestSeller: ItemSales | null;
  worstSeller: ItemSales | null;  // el menos vendido ENTRE los que sí vendieron; null si nadie vendió
  totalStock: number;             // tiempo real, items activos
  zeroSalesItems: ItemRef[];      // productos sin una sola venta (para evaluar retiro)
}

// ---------- Admin ----------
export interface AdminUserSummaryResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  active: boolean;
  createdAt: string;
}
```

---

## 5. Catálogo completo de endpoints

**Roles**: 🌐 público · 🔑 autenticado (cualquier rol) · 🛍 CUSTOMER · 🏪 ASSOCIATE · 👑 ADMIN

### Auth
| Método | Ruta | Rol | Request | Response (`data`) |
|--------|------|-----|---------|-------------------|
| POST | `/auth/register/customer` | 🌐 | `RegisterCustomerRequest` | `LoginResponse` |
| POST | `/auth/register/associate` | 🌐 | `RegisterAssociateRequest` | `LoginResponse` |
| POST | `/auth/register/admin` | 👑 | `RegisterCustomerRequest` | `LoginResponse` |
| POST | `/auth/login` | 🌐 | `LoginRequest` | `LoginResponse` |

### Perfil y direcciones
| Método | Ruta | Rol | Request | Response |
|--------|------|-----|---------|----------|
| GET | `/profile/me` | 🔑 | — | `CustomerProfileResponse` o `AssociateProfileResponse` según rol |
| PUT | `/profile/me/customer` | 🛍 | `CustomerProfileUpdateRequest` | `CustomerProfileResponse` |
| PUT | `/profile/me/associate` | 🏪 | `AssociateProfileUpdateRequest` | `AssociateProfileResponse` |
| GET | `/profile/me/addresses` | 🔑 | — | `UserAddressResponse[]` |
| POST | `/profile/me/addresses` | 🔑 | `Address` | `UserAddressResponse` (la primera queda predeterminada sola) |
| PUT | `/profile/me/addresses/{addressId}` | 🔑 | `Address` | `UserAddressResponse` |
| DELETE | `/profile/me/addresses/{addressId}` | 🔑 | — | `null` (si era la predeterminada, la más antigua restante hereda el default) |
| PUT | `/profile/me/addresses/{addressId}/default` | 🔑 | — | `UserAddressResponse` (desmarca las demás) |

### Catálogo público
| Método | Ruta | Rol | Request | Response |
|--------|------|-----|---------|----------|
| GET | `/categories` | 🌐 | — | `CategoryResponse[]` (con subcategorías anidadas, solo activas) |
| GET | `/subcategories/{id}` | 🌐 | — | `SubcategoryResponse` (incluye categoría padre) |
| GET | `/brands?q=` | 🌐 | `q` opcional (búsqueda parcial) | `BrandResponse[]` |
| GET | `/brands/{id}` | 🌐 | — | `BrandResponse` |
| GET | `/items?page=&size=` | 🌐 | paginación | `PageResponse<ItemSummaryResponse>` (todo lo activo, más recientes primero) |
| POST | `/items/search` | 🌐 | `ItemSearchRequest` | `PageResponse<ItemSummaryResponse>` (solo activos) |
| GET | `/items/{slug}` | 🌐 | — | `ItemResponse` (404 si inactivo) |

### Administración de catálogo (👑)
| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| POST | `/categories` | `{ name }` | `CategoryResponse` |
| PUT | `/categories/{id}` | `{ name }` | `CategoryResponse` |
| DELETE | `/categories/{id}` | — | `null` (borrado lógico) |
| POST | `/subcategories` | `{ name, categoryId }` | `SubcategoryResponse` |
| PUT | `/subcategories/{id}` | `{ name, categoryId }` | `SubcategoryResponse` |
| DELETE | `/subcategories/{id}` | — | `null` (borrado lógico) |
| PUT | `/brands/{id}` | `{ name }` | `BrandResponse` |
| DELETE | `/brands/{id}` | — | `null` (borrado lógico). No hay POST de marca: nacen por autoservicio. |

### Panel admin (👑)
| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| GET | `/admin/users?page=&size=` | — | `PageResponse<AdminUserSummaryResponse>` |
| PUT | `/admin/users/{userId}/status` | `StatusUpdateRequest` | `null`. Desactivar un ASSOCIATE desactiva sus items en cascada; reactivarlo NO los reactiva. |
| GET | `/admin/items?type=&active=&page=&size=` | — | `PageResponse<ItemResponse>` (incluye inactivos) |
| PUT | `/admin/items/{itemId}/status` | `StatusUpdateRequest` | `null` |

### Inventario del asociado (🏪)
| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| GET | `/associate/items?type=&page=&size=` | — | `PageResponse<ItemResponse>` (inventario propio, **incluye inactivos**) |
| POST | `/associate/items` | `ItemCreateRequest` | `ItemResponse` |
| PUT | `/associate/items/{itemId}` | `ItemCreateRequest` | `ItemResponse` (el slug NO cambia al editar) |
| PUT | `/associate/items/{itemId}/status` | `StatusUpdateRequest` | `ItemResponse` (reactivación manual) |
| DELETE | `/associate/items/{itemId}` | — | `null` (borrado lógico) |

Validaciones por tipo (el backend responde 400 si no se cumplen): `PRODUCT` exige `stock`, `sku` y `brandName`; `SERVICE` no debe llevar `stock` (los campos de servicio son opcionales y se ignoran para productos). SKU duplicado dentro del mismo asociado → 409.

### Órdenes del asociado (🏪)
| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| GET | `/associate/orders?status=&from=&to=&page=&size=` | fechas `yyyy-MM-dd` | `PageResponse<AssociateOrderResponse>` |
| PUT | `/associate/orders/{orderId}/status` | `OrderStatusUpdateRequest` | `AssociateOrderResponse`. Transiciones válidas: `PAID→PROCESSING`, `PROCESSING→COMPLETED`, y `CANCELLED` desde `PAID`/`PROCESSING`. Otra transición → 400. |
| GET | `/associate/sales-report?from=&to=` | fechas opcionales (sin rango = histórico) | `AssociateSalesReportResponse` |

### Carrito y checkout (🔑)
| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| GET | `/cart` | — | `CartResponse` |
| POST | `/cart` | `AddToCartRequest` | `CartResponse`. Mezclar productos y servicios → **409** "No se pueden combinar productos y servicios en el mismo carrito." |
| PUT | `/cart/items/{cartItemId}` | `UpdateCartItemRequest` | `CartResponse` |
| DELETE | `/cart` | — | `CartResponse` (vacío) |
| POST | `/checkout` | `CheckoutRequest` (body opcional) | `OrderResponse` (padre + sub-órdenes). Stock insuficiente → 422. |
| GET | `/orders?sort=recent\|oldest` | — | `OrderResponse[]` (solo órdenes padre) |
| GET | `/orders/{orderId}` | — | `OrderResponse` |

---

## 6. Ejemplos de payloads

### Registro de asociado (nuevo contrato)
```json
POST /auth/register/associate
{
  "email": "asociado1@test.com",
  "password": "password123",
  "firstName": "Luis",
  "lastName": "Gómez",
  "phone": "5559876543",
  "storeName": "Tienda de Luis",
  "rfc": "GOML850101AB1"
}
```

### Crear producto vs servicio
```json
POST /associate/items   (producto)
{
  "title": "Taladro Inalámbrico 20V",
  "type": "PRODUCT",
  "price": 1499.00,
  "stock": 25,
  "sku": "TAL-20V-001",
  "model": "DCD777C2",
  "brandName": "DeWalt",
  "subcategoryId": "<uuid subcategoría>",
  "images": ["https://example.com/taladro.jpg"]
}
```
```json
POST /associate/items   (servicio — sin stock; price = "Desde $")
{
  "title": "Instalación Eléctrica Residencial",
  "type": "SERVICE",
  "price": 999.00,
  "subcategoryId": "<uuid subcategoría>",
  "durationValue": 3,
  "durationUnit": "HORAS",
  "serviceMode": "PRESENCIAL",
  "coverageZone": "Ciudad de México y área metropolitana"
}
```

### Búsqueda universal
```json
POST /items/search
{ "q": "dewalt", "type": "PRODUCT", "sortBy": "bestsellers", "page": 0, "size": 20 }
```

### Respuesta de checkout (orden padre + sub-órdenes)
```json
{
  "code": 200, "error": false, "message": "OK",
  "data": {
    "id": "…",
    "aggregateStatus": "EN_PROCESO",
    "total": 4497.00,
    "shippingAddress": { "street": "Av. Siempre Viva 123", "city": "CDMX", "state": "CDMX", "postalCode": "01000", "country": "México" },
    "createdAt": "2026-07-20T18:30:00Z",
    "subOrders": [
      { "id": "…", "associateId": "…", "storeName": "Tienda de Luis", "status": "PAID", "total": 2998.00, "items": [ { "itemId": "…", "title": "Taladro Inalámbrico 20V", "unitPriceAtPurchase": 1499.00, "quantity": 2, "subtotal": 2998.00 } ] },
      { "id": "…", "associateId": "…", "storeName": "Ferretería El Tornillo", "status": "PAID", "total": 1499.00, "items": [ "…" ] }
    ]
  }
}
```

### Reporte de ventas
```json
GET /associate/sales-report?from=2026-01-01&to=2026-06-30
{
  "totalRevenue": 45890.50,
  "completedOrdersCount": 32,
  "bestSeller":  { "itemId": "…", "title": "Taladro Inalámbrico 20V", "unitsSold": 58 },
  "worstSeller": { "itemId": "…", "title": "Nivel Láser", "unitsSold": 2 },
  "totalStock": 340,
  "zeroSalesItems": [ { "itemId": "…", "title": "Escalera Plegable 3m" } ]
}
```

---

## 7. Notas de migración para el frontend

1. **Formulario de registro de asociado**: renombrar `taxId` → `rfc`, quitar `storeSlug`, validar RFC en cliente con `^([A-ZÑ&]{3,4})\d{6}([A-Z\d]{3})$` (el backend normaliza a mayúsculas).
2. **Edición de perfil de asociado**: quitar el campo RFC; agregar el bloque `storeAddress`.
3. **Perfil de cliente**: quitar los campos de dirección del formulario; construir pantalla "Mis direcciones" contra `/profile/me/addresses` (lista + marcar predeterminada + CRUD).
4. **Selects de catálogo**: primero `GET /categories` (trae subcategorías anidadas); el item se crea/filtra con `subcategoryId`. Ya no existe `category.slug`.
5. **Campo marca**: input de texto libre con autocompletado alimentado por `GET /brands?q=` — el usuario puede escribir una marca nueva.
6. **Formulario de item**: condicionar campos según `type` (producto: stock/sku/marca obligatorios; servicio: duración/modalidad/cobertura y sin stock). Para servicios mostrar el precio como `Desde $X`.
7. **Buscador**: migrar de `GET /items?...` a `POST /items/search` con body. `GET /items` queda para el listado sin filtros.
8. **Órdenes del comprador**: renderizar la orden padre con `aggregateStatus` y expandir sub-órdenes por tienda, cada una con su propio estado.
9. **Panel del asociado**: nuevas vistas — inventario propio (`GET /associate/items`, muestra `active`), gestión de sub-órdenes con transiciones `PAID → PROCESSING → COMPLETED` / `CANCELLED`, y reporte de ventas con filtros de fecha.
10. **Errores**: manejar 409 al mezclar tipos en el carrito y al duplicar SKU/RFC/email; 400 con `errors` (mapa campo→mensaje) en validaciones; 422 por stock insuficiente.
11. **JWT sin cambios**: `sub` = userId, claims `email` y `role` (ahora puede venir `ADMIN`). Expira en 120 min.
