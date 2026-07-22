# Planificación de Mejoras — Reestructuración de la API

> Documento maestro de trabajo. Consolida y ordena todo lo definido en `sig-paso.md` y resuelto en `dudastecnicas.md` → `dudastecnicas4.md`. Se construye **paso por paso**; cada paso se valida contigo antes de avanzar al siguiente.

## Contexto técnico base (estado actual del backend)

- Spring Boot 3 / Spring Security 6, OAuth2 Resource Server con JWT HS256.
- Sin prefijo de contexto: la API vive en la raíz (`/`), puerto `8080`.
- Roles actuales: `CUSTOMER`, `ASSOCIATE` (no existe `ADMIN` todavía).
- Persistencia JPA con `ddl-auto: update` sobre SQL Server; entidades heredan de `Auditable` (`id` UUID, `createdAt`, `updatedAt`).
- Envoltura de respuesta universal: `ApiResponse<T>` (`code`, `error`, `message`, `data`). Paginación: `PageResponse<T>`.

## Índice de pasos

| Paso | Título | Estado |
|------|--------|--------|
| 0 | Preparación — Purga de base de datos | ✅ Detallado abajo |
| 1 | Módulo de Autenticación | ✅ Detallado abajo |
| 2 | Asociados / Inventario (productos + servicios) | ⏳ Pendiente |
| 3 | Catálogo: Categorías, Subcategorías y Marcas + Rol ADMIN | ⏳ Pendiente |
| 4 | Catálogo de productos y servicios (búsqueda tipo marketplace) | ⏳ Pendiente |
| 5 | Borrado lógico global | ⏳ Pendiente |
| 6 | Direcciones múltiples, órdenes multi-asociado y reportes | ⏳ Pendiente |
| 8 | Actualización de Postman y Swagger UI | ⏳ Pendiente |
| 9 | `actualizacion-backend.md` para el equipo frontend | ⏳ Pendiente |

> La numeración respeta `sig-paso.md`, que salta del Paso 6 al Paso 8 (no existe un Paso 7).

## Convenciones de ejecución

- Cada cambio de entidad implica regenerar esquema vía `ddl-auto: update`; los cambios destructivos (renombrar/eliminar columnas) se apoyan en la purga previa del Paso 0.
- Toda validación de negocio se hace en el backend, nunca solo confiando en el frontend.
- Los nombres de campos, endpoints y clases indicados aquí son los definitivos; la implementación debe seguirlos al pie de la letra para que Postman/Swagger/frontend queden consistentes.

---

# Paso 0 — Preparación: Purga de base de datos

**Objetivo:** dejar la base limpia de registros antes de aplicar los cambios de esquema (renombrados de columnas, nuevas tablas, borrado lógico), evitando conflictos con datos previos inconsistentes.

**Decisión tomada:** script SQL manual entregado en un `.md`, ejecutado por ti directamente contra la base. No se crea ningún endpoint de "borrar todo" (riesgo de seguridad).

### Entregable

- Archivo nuevo: `md/purga-base-de-datos.md`.
- Contenido: script SQL para SQL Server que vacía todas las tablas respetando el orden de dependencias (FKs). Orden tentativo (de hoja a raíz):
  1. `order_items`
  2. `orders`
  3. `cart_items`
  4. `carts`
  5. `item_images`
  6. `items`
  7. `associate_profiles`
  8. `categories`
  9. `users`
- Estrategia: `DELETE` en orden de dependencias (no `TRUNCATE`, porque SQL Server no permite `TRUNCATE` sobre tablas referenciadas por FK). Se incluye una nota para deshabilitar/rehabilitar constraints si se prefiere `TRUNCATE`.
- El script se **regenerará al final** (antes del Paso 8) para incluir las tablas nuevas creadas en los pasos 2–6 (`subcategories`, `brands`, `user_addresses`, tabla de sub-órdenes, etc.), de modo que quede una purga completa y actualizada.

### Criterio de aceptación

- Ejecutar el script deja todas las tablas en 0 registros sin errores de FK.

---

# Paso 1 — Módulo de Autenticación

**Objetivo:** endurecer el registro de asociados (RFC formal y único, slug de tienda autogenerado) y confirmar el manejo de duplicados.

## 1.1 Asociados

### 1.1.1 Renombrar `taxId` → `rfc` con formato y unicidad

**Decisiones tomadas:**
- `taxId` pasa a llamarse `rfc` en toda la cadena (entidad, DTOs, servicio, repositorio).
- El RFC es **obligatorio** y **único** (irrepetible entre asociados).
- Se valida el **formato de RFC mexicano** (persona física 13 caracteres, persona moral 12).

**Cambios por archivo:**

- `auth/entity/AssociateProfile.java`
  - Renombrar el campo `private String taxId;` → `private String rfc;`.
  - Anotar: `@Column(nullable = false, unique = true)`.

- `auth/dto/RegisterAssociateRequest.java`
  - Renombrar `taxId` → `rfc`.
  - Anotaciones: `@NotBlank` + `@Pattern(regexp = RFC_REGEX, message = "El RFC no tiene un formato válido.")`.
  - `RFC_REGEX` propuesto (acepta física y moral, mayúsculas): `^([A-ZÑ&]{3,4})\\d{6}([A-Z\\d]{3})$`.
  - Normalizar a mayúsculas en el servicio antes de validar unicidad (evitar duplicados por diferencia de mayúsculas/minúsculas).

