# Dudas técnicas — Reestructuración de API

Preguntas que necesito resolver antes de armar `planificaciondemejoras.md`. Están agrupadas por paso del documento `sig-paso.md`. Responde inline (debajo de cada pregunta) o como prefieras; en cuanto estén resueltas armo el plan completo.

---

## Preparación — Purga de base de datos

1. `ddl-auto: update` está activo, así que el esquema no se borra solo. ¿Purga = un script SQL (`DELETE`/`TRUNCATE` de todas las tablas) que yo genere y me confirmas que corres tú manualmente, o prefieres que sea un endpoint temporal? (Recomiendo script SQL manual — no dejar un endpoint de "borrar todo" ni siquiera temporal, es un riesgo de seguridad si se olvida quitar).
R = Script manual en un md para que yo puedo ejecutarlo en la base
---

## Paso 1 — Autenticación

### 1.1 Asociados

2. **RFC**: ¿solo renombrar `taxId` → `rfc` (String libre), o también validar formato de RFC mexicano (12 caracteres persona moral / 13 persona física, patrón alfanumérico)? Hoy `taxId` es opcional y sin unicidad — pediste que no se repita, así que además de renombrar hay que agregar `unique = true` y el chequeo de duplicado en `AuthService`. ¿El RFC pasa a ser obligatorio o se mantiene opcional (y solo se valida unicidad cuando viene informado)?
R1 = Se debe validar el formato para que coincida con el formato mexicano.
R2 =  Debe ser obligatorio y unique para el que sea unico e irrepetible
3. **storeSlug autogenerado**: se generará desde `storeName` (ej. "Ferretería El Tornillo" → `ferreteria-el-tornillo`). Si ya existe ese slug, ¿qué estrategia de colisión prefieres?
   - a) Sufijo numérico incremental (`ferreteria-el-tornillo-2`)
   - b) Sufijo aleatorio corto (`ferreteria-el-tornillo-x7k2`)
R = Opcion b, sufijo aleatorio corto
### 1.2 Registro general

4. Duplicado de email ya está cubierto (`DuplicateResourceException`). Con el RFC único agrego la misma validación para asociados. ¿Confirmas que no hace falta nada adicional aquí, o hay otro caso de duplicado que tengas en mente?
R = Son los unicos casos que se me ocurren de momento
---

## Paso 2 — Inventario: productos y servicios

Este es el cambio más grande de todo el plan y necesito que decidamos el modelo de datos junto contigo.

5. **Modelo para servicios intangibles**: hoy `Item` tiene `stock` (obligatorio) pensado solo para productos físicos. Para soportar servicios (plomería, carpintería, etc.) veo dos caminos:
   - a) **Un solo `Item`** con un campo `type` (`PRODUCT` / `SERVICE`). `stock` se vuelve opcional (null para servicios). Más simple, reutiliza todo el catálogo/búsqueda/filtrado tal cual.
   - b) **Dos entidades separadas** (`Product` y `Service`) con tabla propia. Más "correcto" a largo plazo si productos y servicios terminan necesitando campos muy distintos, pero duplica controllers, DTOs, repositorios y toda la lógica de búsqueda/filtrado del Paso 4.

   Mi recomendación es **(a)**: un `Item` con `type` y campos opcionales según el tipo. ¿Estás de acuerdo, o ya tienes en mente campos tan distintos entre producto y servicio (ej. duración, disponibilidad por horario) que amerite separarlos?
R = Opcion a, un solo item
6. Si es la opción (a): ¿qué campos adicionales necesita un servicio que un producto no tiene? (ej. duración estimada, modalidad presencial/remoto, zona de cobertura). Si no hay ninguno todavía, lo dejamos solo con `type` y ampliamos después.
R = Los campos adicionales que mencionaste como duración estimada, modalidad presencial/remoto, zona de cobertura mas aparte presupuesto estimado
(ej. Desde $999.00)
7. **Slug de items**: mismo caso que el punto 3 (store), autogenerado desde `title`. ¿Misma estrategia de colisión que elijas para storeSlug?
R = misma respuesta que para el punto 3
---

## Paso 3 — Categorías, subcategorías y marcas

8. **Jerarquía Categoría → Subcategoría → Marca**: tal como lo describes ("cada subcategoría debe tener al menos 2 marcas"), la Marca queda **ligada a la subcategoría**, no independiente. Esto es distinto al modelo típico de e-commerce (donde una marca como "Samsung" vive sola y se relaciona con productos de cualquier categoría). Con marca-por-subcategoría, si "Samsung" vende celulares y electrodomésticos, tendrías que crear "Samsung" dos veces (una por subcategoría). ¿Es realmente lo que quieres, o prefieres marcas independientes/globales que luego se asocian a uno o varios productos (relación muchos-a-muchos, sin importar la subcategoría)?
R = Prefiero que la marca sea independiente/global como dices, gracias por esa observacion
9. **¿Quién puede crear/editar/eliminar categorías, subcategorías y marcas?** Hoy el sistema **no tiene rol ADMIN** (solo `CUSTOMER` y `ASSOCIATE`). Necesito que definas una de estas opciones:
   - a) Crear un nuevo rol `ADMIN` con su propio flujo de autenticación/gestión.
   - b) Permitir que cualquier `ASSOCIATE` gestione el catálogo global de categorías (riesgo: un asociado podría borrar/editar categorías usadas por otros asociados).
   - c) Otra idea que tengas.

   Recomiendo (a): es el enfoque correcto para un catálogo compartido entre todos los asociados.
