# Seed de catálogo — 10 categorías × 3 subcategorías

> Ejecutar autenticado como **ADMIN** (login con `app.admin.email` / `app.admin.password`; el token queda en `{{token}}` con el script del request de login de la colección). Las **marcas no se siembran**: se crean solas por autoservicio cuando los asociados registran productos con `brandName`.

## Flujo

1. `POST {{base_url}}/auth/login` con las credenciales del ADMIN.
2. Por cada categoría de la tabla: `POST {{base_url}}/categories` con body `{ "name": "<categoría>" }`. Guardar el `data.id` de la respuesta.
3. Por cada subcategoría: `POST {{base_url}}/subcategories` con body `{ "name": "<subcategoría>", "categoryId": "<id de su categoría>" }`.
4. Verificar con `GET {{base_url}}/categories`: deben venir 10 categorías con 3 subcategorías anidadas cada una.

## Datos

| # | Categoría | Subcategorías |
|---|-----------|---------------|
| 1 | Electrónica | Celulares y Smartphones · Computadoras y Laptops · Audio y Audífonos |
| 2 | Hogar y Cocina | Muebles · Electrodomésticos · Decoración |
| 3 | Moda | Ropa de Mujer · Ropa de Hombre · Calzado |
| 4 | Deportes y Fitness | Ejercicio y Gimnasio · Ciclismo · Camping y Aire Libre |
| 5 | Juguetes y Bebés | Juguetes · Artículos para Bebé · Juegos de Mesa |
| 6 | Herramientas y Construcción | Herramientas Eléctricas · Herramientas Manuales · Material Eléctrico |
| 7 | Belleza y Cuidado Personal | Maquillaje · Cuidado de la Piel · Cuidado del Cabello |
| 8 | Automotriz | Refacciones · Accesorios para Auto · Llantas y Rines |
| 9 | Libros y Papelería | Libros · Papelería y Oficina · Arte y Manualidades |
| 10 | Mascotas | Alimento para Mascotas · Accesorios · Higiene y Salud |

## Bodies listos para copiar

### Categorías (`POST /categories`)

```json
{ "name": "Electrónica" }
{ "name": "Hogar y Cocina" }
{ "name": "Moda" }
{ "name": "Deportes y Fitness" }
{ "name": "Juguetes y Bebés" }
{ "name": "Herramientas y Construcción" }
{ "name": "Belleza y Cuidado Personal" }
{ "name": "Automotriz" }
{ "name": "Libros y Papelería" }
{ "name": "Mascotas" }
```

### Subcategorías (`POST /subcategories`)

Reemplazar `CAT_ID_N` por el id devuelto al crear la categoría correspondiente:

```json
{ "name": "Celulares y Smartphones", "categoryId": "2a79ae32-a902-4ac2-b597-9f5b11afb271" }
{ "name": "Computadoras y Laptops", "categoryId": "2a79ae32-a902-4ac2-b597-9f5b11afb271" }
{ "name": "Audio y Audífonos", "categoryId": "2a79ae32-a902-4ac2-b597-9f5b11afb271" }
{ "name": "Muebles", "categoryId": "8cf8f18d-499c-432a-a04c-bc9eff05426b" }
{ "name": "Electrodomésticos", "categoryId": "8cf8f18d-499c-432a-a04c-bc9eff05426b" }
{ "name": "Decoración", "categoryId": "8cf8f18d-499c-432a-a04c-bc9eff05426b" }
{ "name": "Ropa de Mujer", "categoryId": "631ba5c0-d0cf-45d9-9d31-414668025d6f" }
{ "name": "Ropa de Hombre", "categoryId": "631ba5c0-d0cf-45d9-9d31-414668025d6f" }
{ "name": "Calzado", "categoryId": "631ba5c0-d0cf-45d9-9d31-414668025d6f" }
{ "name": "Ejercicio y Gimnasio", "categoryId": "dedddac2-3db1-4fe6-8a6b-5187b85935ce" }
{ "name": "Ciclismo", "categoryId": "dedddac2-3db1-4fe6-8a6b-5187b85935ce" }
{ "name": "Camping y Aire Libre", "categoryId": "dedddac2-3db1-4fe6-8a6b-5187b85935ce" }
{ "name": "Juguetes", "categoryId": "6fc85f71-24c3-4f8f-adb1-68865351a047" }
{ "name": "Artículos para Bebé", "categoryId": "6fc85f71-24c3-4f8f-adb1-68865351a047" }
{ "name": "Juegos de Mesa", "categoryId": "6fc85f71-24c3-4f8f-adb1-68865351a047" }
{ "name": "Herramientas Eléctricas", "categoryId": "1ff141fc-4330-47b7-8881-c8b844c1ec7c" }
{ "name": "Herramientas Manuales", "categoryId": "1ff141fc-4330-47b7-8881-c8b844c1ec7c" }
{ "name": "Material Eléctrico", "categoryId": "1ff141fc-4330-47b7-8881-c8b844c1ec7c" }
{ "name": "Maquillaje", "categoryId": "e4b888bc-2cf1-4e77-8163-8cf49af30796" }
{ "name": "Cuidado de la Piel", "categoryId": "e4b888bc-2cf1-4e77-8163-8cf49af30796" }
{ "name": "Cuidado del Cabello", "categoryId": "e4b888bc-2cf1-4e77-8163-8cf49af30796" }
{ "name": "Refacciones", "categoryId": "fa903d82-8bca-491b-94e6-ce3293d0bd05" }
{ "name": "Accesorios para Auto", "categoryId": "fa903d82-8bca-491b-94e6-ce3293d0bd05" }
{ "name": "Llantas y Rines", "categoryId": "fa903d82-8bca-491b-94e6-ce3293d0bd05" }
{ "name": "Libros", "categoryId": "6744729b-1513-41a9-a843-efd0fe3b632d" }
{ "name": "Papelería y Oficina", "categoryId": "6744729b-1513-41a9-a843-efd0fe3b632d" }
{ "name": "Arte y Manualidades", "categoryId": "6744729b-1513-41a9-a843-efd0fe3b632d" }
{ "name": "Alimento para Mascotas", "categoryId": "8dcc6869-631d-4d96-b134-49e4231befc1" }
{ "name": "Accesorios", "categoryId": "8dcc6869-631d-4d96-b134-49e4231befc1" }
{ "name": "Higiene y Salud", "categoryId": "8dcc6869-631d-4d96-b134-49e4231befc1" }
```