- `auth/dto/AssociateProfileResponse.java`
  - Renombrar `taxId` → `rfc`.

- `auth/dto/AssociateProfileUpdateRequest.java`
  - **Decisión de diseño (la tomo como tech lead):** el RFC es un identificador fiscal permanente → **se elimina del request de actualización** (no editable tras el registro). Si en el futuro se necesita corregir, será una operación administrativa aparte con re-validación de unicidad. Esto evita reintroducir el chequeo de duplicados en el flujo de edición de perfil.
  - Confírmame si prefieres mantenerlo editable; por defecto queda inmutable.

- `auth/repository/AssociateProfileRepository.java`
  - Agregar `boolean existsByRfc(String rfc);`.

- `auth/service/AuthService.java` (`registerAssociate`)
  - Tras validar email duplicado, normalizar `rfc` a mayúsculas y validar: `if (associateProfileRepository.existsByRfc(rfc)) throw new DuplicateResourceException("Ya existe un asociado registrado con el RFC: " + rfc);`.

### 1.1.2 `storeSlug` autogenerado por el backend

**Decisiones tomadas:**
- El cliente **ya no envía** `storeSlug` en el registro.
- El backend lo genera a partir del `storeName`.
- Estrategia de colisión: **sufijo aleatorio corto** (ej. `ferreteria-el-tornillo-x7k2`).
- El slug es **estable**: se genera una sola vez en el registro y no cambia aunque luego se edite el `storeName` (se usa como referencia pública/URL).

**Cambios por archivo:**

- `auth/dto/RegisterAssociateRequest.java`
  - **Eliminar** el campo `storeSlug` (ya no lo envía el cliente).

- Nueva utilidad reutilizable: `common/util/SlugGenerator.java`
  - Método `String toSlug(String text)`: normaliza (minúsculas, quita acentos con `Normalizer` de la stdlib, reemplaza no-alfanuméricos por `-`, colapsa guiones repetidos, recorta guiones de los extremos).
  - Método `String randomSuffix()`: sufijo corto alfanumérico (ej. 4 caracteres).
  - Se reutilizará en el Paso 2 para el slug de los items → se define aquí una sola vez.

- `auth/service/AuthService.java` (`registerAssociate`)
  - Generar slug base con `slugGenerator.toSlug(request.storeName())`.
  - Si `associateProfileRepository.existsByStoreSlug(base)` es `true`, anexar `"-" + slugGenerator.randomSuffix()` y reintentar hasta obtener uno libre.
  - Inyectar `SlugGenerator` como dependencia del servicio.

> Nota: `existsByStoreSlug` ya existe en el repositorio (se usa hoy para validar el slug enviado). Se conserva; solo cambia quién produce el valor.

## 1.2 Registro de usuarios en general

**Decisión tomada:** los únicos casos de duplicado a controlar son:
1. **Email duplicado** (clientes y asociados) — ya cubierto por `DuplicateResourceException` en `registerCustomer` y `registerAssociate`. Sin cambios.
2. **RFC duplicado** (solo asociados) — se agrega en 1.1.1.

No se requiere lógica adicional en este paso.

## Impacto y consideraciones del Paso 1

- **Esquema:** renombrar la columna `tax_id` → `rfc` y agregar constraint `UNIQUE`. Con la base ya purgada (Paso 0), `ddl-auto: update` aplica el cambio sin conflicto de datos. Verificar que Hibernate genere la columna con el nombre esperado (si hiciera falta, fijar `@Column(name = "rfc")`).
- **Contrato de API roto intencionalmente:** el body de `POST /auth/register/associate` cambia (sale `storeSlug`, `taxId` → `rfc`). Se documentará en el Paso 9 para el frontend y se actualizará en Postman/Swagger en el Paso 8.
- **Seguridad:** sin cambios en el flujo JWT; el token sigue llevando `sub`, `email`, `role`.

## Criterios de aceptación del Paso 1

1. Registrar un asociado sin enviar `storeSlug` funciona y persiste un slug derivado del `storeName`.
2. Registrar dos asociados con el mismo `storeName` produce slugs distintos (el segundo con sufijo aleatorio).
3. Registrar un asociado con RFC de formato inválido devuelve error de validación (400).
4. Registrar dos asociados con el mismo RFC devuelve `DuplicateResourceException` (409).
5. Registrar dos usuarios (cliente o asociado) con el mismo email sigue devolviendo error de duplicado.
6. El `AssociateProfileResponse` expone `rfc` (no `taxId`).

---

# Paso 2 — Asociados / Inventario: productos y servicios

**Objetivo:** unificar productos tangibles y servicios intangibles en una sola entidad `Item` con un discriminador `type`, agregar los campos que cada tipo necesita, autogenerar el slug del item y permitir que el asociado gestione (ver, crear, editar, eliminar) tanto productos como servicios con el mismo flujo.

**Dependencia:** las relaciones `Item → Subcategory` e `Item → Brand` se materializan aquí pero sus entidades (`Subcategory`, `Brand`) y su administración se definen en el **Paso 3**. Al implementar, crear primero las entidades del Paso 3 y luego las relaciones de este paso.

## 2.1 Discriminador de tipo (producto vs servicio)

**Decisión tomada:** un solo `Item` con campo `type` (`PRODUCT` / `SERVICE`); `stock` pasa a opcional.

