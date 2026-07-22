# ARQUITECTURA-API.md — Marketplace E-commerce (Spring Boot 3 / Java 21)

## 0. Supuestos y decisiones explícitas

- **Corrección tras inspeccionar el `pom.xml` real**: el esqueleto ya incluye `spring-boot-starter-security-oauth2-resource-server` (no listado explícitamente en `instrucciones-backend.md`, pero presente). Esto cambia la Fase 1: **no se añade `jjwt` ni ninguna dependencia nueva**. Se usa `JwtEncoder`/`JwtDecoder` de Nimbus (ya transitivo por ese starter) con una clave simétrica propia, sin Authorization Server externo.
- **"OAuth2.0 y JWT"** se interpreta como **JWT autoemitido (self-issued)** validado con el `oauth2-resource-server` de Spring Security, **no** un Authorization Server OAuth2 completo. Un servidor OAuth2 real (con `spring-authorization-server`) es sobre-ingeniería para un solo cliente (la propia API).
- **Perfiles extendidos de Asociado**: se modelan por **composición (One-To-One)**, no por herencia JPA (`SINGLE_TABLE`/`JOINED`). Razón: `User` es la entidad usada transversalmente por los 4 módulos; una jerarquía de herencia la acoplaría a discriminadores y columnas nulas para el 100% de los clientes. Un `AssociateProfile` opcional, creado solo cuando `role = ASSOCIATE`, es más simple, más barato de mapear con MapStruct y no exige tocar `User` si mañana aparece `ROLE_ADMIN`.
- **Catálogo "híbrido"**: filtros dinámicos (precio, categoría) se resuelven con `JpaSpecificationExecutor<Item>` (Spring Data). Los ordenamientos fijos ("más vendidos", "más recientes") son *métodos derivados* del repositorio con `Sort` explícito (`findAllByOrderBySalesCountDesc`, `findAllByOrderByCreatedAtDesc`). No se introduce Querydsl ni criteria API manual: Specification + Pageable ya cubre el 100% del caso sin dependencias nuevas.
- **Categorías planas**: el enunciado no pide subcategorías/árbol, así que `Category` es una tabla plana simple. No se modela jerarquía especulativa.
- **Direcciones**: "dirección de envío principal" es una sola dirección por cliente → `@Embeddable Address` embebido en `User`, no una tabla `Address` aparte con relación 1:N (no se pidió múltiples direcciones).
- **Multi-tenancy de asociados**: se resuelve en dos capas baratas, no con un framework de tenancy: (1) `@PreAuthorize("hasRole('ASSOCIATE')")` en el controlador para el rol, (2) verificación explícita de propiedad (`item.getAssociate().getId().equals(principal.getId())`) al inicio del método de servicio, lanzando `ForbiddenOperationException` si no coincide. Es más simple de depurar que SpEL con beans custom en `@PreAuthorize`.

---

## 1. Árbol de Carpetas (Arquitectura por Capas Clásica)

