# Guía de Pruebas - Postman

URL base: `http://localhost:8080`

Todas las respuestas siguen el sobre `ApiResponse`:
```json
{ "code": 200, "error": false, "message": "OK", "data": { } }
```

## Autenticación en Postman

1. Ejecuta `POST /auth/login` (o cualquier registro, que también devuelve token).
2. Copia `data.accessToken` de la respuesta.
3. En cada request protegido, ve a la pestaña **Authorization** → tipo **Bearer Token** → pega el token. O agrega el header manualmente:
   ```
   Authorization: Bearer {{token}}
   ```
4. Recomendado: guarda el token en una variable de colección `{{token}}` con un script en la pestaña **Tests** del login:
   ```js
   pm.collectionVariables.set("token", pm.response.json().data.accessToken);
   ```

El token expira en **120 minutos** (`app.jwt.expiration-minutes`). El claim `role` define el rol (`CUSTOMER` o `ASSOCIATE`).

---

## 1. Módulo Auth

### 1.1 POST /auth/register/customer
Público. Registra un cliente y devuelve token.

```json
{
  "email": "cliente1@test.com",
  "password": "password123",
  "firstName": "Ana",
  "lastName": "Pérez",
  "phone": "5551234567"
}
```
- 201 Created → `{ accessToken, tokenType, expiresInSeconds }`
- 409 Conflict si el email ya existe.
- 400 Bad Request si falta `email`/`password`/`firstName`/`lastName`, email inválido, o `password` < 8 caracteres.

### 1.2 POST /auth/register/associate
Público. Registra un asociado (vendedor) y devuelve token.

```json
{
  "email": "asociado1@test.com",
  "password": "password123",
  "firstName": "Luis",
  "lastName": "Gómez",
  "phone": "5559876543",
  "storeName": "Tienda de Luis",
  "storeSlug": "tienda-de-luis",
  "taxId": "RFC123456"
}
```
- 201 Created.
- 409 Conflict si el email o el `storeSlug` ya existen.
- 400 Bad Request si falta algún campo obligatorio.

### 1.3 POST /auth/login
Público.

```json
{
  "email": "cliente1@test.com",
  "password": "password123"
}
```
- 200 OK → token.
- 401 Unauthorized si el email o password son incorrectos.
- 400 Bad Request si `email`/`password` vienen vacíos.

### 1.4 GET /profile/me
Protegido (cualquier rol autenticado). Header `Authorization: Bearer {{token}}`.
- 200 OK → perfil de cliente o de asociado, según el rol del token.
- 401 Unauthorized sin token o token inválido/expirado.

### 1.5 PUT /profile/me/customer
Protegido, solo rol `CUSTOMER`.

```json
{
  "firstName": "Ana",
  "lastName": "Pérez",
  "phone": "5551234567",
  "street": "Av. Siempre Viva 123",
  "city": "Ciudad de México",
  "state": "CDMX",
  "postalCode": "01000",
  "country": "México"
}
```
- 200 OK.
- 403 Forbidden si el token es de un asociado.
- 400 Bad Request si falta `firstName`/`lastName`.

### 1.6 PUT /profile/me/associate
Protegido, solo rol `ASSOCIATE`.

```json
{
  "firstName": "Luis",
  "lastName": "Gómez",
  "phone": "5559876543",
  "storeName": "Tienda de Luis",
  "taxId": "RFC123456",
  "publicBio": "Vendemos artesanías locales.",
  "publicContactEmail": "contacto@tiendadeluis.com",
  "publicContactPhone": "5559876543"
}
```
- 200 OK.
- 403 Forbidden si el token es de un cliente.
- 400 Bad Request si falta `firstName`/`lastName`/`storeName`.

---

## 2. Módulo Catálogo (Público)

### 2.1 GET /categories
Público. Sin parámetros.
- 200 OK → lista de categorías.

### 2.2 GET /items
Público. Query params opcionales:
- `priceMin`, `priceMax` (BigDecimal)
- `categorySlug` (String)
- `sortBy`: `recent` (default), `bestsellers`, `price_asc`, `price_desc`
- `page` (default 0), `size` (default 20)

Ejemplo: `GET /items?categorySlug=artesanias&sortBy=price_asc&page=0&size=10`
- 200 OK → página de `ItemSummaryResponse`.
- 400 Bad Request si `priceMin`/`priceMax`/`page`/`size` no son parseables al tipo esperado.

### 2.3 GET /items/{slug}
Público.
- 200 OK → detalle del ítem.
- 404 Not Found si el slug no existe.

---

## 3. Módulo Asociados (Inventario)

Todos protegidos, solo rol `ASSOCIATE`. El asociado debe tener perfil (creado en el registro).

### 3.1 POST /associate/items