- Nuevo enum `catalog/entity/ItemType.java`: `PRODUCT`, `SERVICE`.
- `catalog/entity/Item.java`:
  - Agregar `@Enumerated(EnumType.STRING) @Column(nullable = false) private ItemType type;`.
  - `stock`: cambiar a **opcional** (`@Column(nullable = true)`), `null` para servicios, obligatorio (> = 0) para productos. La obligatoriedad por tipo se valida en el servicio, no con anotación de columna.

## 2.2 Campos nuevos de `Item`

**Comunes (producto y servicio):**
- `sku` (`String`): **único por asociado** (constraint compuesto `associate_id` + `sku`), no global. Definir en `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"associate_id", "sku"}))`. Obligatorio para productos; para servicios se decidirá según validación de servicio (por defecto opcional).
- `model` (`String`, opcional).
- `brand` (`@ManyToOne(fetch = LAZY)` a `Brand`, **opcional** — obligatorio para producto, opcional para servicio). Ver Paso 3 para la entidad `Brand` y el mecanismo de autoservicio.

**Solo servicio (todos opcionales, `null` para productos):**
- `durationValue` (`Integer`) + `durationUnit` (enum `catalog/entity/DurationUnit.java`: `HORAS`, `DIAS`, `SEMANAS`).
- `serviceMode` (enum `catalog/entity/ServiceMode.java`: `PRESENCIAL`, `REMOTO`, `AMBOS`).
- `coverageZone` (`String`, texto libre, ej. "Ciudad de México y área metropolitana").
- **Presupuesto "desde":** se **reutiliza el campo `price` existente**; para servicios se interpreta como "precio desde" y el frontend antepone el texto "Desde $". No se agrega columna nueva.

## 2.3 Slug de item autogenerado

**Decisión tomada:** igual que el store slug (Paso 1) — base derivada del `title`, sufijo aleatorio corto en colisión, slug estable.

- `Item.slug` deja de venir en el request; se genera en el servicio con la utilidad `common/util/SlugGenerator` (creada en el Paso 1).
- El `InventoryService` usa `itemRepository.existsBySlug(base)` (agregar si no existe) y reintenta con sufijo aleatorio en colisión.

## 2.4 DTOs de inventario

- `associate/dto/ItemCreateRequest.java` (reestructurar):
  - **Quitar** `slug` (autogenerado).
  - **Quitar** `categoryId`; **agregar** `subcategoryId` (`@NotNull UUID`) — ver Paso 3.
  - **Agregar** `type` (`@NotNull ItemType`).
  - **Agregar** `brandName` (`String`, texto libre; el backend hace "buscar o crear" — ver Paso 3.3). Obligatorio para producto, opcional para servicio (validado en servicio).
  - **Agregar** `sku` (`String`), `model` (`String`, opcional).
  - **Agregar** campos de servicio: `durationValue`, `durationUnit`, `serviceMode`, `coverageZone` (todos opcionales; requeridos/ignorados según `type`, validado en servicio).
  - `price` sigue obligatorio (`@DecimalMin > 0`); `stock` pasa a opcional a nivel de bean, la regla real la aplica el servicio según `type`.
- `associate/dto/ItemUpdateRequest.java`: mismos campos que el create.
- **Validación condicional por tipo** en `InventoryService` (no con anotaciones, porque depende de `type`):
  - `PRODUCT`: `stock` y `brandName` obligatorios; campos de servicio deben ir nulos (o se ignoran).
  - `SERVICE`: `stock` debe ir nulo; `brandName` opcional; campos de servicio opcionales.
  - Se lanza una excepción de validación de negocio (400) si la combinación es incoherente.

## 2.5 Respuestas de item

- `catalog/dto/ItemResponse.java` (detalle completo): agregar `type`, `sku`, `model`, `brand` (objeto `BrandResponse`), `subcategory` (objeto con su categoría padre), y bloque de servicio (`durationValue`, `durationUnit`, `serviceMode`, `coverageZone`) — estos últimos `null` para productos.
- `catalog/dto/ItemSummaryResponse.java` (tarjetas de catálogo): agregar `type` y `brand`; reemplazar `category` por `subcategory` (con nombre de categoría padre incluido). Mantener liviano para el listado.

## 2.6 Endpoints de inventario (sin cambio de rutas)

Las rutas existentes ya cubren productos y servicios de forma unificada (el `type` viaja en el body):
- `POST /associate/items` — crea producto o servicio (`@PreAuthorize("hasRole('ASSOCIATE')")`).
- `PUT /associate/items/{itemId}` — edita.
- `DELETE /associate/items/{itemId}` — pasa a **borrado lógico** (ver Paso 5).
- **Nuevo:** `GET /associate/items` — lista el inventario del asociado autenticado (productos y servicios, incluyendo inactivos, para que gestione su catálogo). Paginado y con filtro opcional por `type`.

## Criterios de aceptación del Paso 2

1. Crear un producto sin `slug` persiste un slug derivado del `title`; dos productos con el mismo título → slugs distintos.
2. Crear un producto exige `stock`, `brandName`, `sku`; crear un servicio los relaja según las reglas por tipo.
3. Un `sku` repetido dentro del mismo asociado se rechaza; el mismo `sku` en otro asociado se permite.
4. `brandName` nuevo crea la marca; `brandName` existente (case-insensitive) la reutiliza (ver Paso 3).
5. `GET /associate/items` devuelve el inventario propio incluyendo items inactivos.

---