```
src/main/java/com/migracion/marketplace/
│
├── MarketplaceApplication.java
│
├── common/                              # Transversal, sin lógica de negocio
│   ├── config/
│   │   ├── SecurityConfig.java          # SecurityFilterChain, PasswordEncoder, CORS
│   │   └── JpaAuditingConfig.java       # @EnableJpaAuditing
│   ├── security/
│   │   ├── JwtService.java              # NimbusJwtEncoder: firma tokens
│   │   ├── SecurityUser.java            # UserDetails sobre User
│   │   └── CustomUserDetailsService.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice -> ProblemDetail
│   │   ├── ResourceNotFoundException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── InsufficientStockException.java
│   │   └── ForbiddenOperationException.java
│   ├── entity/
│   │   └── Auditable.java               # @MappedSuperclass: id UUID, @CreatedDate, @LastModifiedDate
│   └── dto/
│       ├── ApiResponse.java             # { code, error, message, data }
│       └── PageResponse.java            # envoltura serializable de Page<T>
│
├── auth/
│   ├── controller/
│   │   ├── AuthController.java          # POST /auth/register/customer, /auth/register/associate, /auth/login
│   │   └── ProfileController.java       # GET/PUT /profile/me
│   ├── service/
│   │   ├── AuthService.java
│   │   └── ProfileService.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── AssociateProfileRepository.java
│   ├── entity/
│   │   ├── User.java                    # extends Auditable
│   │   ├── Role.java                    # enum: CUSTOMER, ASSOCIATE
│   │   ├── Address.java                 # @Embeddable
│   │   └── AssociateProfile.java        # @OneToOne @MapsId hacia User
│   ├── dto/
│   │   ├── RegisterCustomerRequest.java
│   │   ├── RegisterAssociateRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java           # accessToken, tokenType, expiresIn
│   │   ├── CustomerProfileResponse.java
│   │   ├── CustomerProfileUpdateRequest.java
│   │   ├── AssociateProfileResponse.java
│   │   └── AssociateProfileUpdateRequest.java
│   └── mapper/
│       ├── UserMapper.java              # MapStruct
│       └── AssociateProfileMapper.java
│
├── catalog/                             # Público, solo lectura
│   ├── controller/
│   │   ├── CategoryController.java      # GET /categories
│   │   └── ItemController.java          # GET /items (filtros + paginación), GET /items/{slug}
│   ├── service/
│   │   ├── CategoryService.java
│   │   └── ItemQueryService.java
│   ├── repository/
│   │   ├── CategoryRepository.java
│   │   └── ItemRepository.java          # extends JpaRepository + JpaSpecificationExecutor<Item>
│   ├── entity/
│   │   ├── Category.java                # extends Auditable
│   │   ├── Item.java                    # extends Auditable
│   │   └── ItemImage.java
│   ├── dto/
│   │   ├── ItemResponse.java            # incluye category + associate_info + images (contrato del enunciado)
│   │   ├── ItemSummaryResponse.java     # para listados/paginación
│   │   ├── CategoryResponse.java
│   │   ├── AssociateInfoResponse.java   # id, store_name (subset público de AssociateProfile)
│   │   └── ItemFilterCriteria.java      # price min/max, categorySlug, sort
│   └── mapper/
│       ├── ItemMapper.java
│       └── CategoryMapper.java
│
├── associate/                           # Privado, ROLE_ASSOCIATE
│   ├── controller/
│   │   └── InventoryController.java     # POST/PUT/DELETE /associate/items
│   ├── service/
│   │   └── InventoryService.java        # valida ownership, reutiliza entidades de catalog
│   └── dto/
│       ├── ItemCreateRequest.java
│       └── ItemUpdateRequest.java
│   # (reutiliza ItemRepository, ItemMapper, entidades de catalog: sin duplicar capas)
│
└── purchase/
    ├── controller/
    │   ├── CartController.java          # GET/POST/DELETE /cart, PUT /cart/items/{id}
    │   └── OrderController.java         # POST /checkout, GET /orders, GET /orders/{id}
    ├── service/
    │   ├── CartService.java
    │   └── CheckoutService.java         # @Transactional: valida stock, crea Order, descuenta stock
    ├── repository/
    │   ├── CartRepository.java
    │   ├── CartItemRepository.java
    │   ├── OrderRepository.java
    │   └── OrderItemRepository.java
    ├── entity/
    │   ├── Cart.java                    # @OneToOne con User
    │   ├── CartItem.java
    │   ├── Order.java                   # extends Auditable
    │   ├── OrderItem.java               # snapshot de precio unitario
    │   └── OrderStatus.java             # enum: CREATED, PAID, CANCELLED
    ├── dto/
    │   ├── CartResponse.java
    │   ├── AddToCartRequest.java
    │   ├── UpdateCartItemRequest.java
    │   ├── OrderResponse.java
    │   └── OrderItemResponse.java
    └── mapper/
        ├── CartMapper.java
        └── OrderMapper.java
```

Regla de dependencia entre módulos: `associate` y `purchase` pueden importar entidades/repositorios/mappers de `catalog` y `auth` (relaciones reales del dominio: un `Item` pertenece a un `AssociateProfile`, un `Order` pertenece a un `User`). `catalog` y `auth` **no** importan nada de `associate` ni `purchase`. Sin interfaces "puerto" intermedias — import directo, es Java, no hexagonal.

---

## 2. Diseño del Modelo de Datos

### 2.1 Cómo Hibernate genera el esquema

Con `ddl-auto=update` y las anotaciones estándar de JPA, Hibernate genera automáticamente:

- **Tablas**: una por cada `@Entity` (nombre = nombre de clase en snake_case por la estrategia física por defecto de Spring Boot).
- **PKs UUID**: `@Id @GeneratedValue(strategy = GenerationType.UUID)` (soportado nativamente en Hibernate 6 / Spring Boot 3, sin necesidad de `@GenericGenerator`). Columna `uniqueidentifier` en SQL Server.
- **FKs**: cualquier `@ManyToOne` o `@OneToOne` con `@JoinColumn` genera la columna FK + la constraint `FOREIGN KEY` automáticamente.
- **Tablas intermedias**: no hay `@ManyToMany` en este diseño (se evitó a propósito: `CartItem` y `OrderItem` son entidades explícitas, no tablas intermedias implícitas, porque ambas necesitan atributos propios como `quantity` y `unitPriceAtPurchase`). Si Hibernate necesitara una, bastaría `@JoinTable`.
- **Constraints**: `@Column(nullable = false, unique = true)` sobre `email`, `slug` (en `Category` e `Item`). `@Column(precision = 12, scale = 2)` en todos los `BigDecimal` monetarios.
- **Índices**: `@Table(indexes = { @Index(columnList = "category_id") })` en `Item` para acelerar el filtro por categoría; índice sobre `slug` (ya cubierto por el `unique`).