```json
{
  "title": "Taza artesanal",
  "slug": "taza-artesanal",
  "description": "Taza de barro hecha a mano.",
  "price": 150.00,
  "stock": 20,
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "images": ["https://ejemplo.com/taza.jpg"]
}
```
> Reemplaza `categoryId` por un UUID real obtenido de `GET /categories`.

- 200 OK → ítem creado.
- 403 Forbidden si el token no es de un asociado.
- 409 Conflict si el `slug` ya existe.
- 404 Not Found si `categoryId` no existe.
- 400 Bad Request si falta `title`/`slug`/`price`/`stock`/`categoryId`, `price` <= 0, o `stock` negativo.

### 3.2 PUT /associate/items/{itemId}
Mismo body que la creación.
- 200 OK.
- 403 Forbidden si el ítem no pertenece al asociado autenticado.
- 404 Not Found si el ítem no existe.
- 409 Conflict si el nuevo `slug` ya lo usa otro ítem.
- 400 Bad Request igual que en creación.

### 3.3 DELETE /associate/items/{itemId}
- 200 OK.
- 403 Forbidden si el ítem no pertenece al asociado autenticado.
- 404 Not Found si el ítem no existe.

---

## 4. Módulo Compra (Carrito y Checkout)

Todos protegidos, cualquier usuario autenticado (pensado para rol `CUSTOMER`).

### 4.1 GET /cart
- 200 OK → carrito del usuario (vacío si no tiene items).

### 4.2 POST /cart
Agrega un ítem al carrito.

```json
{
  "itemId": "00000000-0000-0000-0000-000000000000",
  "quantity": 2
}
```
- 200 OK → carrito actualizado.
- 404 Not Found si `itemId` no existe.
- 400 Bad Request si falta `itemId`/`quantity`, o `quantity` <= 0.

### 4.3 PUT /cart/items/{cartItemId}
Actualiza la cantidad de una línea del carrito.

```json
{ "quantity": 5 }
```
- 200 OK.
- 404 Not Found si `cartItemId` no existe.
- 400 Bad Request si `quantity` <= 0 o falta.

### 4.4 DELETE /cart
Vacía el carrito completo.
- 200 OK.

### 4.5 POST /purchase/checkout
Convierte el carrito en una orden. Valida stock disponible por cada línea, descuenta stock, incrementa `salesCount` y vacía el carrito.

> Nota: la ruta real registrada es `POST /checkout` (sin prefijo `/purchase`), ver `OrderController`.

- 200 OK → orden creada con items, subtotal y total.
- 404 Not Found si el carrito está vacío.
- 422 Unprocessable Entity si algún ítem no tiene stock suficiente.

### 4.6 GET /orders
Lista las órdenes del usuario autenticado.
- 200 OK.

### 4.7 GET /orders/{orderId}
- 200 OK → detalle de la orden.
- 404 Not Found si la orden no existe o no pertenece al usuario autenticado.

---

## 5. Flujo end-to-end recomendado

1. `POST /auth/register/associate` → guarda `{{token}}` del asociado.
2. `GET /categories` → copia un `categoryId` (si no hay categorías, deben precargarse en la BD).
3. `POST /associate/items` (con `{{token}}` de asociado) → crea un ítem, copia su `id`.
4. `POST /auth/register/customer` → guarda `{{token}}` del cliente (sobrescribe la variable, o usa una variable distinta `{{tokenCustomer}}`).
5. `POST /cart` (con `{{tokenCustomer}}`) → agrega el ítem creado en el paso 3.
6. `GET /cart` → confirma que la línea aparece.
7. `POST /checkout` (con `{{tokenCustomer}}`) → genera la orden, descuenta stock.
8. `GET /items/{slug}` (público) → confirma que el `stock` bajó.
9. `GET /orders` → confirma que la orden quedó registrada.

---

## 6. Errores comunes (formato `ProblemDetail`)

Todos los errores devuelven `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Error de validación en los datos enviados.",
  "errors": { "email": "must not be blank" }
}
```

| Situación | Status |
|---|---|
| Credenciales inválidas en login | 401 |
| Recurso no encontrado (item, orden, categoría, perfil) | 404 |
| Email / slug / storeSlug duplicado | 409 |
| Stock insuficiente en checkout | 422 |
| Operación sobre recurso ajeno (item de otro asociado) | 403 |
| Body inválido (`@Valid` falla) | 400 |
| Path variable con formato inválido (UUID mal formado) | 400 |
| JSON del body mal formado / ilegible | 400 |
| Error no controlado | 500 |

---

## 7. Documentación interactiva (Swagger)

Con el servidor corriendo (`./mvnw spring-boot:run`):
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Ambas rutas son públicas (no requieren token) para poder explorar los contratos antes de autenticarse.