# Paso 3 — Catálogo: Categorías, Subcategorías, Marcas y Rol ADMIN

**Objetivo:** construir la jerarquía Categoría → Subcategoría, introducir Marcas globales por autoservicio, habilitar la gestión administrativa del catálogo mediante un nuevo rol `ADMIN`, y entregar el seed de datos por Postman.

## 3.1 Categorías y Subcategorías

**Decisiones tomadas:** se elimina `slug` de categoría; el item se clasifica por `subcategoryId`; el servicio puede resolver la categoría padre a partir de la subcategoría.

- `catalog/entity/Category.java`:
  - **Eliminar** el campo `slug` (y su constraint `unique`).
  - Conservar `name` (obligatorio).
  - Agregar `active` (Paso 5).
- Nueva entidad `catalog/entity/Subcategory.java`:
  - `name` (obligatorio).
  - `@ManyToOne(fetch = LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;` (categoría padre).
  - `active` (Paso 5).
- `catalog/entity/Item.java`: reemplazar la relación `@ManyToOne Category category` por `@ManyToOne Subcategory subcategory` (`category_id` → `subcategory_id`). La categoría padre se obtiene vía `item.getSubcategory().getCategory()`.

**DTOs / respuestas:**
- `catalog/dto/CategoryResponse.java`: quitar `slug`; queda `id`, `name`. Opcionalmente incluir la lista de subcategorías.
- Nuevo `catalog/dto/SubcategoryResponse.java`: `id`, `name`, y datos de la categoría padre (`categoryId`, `categoryName`).
- El `CategoryService` expone un método para **resolver la categoría padre por `subcategoryId`** (requerido por tu respuesta al punto 1 de `dudastecnicas3.md`).

**Endpoints:**
- `GET /categories` — listado (público). Devuelve categorías con sus subcategorías anidadas.
- `GET /subcategories/{id}` — detalle de subcategoría con su categoría padre (público).
- **CRUD administrativo (solo ADMIN):** `POST/PUT/DELETE /categories` y `POST/PUT/DELETE /subcategories` (`@PreAuthorize("hasRole('ADMIN')")`). El `DELETE` es borrado lógico (Paso 5).

## 3.2 Marcas (Brands) globales

**Decisiones tomadas:** marca independiente/global (no ligada a subcategoría); una sola marca por item (opcional en servicios); creación por **autoservicio** ("buscar o crear") al registrar el producto; el ADMIN mantiene/edita/fusiona/desactiva.

- Nueva entidad `catalog/entity/Brand.java`:
  - `name` (obligatorio, único case-insensitive).
  - `active` (Paso 5).
- Nuevo `catalog/repository/BrandRepository.java`:
  - `Optional<Brand> findByNameIgnoreCase(String name);` (para "buscar o crear").
  - Método de búsqueda por nombre parcial: `List<Brand> findByNameContainingIgnoreCase(String q);` (o vía Specification).
- Nuevo `catalog/dto/BrandResponse.java`: `id`, `name`.
- **Endpoints de marca:**
  - `GET /brands` — listado completo (público, para selects/autocompletado). Acepta parámetro **opcional** `?q=` para búsqueda por nombre parcial (mismo estilo que el buscador de productos).
  - `GET /brands/{id}` — detalle por id (público).
  - **Administrativos (solo ADMIN):** `PUT /brands/{id}` (renombrar/fusionar), `DELETE /brands/{id}` (borrado lógico). No hay `POST` público de marca: las marcas nacen del autoservicio.

## 3.3 Autoservicio de marca al crear producto

- En `InventoryService.create()` / `update()`:
  - Si `brandName` viene informado: `brandRepository.findByNameIgnoreCase(brandName)`; si no existe, se crea una `Brand` nueva con ese nombre (normalizado/trim). Se asigna al item.
  - Si `brandName` viene vacío y el `type` es `SERVICE`: se permite `brand = null`.
  - Si `type` es `PRODUCT` y `brandName` viene vacío: error de validación (marca obligatoria para producto).
- El `ItemResponse` devuelve el objeto `BrandResponse` completo para que el frontend lo use como filtro después.

## 3.4 Rol ADMIN

**Decisiones tomadas (alcance aprobado en `dudastecnicas2.md` punto 6):**

- `auth/entity/Role.java`: agregar `ADMIN`.
- **Lo que hace el ADMIN:**
  - CRUD completo de Categorías, Subcategorías y Marcas.
  - Ver listado de todos los usuarios (clientes y asociados) con datos no sensibles (nombre, email, rol, fecha de registro, estado activo/inactivo) — sin contraseñas.
  - Activar/desactivar cualquier usuario o item (borrado lógico del Paso 5).
  - Ver el catálogo completo de cualquier asociado (solo lectura, para moderación).
- **Lo que NO hace el ADMIN:**
  - No crea/edita/elimina items de un asociado (eso es exclusivo del asociado dueño).
  - No participa en checkout ni ve carritos ajenos.
  - No gestiona el estado de las órdenes de un asociado.
- **Endpoints administrativos nuevos (bajo `/admin`, todos `@PreAuthorize("hasRole('ADMIN')")`):**
  - `GET /admin/users` — listado paginado de usuarios con datos no sensibles (nuevo DTO `AdminUserSummaryResponse`).
  - `PUT /admin/users/{userId}/status` — activar/desactivar usuario.
  - `PUT /admin/items/{itemId}/status` — activar/desactivar item.
  - `GET /admin/items` — catálogo global (solo lectura, incluye inactivos), con filtros.
  - (El CRUD de categorías/subcategorías/marcas vive en sus propios controllers con `hasRole('ADMIN')`, no se duplica bajo `/admin`.)

