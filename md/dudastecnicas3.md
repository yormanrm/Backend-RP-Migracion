# Dudas técnicas 3 — Últimos puntos antes de armar el plan

Revisé `dudastecnicas2.md` completo. Casi todo cerrado — quedan 5 puntos, uno de ellos importante porque hay una contradicción entre dos respuestas tuyas que necesito que resuelvas.

Decido yo solo (sin impacto de negocio, no hace falta que respondas):
- Unidad de duración de servicios: enum `HORAS` / `DIAS` / `SEMANAS`.
- Valores de modalidad: enum `PRESENCIAL` / `REMOTO` / `AMBOS`.
- SKU único por asociado: constraint compuesto (`associate_id` + `sku`), no columna única global.

---

## 1. Contradicción a resolver: ¿quién crea las marcas?

En el punto 6 de `dudastecnicas2.md` aprobaste que el rol **ADMIN** haga el CRUD completo de Categorías, Subcategorías **y Marcas**. Pero en el punto 2 dijiste que "las marcas se irán llenando conforme los usuarios llenen los productos" — eso suena a que el **ASOCIADO** puede crear una marca nueva directamente al registrar un producto, sin pasar por el ADMIN.

Son dos modelos distintos y necesito que elijas uno:

- **a) Catálogo curado por ADMIN**: el asociado solo puede elegir una marca de la lista ya existente (`brandId` obligatorio, viene de un select poblado con `GET /brands`). Si la marca que necesita no existe, tiene que pedirle al ADMIN que la cree primero. Más control, pero más fricción para el asociado.
- **b) Autoservicio con auditoría**: el asociado escribe el nombre de la marca al crear su producto; el backend busca si ya existe (comparación case-insensitive) y si no, la crea automáticamente ("buscar o crear"). El ADMIN conserva la capacidad de editar/fusionar/desactivar marcas después, para limpiar duplicados o nombres mal escritos. Cero fricción, pero requiere que el ADMIN haga mantenimiento de vez en cuando.

Por lo que describiste en el punto 2, me inclino a pensar que quieres la **(b)**. ¿Confirmas, o prefieres la (a)?

R = Si, justo espero lo de la opcion B

---

## 2. Si es autoservicio (b): ¿el request de creación de producto manda `brandId` o `brandName`?

Si eliges la opción (b) del punto anterior: `ItemCreateRequest`/`ItemUpdateRequest` recibiría un campo `brandName` (texto libre, opcional ya que confirmaste que en servicios la marca es opcional) en vez de `brandId`, y el backend resuelve internamente la marca (crea si no existe, reutiliza si ya existe). El `ItemResponse` seguiría devolviendo el objeto `Brand` completo (`id`, `name`) para que el frontend lo pueda usar como filtro después. ¿Así está bien?

R = Si, asi esta bien

---

## 3. Endpoints de marca — confirmar los 3 que mencionaste

Confirmo que voy a crear:
- `GET /brands` — listado completo (público, para poblar selects/autocompletado).
- `GET /brands/{id}` — detalle por id.
- Búsqueda por nombre: ¿la resuelvo como parámetro opcional en el mismo `GET /brands?q=nombre` (mismo estilo que el buscador de productos del Paso 4), o prefieres un endpoint separado `GET /brands/search?name=`? Recomiendo lo primero, es más consistente con el resto de la API.

R = Para los 2 primeros esta bien y para el de busqueda por nombre esta bien por parametro opcional

---

## 4. Estado agregado de la orden "padre"

Con la estructura de orden padre + sub-órdenes por asociado (ya confirmada), puede pasar que un cliente tenga, por ejemplo, la sub-orden de la Tienda A en `COMPLETED` y la de la Tienda B todavía en `PROCESSING`. Para que el cliente entienda de un vistazo el estado general de su compra completa, propongo que la orden padre calcule (sin guardar un campo redundante en base de datos, solo al momento de responder el JSON) un estado agregado tipo `PARCIALMENTE_COMPLETADA` / `COMPLETADA` / `EN_PROCESO` / `CANCELADA`, derivado de las sub-órdenes hijas. ¿Te sirve esto, o prefieres que el frontend arme esa vista combinando los estados de las sub-órdenes por su cuenta (sin que el backend calcule nada extra)?

R = Prefiero que lo maneje el backend. Devolver ese nuevo estado de Parcialmente completada calculando desde aqui esta bien

---

## 5. Alcance exacto del reporte de ventas del asociado

Pediste: cuánto ha ganado, producto más vendido, menos vendido, y stock total — todo desglosado en un JSON, con filtro de fecha. Para armar bien el DTO de respuesta necesito 3 precisiones:

- **Producto más/menos vendido**: ¿se calculan considerando solo las ventas **dentro del rango de fechas** filtrado, o son cifras **históricas de siempre** (usando el `salesCount` acumulado del item) sin importar qué rango se haya seleccionado?
- **Stock total de sus productos**: entiendo que esto es inventario **actual en tiempo real** (suma de `stock` de todos sus items activos), independiente del rango de fechas — ¿correcto?
- **Producto "menos vendido"**: si un asociado tiene productos con 0 ventas, ¿esos cuentan como "el menos vendido" (empatan en 0), o el ranking de "menos vendido" solo debe considerar productos que ya tuvieron al menos una venta?
- Si el asociado no manda ningún rango de fechas al pedir el reporte, ¿el default es "todo el histórico" o un período fijo (ej. últimos 30 días)?

R1 = Son cifras historicas de siempre pero tambien pueden generarlas por rango
R2 = si, es correcto
R3 = Exacto, el default es todo el historico

---

Cuando respondas estos 5 puntos armo `planificaciondemejoras.md` con el plan completo y ordenado, incluyendo los Pasos 8 y 9.
