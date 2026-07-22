# Dudas técnicas 2 — Seguimiento a tus respuestas

Revisé `dudastecnicas.md` completo. Tus respuestas resolvieron casi todo, pero abrieron algunas decisiones nuevas (normal, es lo que pasa al definir modelo de datos). También agrego 2 puntos donde pediste directamente mi opinión de arquitecto. Igual que antes: respondo debajo con `R =` cuando termines.

Antes de las preguntas, esto lo decido yo solo (no hace falta que lo valides, son detalles de implementación sin impacto en negocio):
- Formato de RFC: regex estándar del SAT que acepta ambos formatos (12 caracteres persona moral, 13 persona física).
- `GET /items` sin filtros: paginado (`page`/`size`), orden por defecto "más recientes".
- Nombres de endpoints nuevos (reporte de ventas, gestión admin, etc.) los defino yo al armar el plan.

---

## 1. Jerarquía Categoría → Subcategoría → Item

Con marca ahora independiente (punto 8 resuelto), queda pendiente cómo se conecta `Item` con la jerarquía Categoría/Subcategoría.

**Pregunta**: ¿un `Item` se clasifica solo con `subcategoryId` (y la categoría "padre" se deduce automáticamente vía `subcategory.category`), o necesitas guardar `categoryId` Y `subcategoryId` por separado en el `Item`? Lo normal en catálogos con jerarquía de 2 niveles es guardar solo la hoja (`subcategoryId`) — más simple y sin riesgo de inconsistencia (que un item tenga categoría A pero subcategoría de la categoría B). Recomiendo eso.

R = Que se clasifiquen por subcategoryId pero que el servicio de las categorias tenga la capacidad de traer la categoria padre por subcategoryId

---

## 2. Relación Item ↔ Marca

Ya confirmaste que la marca es independiente/global. Falta la cardinalidad:

**Pregunta**: ¿un `Item` tiene **una sola marca** (`ManyToOne`, lo normal — un producto/servicio es "de" una marca), o puede tener **varias marcas a la vez** (`ManyToMany`)? Recomiendo una sola marca por item.

Y con esto, el requisito original de Paso 3 ("cada subcategoría debe tener al menos 2 marcas") ya no es una relación real en la base de datos — pasa a ser solo una instrucción para la variedad de datos de prueba que cargue por Postman (repartir marcas entre subcategorías al momento de crear los productos de ejemplo, no un vínculo Marca↔Subcategoría). ¿Correcto?

R = Un item puede tener una sola marca y para un servicio es opcional. Para la poblacion de datos en postman, entonces solo me daras la peticion para llenar las categorias, las marcas se iran llenando conforme los usuarios llenen los productos pero, eso abre el escenario en que debemos crear otra tabla para las marcas, y eso a su vez abre que tengamos que crear un ednpoint para traer todas las marcas, para buscar una marca por nombre y otro para buscar por id. Confirmame esto por favor

---

## 3. Filtro de catálogo: ¿por ID o por nombre?

Preguntaste si el filtro de categoría puede ir por `categoryName` en vez de `categoryId`. Mi recomendación como tech lead: **no**, por estas razones:
- Nombres se pueden repetir o cambiar (si renombras "Electrónica" a "Electrónicos", cualquier bookmark/link roto usando el nombre deja de filtrar).
- Comparar texto (mayúsculas, acentos, espacios) es más frágil y lento que comparar un UUID indexado.
- El frontend igual necesita mostrarte el `name` para el usuario — pero internamente, cuando arma el request, usa el `id` que ya recibió en `GET /categories`. Es exactamente como ya funciona `categoryId` en `ItemCreateRequest` hoy.

Propongo mantener `subcategoryId` (ver punto 1) como filtro, y el frontend muestra `subcategory.name` en pantalla. ¿De acuerdo, o hay una razón puntual por la que necesitas que sea por nombre?

R = De acuerdo

---

## 4. Campos nuevos para servicios

Confirmaste: duración estimada, modalidad, zona de cobertura, presupuesto "desde $999.00". Necesito precisar el tipo de dato de cada uno:

- **Duración estimada**: ¿texto libre ("2-3 horas", "1 semana") o un número + unidad estructurada (ej. `durationValue: 2`, `durationUnit: HORAS/DIAS/SEMANAS`)? Texto libre es más simple pero no se puede ordenar/filtrar por duración después.
- **Modalidad**: ¿enum cerrado `PRESENCIAL` / `REMOTO` / `AMBOS`, o texto libre?
- **Zona de cobertura**: ¿texto libre (ej. "Ciudad de México y área metropolitana"), o algo estructurado (lista de ciudades/estados)? Texto libre es lo razonable para un primer alcance.
- **Presupuesto "desde $999.00"**: ¿es un campo nuevo separado (`estimatedFrom: BigDecimal`) que se suma al `price` que ya existe, o reemplaza la semántica de `price` para servicios (es decir, en un servicio el `price` ya existente se interpreta como "precio desde" y el frontend le antepone el texto "Desde $")? Recomiendo lo segundo — reutilizar `price` con una interpretación distinta según `type`, para no duplicar el campo.

R1 = para duracion estimada sera un numero mas unidad estructurada. 
R2 = modalidad un enum
R3 = zona de cobertura texto libre
R4 = presupuesto reemplaza price para servicios y el front antepone eltexto desde
---

## 5. SKU: ¿único por asociado o único en todo el sistema?

Dijiste "único por producto" — necesito el alcance exacto: si dos asociados distintos usan el mismo SKU (cosa común, cada tienda maneja su propio sistema de códigos internos), ¿debe rechazarse, o el SKU solo debe ser único **dentro del catálogo del mismo asociado**? Lo típico en marketplaces multi-vendedor es unicidad por asociado, no global.

R = unico por asociado

---

## 6. Rol ADMIN — propuesta de alcance

Pediste ayuda para definir esto. Así es como normalmente se maneja un rol admin en un e-commerce/marketplace (y lo adapto a lo que ya tenemos):

**Lo que SÍ haría el ADMIN:**
- CRUD completo de Categorías, Subcategorías y Marcas (Paso 3).
- Ver listado de todos los usuarios registrados (clientes y asociados) con datos no sensibles: nombre, email, rol, fecha de registro, estado activo/inactivo — **sin** ver contraseñas (obviamente ya están hasheadas) ni datos de pago si algún día existen.
- Activar/desactivar manualmente cualquier usuario o item (el borrado lógico del Paso 5), por ejemplo ante una denuncia o fraude.
- Ver el catálogo completo de items de cualquier asociado (solo lectura) — para moderación, no para editar el contenido de otro asociado.

**Lo que NO haría el ADMIN** (para no pisar la autonomía del asociado, como pediste):
- No puede crear, editar ni eliminar items de un asociado — eso es exclusivo de `INVENTORY` (`@PreAuthorize("hasRole('ASSOCIATE')")`) sobre su propio inventario.
- No participa en el flujo de compra/checkout de otros usuarios ni ve el carrito ajeno.
- No gestiona el estado de las órdenes de un asociado (eso es del asociado dueño de la orden, según el Paso 6).

**Cómo se autentica**: mismo mecanismo JWT que ya existe, agregando `ADMIN` al enum `Role`. **No** se auto-registra por `/auth/register/*` como los otros roles — se crea manualmente (seed inicial en base de datos, o un endpoint `POST /auth/register/admin` protegido que solo otro ADMIN pueda invocar). Recomiendo la segunda opción para no depender de tocar la base de datos a mano cada vez.

¿Te parece bien este alcance, agregamos/quitamos algo?

R = Si, me parece bien

---

## 7. Reactivación en cascada de un asociado

Ya confirmaste que al desactivar un asociado, sus items pasan a inactivos en cascada automáticamente. Falta el caso inverso:

**Pregunta**: cuando el asociado se **reactiva** (vuelve a hacer login o un admin lo reactiva), ¿sus items **vuelven a activarse automáticamente** también, o quedan inactivos y el asociado debe reactivarlos uno por uno manualmente desde su inventario? Recomiendo la segunda opción (reactivación manual de items) — evita que productos desactualizados (precio viejo, stock viejo) vuelvan a aparecer en el catálogo sin que el asociado los revise primero.

R = Lo segundo, deberan reactivarlos de manera manual

---

## 8. Direcciones de asociados — confirmar mi interpretación

De tu respuesta entiendo esto, confírmame si es correcto:
- `AssociateProfile` gana un campo nuevo **único** `storeAddress` (la dirección física de la tienda, no es una lista).
- Además, tanto `CUSTOMER` como `ASSOCIATE` comparten la misma entidad nueva `UserAddress` (lista, con `isDefault`) para cuando actúan como **compradores** — ya que hoy nada impide que un `ASSOCIATE` también compre en el marketplace (`CartController`/`OrderController` solo piden "autenticado", no un rol específico).

¿Es correcto este entendimiento?

R = Si, es correcto

---

## 9. Carrito: no mezclar productos y servicios — ¿lo valida el backend?

Preguntaste si hace falta considerar algo en el backend. Sí: **nunca hay que confiar solo en el frontend** para una regla de negocio así. Propongo:

- En `CartService.addItem()`: si el carrito ya tiene items de un `type`, y se intenta agregar un item del `type` contrario, se rechaza con un error claro (ej. `409` con mensaje "No se pueden combinar productos y servicios en el mismo carrito").
- Esto también simplifica el checkout: como cada carrito es de un solo tipo, no hay ambigüedad al dividir en sub-órdenes por asociado (punto 10).

¿Confirmas que quieres esta validación en el backend (no solo en el frontend)?

R = Si, me gusta esa validacion

---

## 10. Estructura de sub-órdenes por asociado (arquitectura del checkout)

Ya elegiste la opción (a): dividir en sub-órdenes por asociado. Falta definir la forma exacta:

**Pregunta**: ¿prefieres...
- a) Una `Order` "padre" (agrupador, sin `status` propio relevante — solo referencia el total general y la fecha) + varias `Order` "hijas" (una por asociado involucrado, cada una con su propio `status` y su lista de `OrderItem`), relacionadas con un `parentOrderId`.
- b) Sin entidad padre: el checkout simplemente crea **N órdenes independientes** (una por asociado), sin ningún registro que las agrupe como "una sola compra". El frontend, si quiere mostrar "tu pedido de hoy", simplemente agrupa por fecha/hora de creación.

La opción (a) es más robusta (por ejemplo, para mostrarle al cliente un solo número de confirmación de compra con el desglose por tienda), pero es una tabla y relación más. La (b) es más simple pero pierde la noción de "una compra = un evento", solo quedan órdenes sueltas. ¿Cuál prefieres?

R = Prefiero la opcion a, quiero mantenerla robusta

---

## 11. Reporte de ventas del asociado — nivel de detalle

Dijiste que necesitas ya filtros por mes y rango de fechas. Para dimensionar el endpoint:

**Pregunta**: ¿el reporte es solo un total agregado (cantidad de órdenes completadas + monto total en el rango), o necesitas también el desglose por producto/servicio (ej. "vendiste 5 de este producto y 3 de este servicio en el rango X")? Lo primero es un solo query de agregación; lo segundo es un group-by adicional. Ambos son viables, solo necesito saber el alcance para el diseño del endpoint y su DTO de respuesta.

R = Lo que quiero con ese reporte es un desglose como van sus ventas en general. Cuanto ha ganado, cual es su producto mas vendido, el menos vendido, el stock total de sus productos. Si quieres mantenerlo simple esta bien, pero quiero que venga desglosado en un objeto json para el front al final del proceso de crearlo

---

Cuando termines, avísame y armo `planificaciondemejoras.md` con el plan completo (incluye Paso 8: actualización de Postman/Swagger, y Paso 9: `actualizacion-backend.md` para el equipo frontend, ambos ya anotados en tu nota).