**Autenticación del ADMIN:**
- Mismo mecanismo JWT (el claim `role` ya soporta cualquier valor del enum).
- El ADMIN **no** se auto-registra por `/auth/register/*`.
- **Creación de administradores:** `POST /auth/register/admin`, protegido con `@PreAuthorize("hasRole('ADMIN')")` (solo un ADMIN crea otro ADMIN).
- **Bootstrap del primer ADMIN (problema del huevo y la gallina):** un inicializador de datos (`CommandLineRunner`/`ApplicationRunner`) que, al arrancar, si no existe ningún ADMIN, crea uno con credenciales tomadas de variables de entorno/propiedades (`app.admin.email`, `app.admin.password`). Así el primer admin no depende de tocar la base a mano y las credenciales no quedan hardcodeadas.

## 3.5 Seed de datos por Postman

**Decisión tomada:** las marcas se llenan solas por autoservicio, así que el seed cubre solo categorías y subcategorías.

- Archivo nuevo: `md/postman-seed-catalogo.md`.
- Contenido: peticiones (autenticadas como ADMIN) para crear **10 categorías**, cada una con **3 subcategorías** (30 subcategorías en total), inspiradas en los rubros más comunes de Amazon/Mercado Libre (Electrónica, Hogar, Moda, Deportes, Juguetes, Herramientas, Belleza, Automotriz, Libros, Mascotas, etc.).
- Las marcas se poblarán naturalmente cuando los asociados registren productos (autoservicio), como acordamos.

## Criterios de aceptación del Paso 3

1. `GET /categories` devuelve categorías sin `slug`, con subcategorías anidadas.
2. Crear categoría/subcategoría/marca sin ser ADMIN → 403; siendo ADMIN → éxito.
3. Registrar un producto con una marca nueva la crea; con una existente (aunque difiera en mayúsculas) la reutiliza.
4. Resolver la categoría padre a partir de un `subcategoryId` funciona.
5. Al arrancar sin ADMIN previo, el bootstrap crea el primer ADMIN desde configuración.
6. El seed de Postman deja 10 categorías × 3 subcategorías.

---

# Paso 4 — Catálogo de productos y servicios (búsqueda tipo marketplace)

**Objetivo:** ofrecer un buscador universal estilo Amazon/Mercado Libre mediante `POST` con body JSON, más filtros y ordenamientos, aplicable por igual a productos y servicios.

## 4.1 Endpoint de búsqueda

**Decisiones tomadas:** buscador universal por un solo campo `q`; parámetros en body JSON (no query gigante); se conserva `GET /items` para traer todo sin filtros.

- **Nuevo:** `POST /items/search` (público). Body `catalog/dto/ItemSearchRequest.java`:
  - `q` (`String`, opcional): término único de búsqueda.
  - `subcategoryId` (`UUID`, opcional).
  - `brandId` (`UUID`, opcional).
  - `type` (`ItemType`, opcional): filtra productos o servicios.
  - `priceMin`, `priceMax` (`BigDecimal`, opcionales).
  - `sortBy` (`String`, default `recent`): `recent`, `bestsellers`, `price_asc`, `price_desc`.
  - `page` (default 0), `size` (default 20).
- **`q` compara (LIKE, case-insensitive, tipo "se parece")** contra: `title`, `slug`, `model`, `sku`, nombre de la tienda (`associate.storeName`) y **nombre de la marca** (`brand.name`). Es decir, si el usuario escribe el nombre de una tienda aparecen sus items; si escribe un SKU, los que lo tengan; etc.
- Respuesta: `ApiResponse<PageResponse<ItemSummaryResponse>>`.
- Solo devuelve items **activos** (Paso 5).

## 4.2 `GET /items` (sin filtros)

- Se conserva: trae todos los items activos, **paginado** (`page`/`size`), orden por defecto "más recientes". Sin `q` ni filtros.

## 4.3 Implementación de la búsqueda

- Usar **JPA Specifications** (Spring Data ya disponible) para componer dinámicamente los filtros opcionales + el `OR` de `q` sobre los campos/joins indicados. Evita concatenar SQL y maneja los `null` de forma limpia.
- El `sortBy` se traduce a `Sort` (reutilizando la lógica que hoy vive en `ItemController.resolveSort`, movida al servicio).
- `salesCount` (ya existente) alimenta `bestsellers`; para servicios se interpreta como número de contrataciones.
- **Retiro** de `catalog/dto/ItemFilterCriteria.java` (reemplazado por `ItemSearchRequest`) y limpieza de los `@RequestParam` del `ItemController.search` actual.

## Criterios de aceptación del Paso 4

1. `POST /items/search` con `q` = nombre de una tienda devuelve los items de esa tienda.
2. `q` con un SKU, modelo o marca devuelve coincidencias parciales, sin importar mayúsculas.
3. Filtrar por `type = SERVICE` devuelve solo servicios; por `subcategoryId`/`brandId`/rango de precio, acota correctamente.
4. `sortBy` ordena por recientes, más vendidos y precio asc/desc.
5. `GET /items` sigue devolviendo todo (activo) paginado, sin filtros.
6. Items inactivos nunca aparecen en búsqueda ni en `GET /items`.