R = Opcion a, pero eso implica que debamos discutir que alcance tendra el rol ADMIN. Yo me imagino que ademas del control de las categorias, puede
ver datos no sensibles de todos los usuarios registrados, un manejo de productos quiza pero sin afectar lo que hagan los asociados. Necesito
tu ayuda para definir bien esta parte. Como se maneja un rol de admin en un sitio web ya se de ecommerce como o en general en cualquier sitio web?
10. Confirmo que quitar el `slug` de categoría implica que el filtro de items pasa de `categorySlug` a `categoryId` (o el nuevo `subcategoryId`). ¿De acuerdo?
R = No puede ser por el category name?
11. El pedido de la carga por Postman (10 categorías × 3 subcategorías × 2 marcas) lo genero en cuanto definamos el modelo de datos de los puntos 8–10, ya que la estructura del JSON depende de esas respuestas.
R = De acuerdo
---

## Paso 4 — Catálogo de productos y servicios (búsqueda estilo Amazon/Mercado Libre)

12. **Campos nuevos en `Item`**: para buscar por "modelo" o "sku" necesito agregarlos como columnas nuevas (`model`, `sku`) — hoy no existen. ¿`sku` debe ser único por asociado o único a nivel global de todo el sistema?
R= sku debe ser unico por producto hasta donde yo. Otra columna que se agregaria seria marca como discutimos antes
13. **Búsqueda "amigable"**: ¿un solo campo de texto libre (`q`) que se compara (LIKE, case-insensitive) contra `title`, `slug`, `model`, `sku` y nombre de tienda a la vez — como una barra de búsqueda real —, o parámetros separados por campo (`title=`, `sku=`, `store=`, etc., todos opcionales y combinables)? Por el ejemplo que diste ("si escriben el nombre de la tienda que aparezcan productos de esa tienda") entiendo que es **un solo campo `q`** tipo buscador universal. ¿Confirmas?
R = Si, confirmo. a la lista de contra que se comparara q agrega marca
14. **Endpoint POST con JSON body**: reemplazo el actual `GET /items` (con query params) por `POST /items/search` con un body JSON que incluya `q`, `categoryId`/`subcategoryId`, `priceMin`, `priceMax`, `sortBy`, `type` (producto/servicio), `page`, `size`. ¿Elimino el `GET /items` actual por completo, o lo dejas como alias simple (sin filtros) para casos donde no se necesite búsqueda?
R = que se quede el Get/ITEMS para traer todos los productos sin fultros ni busqueda
15. **Orden "más vendidos"**: ya existe `salesCount` en `Item` y se usa en checkout — para servicios, ¿"más vendido" tiene sentido igual (contar contrataciones) o prefieres otra métrica para servicios?
R = esta bien usar el mismo salesCount para servicios, lo interpretaremos como contador de contrataciones
---

## Paso 5 — Borrado lógico

16. Confirmo el alcance exacto: bandera `active` (boolean) en `User` (asociados y clientes), `Item` y `Category` (y por extensión `Subcategoria`/`Marca` si aplican del Paso 3). **No** incluye `Order`, `Cart` ni `CartItem` — ¿correcto, o alguno de estos también debería tener el flag?
R = es correcto
17. Un `Item` que se marca inactivo (asociado lo "eliminó"): ¿debe seguir siendo visible en el historial de órdenes pasadas que lo referencian (obviamente sí, ya que es solo un FK), pero debe **desaparecer** del catálogo público (`GET/POST /items`) y de los resultados de búsqueda? Asumo que sí, solo confirmo.
R = Si, confirmo lo que dices
18. Un `User` (cliente o asociado) inactivo: ¿debe poder seguir haciendo login (para reactivar su cuenta o ver historial), o el login debe **rechazarse** directamente si `active = false`? Y si es un asociado inactivo, ¿sus items pasan a inactivos en cascada automáticamente, o quedan visibles aunque el asociado esté desactivado?
R = Si, debemos mantener su login para que puedan reactivarla y ver su historial e inventario en el caso de un asociado. Para los asociados, si, si se
desactiva la cuenta los items relacionados a el pasan a inactivos en cascada automaticamente
---

## Paso 6 — Ideas abiertas

### Direcciones múltiples

