# Purga de base de datos — SQL Server

> Ejecutar manualmente contra la base **antes de arrancar el backend** con los cambios de la planificación (`planificaciondemejoras.md`). Al siguiente arranque, Hibernate (`ddl-auto: update`) recrea todas las tablas con el esquema nuevo.

## ¿Por qué DROP y no DELETE?

`ddl-auto: update` solo **agrega** columnas; nunca elimina ni renombra. La reestructuración cambia columnas de forma incompatible con el esquema viejo:

- `associate_profiles.tax_id` → `rfc` (columna nueva NOT NULL; la vieja quedaría huérfana).
- `items.category_id` (NOT NULL viejo) → `subcategory_id`; el viejo NOT NULL rompería los inserts nuevos.
- `items.stock` pasa de NOT NULL a opcional; `categories.slug` desaparece (quedaría NOT NULL huérfano).
- Columnas nuevas NOT NULL (`active`, `type`) no se pueden agregar a tablas con filas existentes.

Con `DELETE` las columnas viejas NOT NULL seguirían existiendo y romperían la aplicación. **DROP** deja a Hibernate crear el esquema definitivo limpio.

## Script (DROP en orden de dependencias)

```sql
-- De hoja a raíz para no violar FKs. IF EXISTS tolera tablas aún no creadas.
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;          -- incluye la auto-referencia parent_order_id
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS item_images;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS user_addresses;  -- nueva (Paso 6)
DROP TABLE IF EXISTS associate_profiles;
DROP TABLE IF EXISTS subcategories;   -- nueva (Paso 3)
DROP TABLE IF EXISTS brands;          -- nueva (Paso 3)
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
```

## Verificación

Tras ejecutar el script y **arrancar el backend una vez**:

```sql
SELECT name FROM sys.tables ORDER BY name;
```

Deben existir: `associate_profiles`, `brands`, `carts`, `cart_items`, `categories`, `items`, `item_images`, `orders`, `order_items`, `subcategories`, `users`, `user_addresses`.

> Nota: al arrancar sin ningún ADMIN, el bootstrap crea el administrador inicial con `app.admin.email` / `app.admin.password` (por defecto `admin@marketplace.local` / `admin12345` en desarrollo — cambiar por variables de entorno `ADMIN_EMAIL` / `ADMIN_PASSWORD` en producción).