---

# Paso 5 — Borrado lógico global

**Objetivo:** que ningún `DELETE` borre físicamente; en su lugar se marca el registro como inactivo mediante una bandera booleana.

**Decisiones tomadas:**
- Bandera `active` (boolean, default `true`) en: `User`, `Item`, `Category`, `Subcategory`, `Brand`. **No** en `Order`, `Cart`, `CartItem`.
- Item inactivo: permanece referenciado en órdenes pasadas (FK), pero **desaparece** del catálogo público y de la búsqueda.
- Usuario inactivo: **puede seguir haciendo login** (para reactivar su cuenta y ver historial/inventario). El login no se rechaza por `active = false`.
- Asociado desactivado: sus items pasan a inactivos **en cascada automáticamente**.
- Asociado reactivado: sus items **no** se reactivan solos; el asociado los reactiva **manualmente** uno por uno.
- El ADMIN puede activar/desactivar cualquier usuario o item (Paso 3.4).

## 5.1 Cambios de entidad

- Agregar `@Column(nullable = false) @Builder.Default private boolean active = true;` a `User`, `Item`, `Category`, `Subcategory`, `Brand`.

## 5.2 Semántica de los DELETE

- `DELETE /associate/items/{itemId}`: set `active = false` (borrado lógico), no `repository.delete()`.
- `DELETE /categories/{id}`, `DELETE /subcategories/{id}`, `DELETE /brands/{id}` (ADMIN): borrado lógico.
- Reactivación de item por el asociado: `PUT /associate/items/{itemId}` o un endpoint dedicado `PUT /associate/items/{itemId}/status` para volver a `active = true`.

## 5.3 Filtrado por `active` en consultas

- Las consultas públicas (catálogo, búsqueda, `GET /categories`, `GET /brands`) filtran **explícitamente** `active = true`.
- **Decisión de diseño (tech lead):** se filtra de forma explícita en cada consulta pública (o vía Specification que añade `active = true`), **en lugar** de un `@Where`/`@SQLRestriction` global de Hibernate. Motivo: el ADMIN necesita ver también los inactivos; un filtro global lo bloquearía y obligaría a rodeos. Explícito = control por caso de uso.
- Consultas del asociado sobre su propio inventario y del ADMIN: incluyen inactivos.

## 5.4 Cascada de desactivación del asociado

- Al desactivar un `User` con rol `ASSOCIATE` (por el propio asociado o por un ADMIN): en la misma transacción, marcar `active = false` todos los `Item` cuyo `associate` sea ese usuario.
- Al reactivar el asociado: **no** se tocan los items (reactivación manual, como se decidió).

## Criterios de aceptación del Paso 5

1. `DELETE` de item/categoría/subcategoría/marca deja el registro en base con `active = false` (no lo borra).
2. Un item inactivo no aparece en `GET /items` ni en `POST /items/search`, pero sí sigue visible en el historial de órdenes que lo referencian.
3. Un usuario inactivo puede hacer login y ver su historial/inventario.
4. Desactivar un asociado marca todos sus items como inactivos automáticamente.
5. Reactivar al asociado deja sus items inactivos hasta que él los reactive manualmente.

---

# Paso 6 — Direcciones múltiples, órdenes multi-asociado y reportes

**Objetivo:** soportar varias direcciones de envío con predeterminada, dirección de tienda para el asociado, dividir las compras en sub-órdenes por asociado con estados gestionables, y entregar un reporte de ventas desglosado.

## 6.1 Direcciones

**Decisiones tomadas:** `Address` deja de ser embebido único en `User` y pasa a entidad propia `UserAddress` (lista, con `isDefault`). Tanto `CUSTOMER` como `ASSOCIATE` la usan como compradores. El asociado gana además una `storeAddress` única (dirección física de la tienda, no lista).

- Nueva entidad `auth/entity/UserAddress.java`:
  - `@ManyToOne(fetch = LAZY) private User user;`
  - Campos: `street`, `city`, `state`, `postalCode`, `country`.
  - `@Column private boolean isDefault;`
- `auth/entity/User.java`: **eliminar** el `@Embedded Address shippingAddress;`. Su información se reemplaza por la relación `OneToMany` a `UserAddress` (o se navega vía repositorio de direcciones).
- `auth/entity/AssociateProfile.java`: agregar `@Embedded private Address storeAddress;` (única, reutiliza el `@Embeddable Address` existente).
- **Reglas:**
  - Exactamente una `UserAddress` con `isDefault = true` por usuario; al marcar una como predeterminada, las demás se desmarcan (en el servicio).
  - La primera dirección creada se marca predeterminada automáticamente.
- **Endpoints (usuario autenticado):**
  - `GET /profile/me/addresses` — lista de direcciones del usuario.
  - `POST /profile/me/addresses` — agrega una dirección.
  - `PUT /profile/me/addresses/{addressId}` — edita.
  - `DELETE /profile/me/addresses/{addressId}` — elimina (física; no participa del borrado lógico del Paso 5).
  - `PUT /profile/me/addresses/{addressId}/default` — marca como predeterminada.
  - La `storeAddress` del asociado se gestiona dentro de `PUT /profile/me/associate` (se agrega al `AssociateProfileUpdateRequest`).
- **Checkout:** el `CheckoutService` toma por defecto la dirección `isDefault` del comprador (o una `addressId` explícita si el frontend la envía).

## 6.2 Órdenes visibles en el propio perfil