19. Hoy `Address` vive **embebido y único** dentro de `User` (`shippingAddress`), no como lista. Para soportar varias direcciones con una marcada como predeterminada, necesito convertirla en una **entidad propia** (`UserAddress`) con relación uno-a-muchos hacia `User` y el flag `isDefault`. ¿Confirmas ese cambio?
R = Confirmo
20. ¿Los **asociados** también necesitan múltiples direcciones (para envíos de devolución, por ejemplo), o las direcciones múltiples son solo para `CUSTOMER` y el asociado mantiene una sola dirección de tienda (o ninguna, ya que hoy `AssociateProfile` ni siquiera tiene campo de dirección)?
R = ambos deben pueden tener varias direcciones, solo que para asociados habra dos tipo. Una unica direccion que sera la de su tienda y un arreglo
de varias direcciones donde pueden recibir los pedidos que hagan
### Órdenes visibles en perfil

21. El endpoint `GET /orders` (con orden por fecha) ya existe y devuelve las órdenes del usuario autenticado. Cuando dices "al visualizar el perfil de un cliente o asociado deben poder ver sus órdenes", ¿te refieres al propio usuario viendo **su propio perfil** (ya cubierto, solo faltaría agregar el parámetro de orden por fecha), o a un tercero/admin viendo el perfil **de otro** usuario y sus órdenes (eso sería una funcionalidad nueva y expondría datos de compra de otro usuario — necesitaría restricción de permisos clara)?
R = me refiero a lo primero, al usuario viendo su propio perfil pero ese es otro de los puntos con el perfil admin y que podemos resolver en otro
archivo de dudas
### Gestión de órdenes por asociados (el punto más importante de esta sección)

22. **Este es el cambio de arquitectura más grande de la sección 6.** Hoy una `Order` pertenece a **un solo `User`** (el comprador) y tiene **un único `status`** para toda la orden. Pero el carrito puede tener items de **varios asociados distintos** a la vez. Si un cliente compra un producto de la Tienda A y un servicio de la Tienda B en la misma orden, ¿cómo esperas que cada asociado gestione "sus" pedidos?
    - a) **Dividir la orden en sub-órdenes por asociado** al hacer checkout (una `Order` "padre" de la compra + una `Order` hija por cada asociado involucrado, cada una con su propio `status`). Es el patrón estándar en marketplaces multi-vendedor (Amazon, Mercado Libre lo hacen así).
    - b) **Mantener una sola `Order`** pero mover el `status` a nivel de `OrderItem` (cada línea del pedido tiene su propio estado, y el asociado solo ve/edita las líneas que le pertenecen).

    Recomiendo (a) porque es más limpio para reportes de ventas y notificaciones, pero (b) es un cambio más chico. ¿Cuál prefieres?
R= Opcion a. Como lo lo dijiste y te lo dije, quiero algo parecido a lo que hacen esas tiendas. Otra cosa que tal vez deberemos considerar, en un carrito no puede haber servicios y productos juntos. Aunque este cambio es mas del frontend, no mezclaremos en un carrrito la compra de un produdcto
y un servicio. Debemos considerar algo en el backend?

23. **Estados de orden nuevos**: hoy `OrderStatus` solo tiene `CREATED`, `PAID`, `CANCELLED`. Para que un asociado gestione "por procesar / completadas / canceladas" necesito agregar estados intermedios. Propongo: `PAID → PROCESSING → COMPLETED`, más `CANCELLED` en cualquier punto antes de `COMPLETED`. ¿Te sirve este flujo, o manejas otros nombres/pasos (ej. `SHIPPED` para productos físicos)?
R = Me sirven esos

24. **Reporte de ventas del asociado** ("cuántas ventas han hecho"): ¿con un conteo/suma simple (total de órdenes completadas y monto acumulado) alcanza por ahora, o ya quieres filtros de fecha (por mes, por rango) desde esta primera versión?
R = Necesito ya implementar por mes, rango de fechas etc.
---

NOTA IMPORTANTE ANTES DE EMPEZAR `planificaciondemejoras.md`: Se trataron todas las dudas expuestas en este documento pero puede
que hayan salido mas. Para estar seguros de que no hay mas dudas, lee con atencion este md y si hay mas dudas exponlas en otro
archivo md llamda dudastecnicas2.md. Adicional a esto, en el arhcivo `sig-paso.md` olvide 2 pasos mas que son:
Paso 8. Actualizacion de postman y SwaggUI
   Una vez tertminada la implementacion de los 7 ppasos anteriores, deberas actualizar la coleccion postman que creamos asi como la documentacion
   del SwaggUI del api
Paso 9. Informar al front de los cambios sucitados
   Una vez terminados los 8 pasos anterior, genera un nuevo archivo llamado actualizacion-backend.md, el cual debera llenar un plan de implementacion
   de todos los cambios que realizamos en los pasos anteriores. Debe ser un plan muy detallado, compartiendo como deberian quedar las interfaces para angular, responses, request, todos los endpoints de la api, etc etc etc. Insisto en que debe ser muy detallado y estructurado ya que sera compartido
   al equipo frontend y puedan hacer todas las modificaciones requeridas