### 2.2 Perfiles extendidos del Asociado (composición, no herencia)

```
User (auth.entity)
 ├─ id: UUID (PK)
 ├─ email: String (unique, not null)
 ├─ passwordHash: String
 ├─ role: Role (CUSTOMER | ASSOCIATE)
 ├─ firstName, lastName, phone: String
 ├─ shippingAddress: Address (@Embeddable, columnas prefijadas)
 └─ createdAt / updatedAt (heredado de Auditable)

AssociateProfile (auth.entity)
 ├─ id: UUID (PK propia, autogenerada, hereda de Auditable)
 ├─ storeName: String (not null)
 ├─ storeSlug: String (unique, not null)
 ├─ taxId: String (RFC)
 ├─ publicBio: String (@Lob opcional)
 ├─ publicContactEmail, publicContactPhone: String
 └─ user: User (@OneToOne @JoinColumn(name = "user_id", unique = true, nullable = false))
```

Nota de implementación: se descartó `@MapsId` (PK compartida) porque exige que el `@Id` no tenga `@GeneratedValue`, lo cual rompía la reutilización de `Auditable` (id autogenerado común a todas las entidades). Una FK única (`user_id`) logra la misma restricción 1-a-1 sin ese conflicto — más simple, mismo resultado.

`AssociateProfile` solo existe si `role = ASSOCIATE`. El registro de asociado (`POST /auth/register/associate`) crea ambas filas en una sola transacción de servicio. Un `CUSTOMER` nunca tiene fila en `associate_profile` — no hay columnas nulas desperdiciadas como pasaría con `SINGLE_TABLE` inheritance.

### 2.3 Catálogo híbrido (filtros dinámicos + orden fijo)

`ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item>`.

- Filtros dinámicos (precio min/max, categoría, slug parcial) → se arma un `Specification<Item>` en `ItemQueryService` combinando predicados solo para los campos presentes en `ItemFilterCriteria` (patrón builder simple, sin librería extra).
- "Más vendidos" / "más recientes" → métodos derivados con `Sort` fijo, ejecutados sobre el mismo repositorio: `findAll(spec, PageRequest.of(page, size, Sort.by("salesCount").descending()))`. El `Sort` se decide en el controlador según un parámetro `sortBy` (`recent` | `bestsellers` | `price_asc` | `price_desc`), sin crear un método de repositorio por combinación.
- `salesCount` en `Item` se incrementa dentro de `CheckoutService` al confirmar una orden (no hay tabla de analítica aparte; es un contador denormalizado, suficiente para el alcance pedido).

### 2.4 Carrito y Checkout

`Cart` es 1:1 con `User` (se crea perezosamente en el primer `POST /cart/items`). `CartItem` referencia `Item` + `quantity`. `CheckoutService.checkout(userId)`:
1. Carga el `Cart` del usuario, valida que no esté vacío.
2. Por cada `CartItem`, valida stock disponible (`InsufficientStockException` si no alcanza).
3. Crea `Order` + `OrderItem` (uno por línea, copiando `unitPriceAtPurchase = item.getPrice()` — snapshot histórico, el precio del `Item` puede cambiar después sin afectar órdenes pasadas).
4. Descuenta stock de cada `Item`, incrementa `salesCount`.
5. Vacía el `Cart`.
Todo el método anotado `@Transactional` — si cualquier paso falla, rollback completo (evita ventas con stock a medio descontar).

---

## 3. Estrategia JWT

- **Login** (`POST /auth/login`): valida credenciales con `AuthenticationManager` + `PasswordEncoder` (BCrypt), y si son válidas, `JwtService` usa un `NimbusJwtEncoder` (clave simétrica `SecretKeySpec`, HS256) para firmar un JWT con claims: `sub` (userId), `role`, `email`, `exp` (ej. 2h). Ambas clases (`JwtEncoder`/`JwtDecoder`) vienen del starter `oauth2-resource-server` ya presente en el `pom.xml`, sin librería nueva.
- **Validación**: en vez de un filtro manual, se configura `SecurityConfig` como Resource Server (`http.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`) apuntando a un `NimbusJwtDecoder` construido con la misma clave simétrica. Spring Security ya provee el filtro de extracción/validación de `Bearer` — no se reimplementa un `OncePerRequestFilter` propio (rung 5 de la escalera: dependencia ya instalada resuelve el problema).
- Un `JwtAuthenticationConverter` propio mapea el claim `role` a `GrantedAuthority` (`ROLE_CUSTOMER` / `ROLE_ASSOCIATE`) para que `hasRole(...)` funcione en `@PreAuthorize`.
- **Stateless real**: `SessionCreationPolicy.STATELESS` en `SecurityConfig`, sin `HttpSession`, sin `CSRF` (no aplica a APIs sin cookies).
- **Autorización**: `@PreAuthorize("hasRole('ASSOCIATE')")` a nivel de método en `InventoryController`/`InventoryService`. Verificación de ownership (multi-tenancy) explícita dentro del servicio, no en la anotación, por legibilidad y trazabilidad del error (`ForbiddenOperationException` con mensaje claro en vez de un 403 genérico de Spring Security).
- **Sin refresh token** en el alcance actual (no lo pide el enunciado): expiración corta + login de nuevo. Se deja como mejora futura, no se construye especulativamente ahora.