**Decisión tomada:** se refiere al usuario viendo **sus propias** órdenes (ya cubierto por `GET /orders`); solo falta el ordenamiento por fecha. (El caso de un ADMIN viendo órdenes de terceros se tratará en un archivo de dudas aparte, como acordaste.)

- `GET /orders`: agregar parámetro opcional de orden por fecha (`sort=recent|oldest`, default `recent`).

## 6.3 Órdenes multi-asociado (el cambio de arquitectura mayor)

**Decisiones tomadas:** dividir en **orden padre + sub-órdenes por asociado** (opción a, robusta). El carrito **no** puede mezclar productos y servicios (validado en backend). Estados de orden ampliados. La orden padre calcula un **estado agregado** en el backend.

### 6.3.1 Validación de carrito de un solo tipo

- En `CartService.addItem()`: si el carrito ya contiene items de un `type` y se intenta agregar uno del `type` contrario → error **409** con mensaje "No se pueden combinar productos y servicios en el mismo carrito."
- Consecuencia: cada carrito (y por tanto cada compra) es de un solo tipo, lo que simplifica la división en sub-órdenes.

### 6.3.2 Modelo de órdenes padre/hija

- `purchase/entity/Order.java` se reestructura para soportar jerarquía:
  - **Orden padre:** representa la compra completa. Guarda `user` (comprador), `total` general y fecha (de `Auditable`). No tiene un `status` propio "operativo" — su estado se **calcula** a partir de las hijas.
  - **Orden hija:** una por asociado involucrado. Guarda su `associate`, su propio `status` (`OrderStatus`), su lista de `OrderItem` y una referencia `parentOrder`.
  - Implementación sugerida: campo `@ManyToOne private Order parentOrder;` (auto-referencia) + `@OneToMany(mappedBy = "parentOrder") private List<Order> childOrders;`, más un discriminador o simplemente `parentOrder == null` ⇒ es padre. Alternativa más explícita: una entidad `OrderGroup` padre separada. **Recomendación:** auto-referencia (`parentOrder`) para no multiplicar tablas; se evalúa al implementar.
- `purchase/entity/OrderItem.java`: cada `OrderItem` cuelga de una orden **hija** (la del asociado correspondiente).

### 6.3.3 Estados de orden

- `purchase/entity/OrderStatus.java`: ampliar a `CREATED`, `PAID`, `PROCESSING`, `COMPLETED`, `CANCELLED`.
- Flujo del asociado sobre **su** sub-orden: `PAID → PROCESSING → COMPLETED`; `CANCELLED` en cualquier punto antes de `COMPLETED`.
- **Estado agregado de la orden padre (calculado, no persistido)** — nuevo enum de solo respuesta `AggregateOrderStatus`: `EN_PROCESO`, `PARCIALMENTE_COMPLETADA`, `COMPLETADA`, `CANCELADA`. Derivado de las hijas:
  - Todas `COMPLETED` → `COMPLETADA`.
  - Algunas `COMPLETED` y otras no → `PARCIALMENTE_COMPLETADA`.
  - Todas `CANCELLED` → `CANCELADA`.
  - En cualquier otro caso → `EN_PROCESO`.

### 6.3.4 Checkout con división

- `CheckoutService.checkout()`:
  1. Validar carrito no vacío y de un solo tipo.
  2. Crear la orden **padre** para el comprador.
  3. Agrupar los `CartItem` por `associate` del item.
  4. Por cada grupo, crear una orden **hija** (`status = PAID`, o `CREATED`→`PAID` según flujo de pago) con sus `OrderItem` y `parentOrder` apuntando al padre.
  5. Incrementar `salesCount` de cada item (contrataciones para servicios).
  6. Vaciar el carrito.
- Respuesta: la orden padre con sus hijas y el estado agregado.

### 6.3.5 Gestión de órdenes por el asociado

- **Nuevos endpoints (`@PreAuthorize("hasRole('ASSOCIATE')")`), bajo `/associate/orders`:**
  - `GET /associate/orders` — sub-órdenes del asociado autenticado, filtrables por `status` (por procesar / completadas / canceladas) y por rango de fecha.
  - `PUT /associate/orders/{orderId}/status` — avanza el estado de su sub-orden (validando transiciones permitidas).
- El asociado solo ve/edita las sub-órdenes que le pertenecen.

### 6.3.6 Respuestas de orden

- `purchase/dto/OrderResponse.java`: adaptar para reflejar la jerarquía. La respuesta al comprador incluye: datos del padre, `aggregateStatus` calculado, y la lista de sub-órdenes (cada una con su asociado, `status` y sus `OrderItemResponse`).
- Nueva vista para el asociado: `AssociateOrderResponse` (su sub-orden con datos del comprador no sensibles y líneas que le corresponden).

## 6.4 Reporte de ventas del asociado

**Decisiones tomadas:** desglose general en un JSON, con filtro por rango de fecha (default = todo el histórico). Incluye cuánto ha ganado, producto más vendido, menos vendido (excluyendo los de 0 ventas; si todos son 0 → `null`), stock total en tiempo real, y **además un arreglo de productos con 0 ventas** para que el asociado decida si retirarlos.

- **Nuevo endpoint:** `GET /associate/sales-report` (`@PreAuthorize("hasRole('ASSOCIATE')")`), con parámetros opcionales `from` y `to` (fechas). Sin rango ⇒ todo el histórico.
- **DTO de respuesta `AssociateSalesReportResponse`:**
  - `totalRevenue` (`BigDecimal`): suma ganada (sub-órdenes completadas del asociado, dentro del rango si se especifica).
  - `completedOrdersCount` (`long`).
  - `bestSeller` (producto/servicio más vendido): `{ itemId, title, unitsSold }` — histórico por defecto, o dentro del rango si se envía.
  - `worstSeller` (menos vendido **entre los que sí han vendido**): mismo shape; `null` si ninguno ha vendido.
  - `totalStock` (`long`): suma de `stock` de todos los items **activos** del asociado, en **tiempo real** (independiente del rango).
  - `zeroSalesItems` (`List`): items del asociado con `salesCount = 0` (`{ itemId, title }`), para evaluar retiro.
- **Implementación:** consultas de agregación sobre las sub-órdenes/`OrderItem` del asociado (con `group by` para rankings). El `totalStock` y `zeroSalesItems` se calculan sobre `Item` (no dependen de fechas).

## Criterios de aceptación del Paso 6

1. Un usuario puede tener varias direcciones; marcar una como predeterminada desmarca las demás; la primera se marca sola.
2. El asociado puede fijar su `storeAddress` desde la edición de perfil.
3. Agregar un producto y luego un servicio al mismo carrito → 409.
4. Un checkout con items de 2 asociados crea 1 orden padre + 2 hijas, cada hija con su estado.
5. El comprador ve el estado agregado (`PARCIALMENTE_COMPLETADA`, etc.) calculado por el backend.
6. El asociado ve y gestiona solo sus sub-órdenes y avanza sus estados con transiciones válidas.
7. El reporte de ventas responde el JSON desglosado, respeta el rango de fechas (default histórico) y trae el arreglo de items sin ventas.

---

# Paso 8 — Actualización de Postman y Swagger UI

**Objetivo:** dejar la documentación viva y la colección de pruebas alineadas con todos los cambios de los pasos 0–6.

- **Swagger / OpenAPI (`springdoc`):**
  - Revisar que todos los endpoints nuevos y modificados queden anotados (`@Operation`, `@Tag`, `@ApiResponse`) con descripciones claras.
  - Verificar que los DTOs nuevos/cambiados (requests con `type`, `brandName`, búsqueda, direcciones, órdenes jerárquicas, reporte) se reflejen en los esquemas.
  - Documentar la autenticación Bearer y los roles requeridos por endpoint.
- **Postman:**
  - Actualizar la colección existente: rutas nuevas (`/items/search`, `/brands`, `/subcategories`, `/admin/**`, `/associate/orders`, `/associate/sales-report`, `/profile/me/addresses/**`), cuerpos JSON nuevos y renombrados (`rfc`, sin `storeSlug`, `type`, etc.).
  - Agregar/actualizar variables de entorno y scripts de token (login → guarda `access_token`).
  - Incluir la colección de seed del Paso 3.5.
- **Regenerar** el script de purga (`md/purga-base-de-datos.md`) para incluir las tablas nuevas (`subcategories`, `brands`, `user_addresses`, órdenes hijas, etc.).

## Criterio de aceptación del Paso 8

- Swagger UI refleja el 100% de los endpoints actuales; la colección de Postman ejecuta el flujo completo (registro → login → catálogo → carrito → checkout → gestión de órdenes → reporte) sin ajustes manuales de contrato.

---

# Paso 9 — `actualizacion-backend.md` para el equipo frontend

**Objetivo:** entregar al equipo Angular un documento único, muy detallado y estructurado, con todos los cambios para que adapten el frontend.

- **Archivo nuevo:** `md/actualizacion-backend.md`.
- **Contenido mínimo:**
  1. **Resumen de cambios** por módulo (qué cambió y por qué), destacando los **breaking changes** de contrato (registro de asociado, catálogo por subcategoría, búsqueda por `POST`, órdenes jerárquicas).
  2. **Catálogo completo de endpoints** (método, ruta, rol requerido, request, response) ya en su estado final.
  3. **Interfaces TypeScript** para Angular de cada request y response nuevos/modificados (mapeando `UUID→string`, `BigDecimal/Long/int→number`, `Instant→string` ISO, enums Java → union types TS): `ItemType`, `DurationUnit`, `ServiceMode`, `OrderStatus`, `AggregateOrderStatus`, `Role` (con `ADMIN`), DTOs de item/búsqueda/marca/subcategoría/dirección/orden/reporte, y las envolturas `ApiResponse<T>` / `PageResponse<T>`.
  4. **Ejemplos de payloads** reales (JSON) de los flujos clave.
  5. **Notas de migración** para el frontend (campos renombrados/eliminados, nuevos selects de subcategoría y marca, buscador universal, vista de órdenes por sub-orden con estado agregado, gestión de direcciones, panel de asociado con reporte).
- Debe quedar autosuficiente: el equipo frontend no debería necesitar leer el código del backend para adaptar sus servicios.

## Criterio de aceptación del Paso 9

- El documento cubre todos los endpoints y tipos finales, con interfaces TS listas para copiar, y es entendible sin acceso al backend.

---

> **Fin de la planificación (Pasos 0–9).** Este documento es la fuente de verdad para la ejecución; cada paso se implementará en orden, respetando dependencias (Paso 3 provee las entidades que el Paso 2 referencia; el Paso 5 atraviesa entidades creadas en pasos previos).