---

## 4. Roadmap de Ejecución

**Fase 1 — Configuración Base**
- `common.entity.Auditable` (`@MappedSuperclass`, id UUID, `@CreatedDate`, `@LastModifiedDate`).
- `common.config.JpaAuditingConfig` (`@EnableJpaAuditing`).
- `common.dto.ApiResponse`, `common.dto.PageResponse`.
- `common.exception.*` + `GlobalExceptionHandler` con `ProblemDetail` (404, 409 duplicado, 400 validación, 403 forbidden, 422 stock insuficiente).

**Fase 2 — Módulo Auth**
- Entidades `User`, `Role`, `Address`, `AssociateProfile`.
- `SecurityConfig` (resource server con `JwtDecoder` + `JwtAuthenticationConverter`, `PasswordEncoder`, reglas de rutas públicas vs protegidas).
- `JwtService` (encoder), `SecurityUser`, `CustomUserDetailsService`.
- `AuthController` (registro customer/associate, login), `ProfileController` (ver/editar perfil propio según rol).

**Fase 3 — Módulo Catálogo**
- Entidades `Category`, `Item`, `ItemImage`.
- `ItemRepository` con `JpaSpecificationExecutor`, `CategoryRepository`.
- `ItemQueryService` (Specification dinámica + Sort por parámetro).
- `ItemController`/`CategoryController` públicos, DTOs y mappers según el contrato del enunciado (`ItemResponse` con `category`, `associate_info`, `images`).

**Fase 4 — Módulo Asociados**
- `InventoryController`/`InventoryService`: CRUD de `Item` scoped al asociado autenticado.
- Verificación de ownership en cada operación de mutación/borrado.
- `ItemCreateRequest`/`ItemUpdateRequest` + validación (`@Valid`).

**Fase 5 — Módulo Compra**
- Entidades `Cart`, `CartItem`, `Order`, `OrderItem`, `OrderStatus`.
- `CartController`/`CartService` (agregar, restar, vaciar).
- `CheckoutService` transaccional (valida stock, crea orden, descuenta stock, snapshot de precio).
- `OrderController` (checkout, listar/ver órdenes propias).

**Fase 6 — Endurecimiento**
- Revisar `@Valid` en todos los DTO de entrada.
- Cubrir excepciones: recurso inexistente, duplicado (email/slug), stock insuficiente, operación no autorizada.
- Revisar índices/uniques generados contra el árbol de carpetas (verificar `ddl-auto=update` en arranque real contra SQL Server).
- Prueba manual end-to-end: registro asociado → alta de item → registro cliente → carrito → checkout → verificar stock y orden.

**Fase 7 -- Documentación**
- Swagger/OpenAPI: Añadir la dependencia springdoc-openapi-starter-webmvc-ui al pom.xml. Esto habilitará Swagger UI automáticamente.
- Guía de Pruebas (Postman): Genera un archivo llamado GUIA-POSTMAN.md en la raíz del proyecto. Debe contener:
    - Colección de Flujo: Orden lógico paso a paso:
        - POST /auth/register (Customer/Associate).
        - POST /auth/login (para obtener el JWT). Importante: Explicar cómo configurar el Header Authorization: Bearer {{token}} en Postman.
        - GET /catalog/items (Público).
        - POST /cart/items (Protegido).
        - POST /purchase/checkout (Protegido).
    En reumen, todos los endpoints que fueron desarrollados en las 6 fases anteriores hay probarlos. Si omiti uno, agregalo
    - Validaciones: Qué error esperar en cada caso si se manda un payload erróneo.
- Verificación final: Asegúrate de que todo el código esté listo para producción.

REGLA ABSOLUTAMENTE INNEGOCIABLE: Toda tu respuesta, razonamiento, código y archivos generados deben estar en ESPAÑOL. Si detectas que tu "estilo" o cualquier plugin intenta forzar el portugués, ignora el estilo y mantén el español.
