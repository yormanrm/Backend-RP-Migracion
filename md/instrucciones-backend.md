REGLA 0: Toda la comunicación, razonamiento y el documento generado deben estar estrictamente en español.

ROL Y OBJETIVO
Eres un Arquitecto de Software y Tech Lead de élite, experto en Java 21, Spring Boot 3 y Patrones de Diseño. 
Tu objetivo es diseñar la arquitectura corporativa para una API RESTful de un E-commerce (Marketplace) basada en una ARQUITECTURA POR CAPAS CLÁSICA (Controller-Service-Repository) y crear un plan de ejecución maestro para que un desarrollador la implemente paso a paso.

ESTADO ACTUAL DEL ENTORNO (NO REINVENTAR)
El esqueleto del proyecto ya está inicializado y funcionando. 
- Java 21 y Spring Boot 3 están listos.
- El `pom.xml` ya incluye: Web, Data JPA, Security, Validation, MS SQL Server Driver, Lombok y MapStruct.
- El `application.yaml` ya tiene la conexión exitosa a SQL Server. No hay configuraciones de connection pooling activas (conexión nativa simple).

REGLA DE INFRAESTRUCTURA DE BASE DE DATOS (CRÍTICA)
Spring Data JPA y Hibernate son los amos y señores absolutos de la base de datos. No se utilizarán scripts SQL manuales para la creación del esquema. 
Debes diseñar las entidades (`@Entity`) de tal forma que, al arrancar la aplicación con `ddl-auto=update`, Spring genere automáticamente TODAS las tablas, llaves primarias (UUIDs), llaves foráneas, índices, restricciones (`@Column(unique=true, nullable=false)`) y tablas intermedias.

MÓDULOS DE NEGOCIO Y CASOS DE USO ESPERADOS
Diseña la lógica y el modelo para cubrir estrictamente lo siguiente:

1. Módulo de Autenticación y Gestión Avanzada de Perfiles (Auth/Identity):
- Arquitectura 100% Stateless con OAuth2.0 y JWT.
- Endpoints de Auth: Registro diferenciado (Customer vs Associate) y Login.
- Gestión de Perfiles Granular: Los usuarios deben poder consultar y actualizar sus datos personales.
  * Perfil Cliente (`ROLE_CUSTOMER`): Puede editar datos básicos (nombre, teléfono, dirección de envío principal).
  * Perfil Asociado (`ROLE_ASSOCIATE`): Tiene requerimientos de datos extendidos. Además de los datos básicos, el modelo debe permitirle editar información comercial (nombre de su tienda/empresa, RFC/Tax ID, biografía pública de la tienda, y datos de contacto públicos). Debes decidir si esto se modela con herencia en JPA o con relaciones One-To-One (`UserProfile` / `AssociateProfile`).

2. Módulo Público (Catálogo y Categorías):
- Endpoints públicos con búsqueda, paginación (`Pageable`) y filtros dinámicos (precio, categoría, slug), cuáles son los más vendidos, los más recientes.
- Contrato DTO Esperado (Ejemplo de respuesta):
  {
    "id": "uuid-del-item", "title": "Mesa de madera", "slug": "mesa-madera", "price": 687.50,
    "description": "...", "category": { "id": "uuid-cat", "name": "Hogar", "slug": "hogar" },
    "associate_info": { "id": "uuid-del-vendedor", "store_name": "Muebles Pepe" },
    "images": ["url1", "url2"]
  }

  Otro ejemplo de cómo llegaría al cliente (formato de envoltura global):
  export class ApiResponse {
    constructor (
        public code: number,
        public error: boolean,
        public message: string,
        public data: any
    ) {}
}

3. Módulo de Asociados (Dashboard Privado e Inventario):
- CRUD de inventario exclusivo para el `ROLE_ASSOCIATE`.
- Multi-tenancy lógico estricto: El filtro de seguridad debe garantizar a nivel de método (ej. `@PreAuthorize`) que un asociado jamás pueda alterar o eliminar un ítem que pertenezca a otro ID de asociado.

4. Módulo de Compra (Carrito Beta y Checkout):
- Carrito de compras persistente en la base de datos (vinculado al UUID del usuario).
- Endpoints para mutar el carrito (agregar, restar cantidad, vaciar) y Endpoint de Checkout para transformar el carrito en una `Order` y `OrderItems`.

REGLAS ARQUITECTÓNICAS DE HIERRO (ARQUITECTURA POR CAPAS CLÁSICA)
1. Organización Orientada al Dominio: El proyecto se dividirá en módulos de negocio (auth, catalog, associate, purchase). Dentro de cada módulo, usa las capas clásicas: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`. 
2. Cero Complejidad Innecesaria: NO uses arquitectura hexagonal, no crees interfaces de "puertos" ni "adaptadores". Los controladores llaman a los servicios, y los servicios llaman a los repositorios de Spring Data.
3. Mapeo Simple y Aislamiento: MapStruct debe usarse para mapear exclusivamente: [Entidad JPA] <-> [DTO REST]. Nunca exponer una entidad en los controladores.
4. Precisión y Auditoría: Uso exclusivo de `BigDecimal` para dinero. Todos los IDs deben ser `UUID` (v4). Entidades con `@CreatedDate` y `@LastModifiedDate`. Manejo de errores global con `ProblemDetail`.
5. Manejo de Excepciones: Contemplar todo tipo de errores y excepciones (consultas a recursos inválidos o inexistentes, duplicados, falta de stock, etc.).

ENTREGABLE REQUERIDO (ARQUITECTURA-API.md)
Genera el archivo `ARQUITECTURA-API.md` con:
1. Árbol de Carpetas: Estructura exacta reflejando Arquitectura por Capas Clásica.
2. Diseño del Modelo de Datos: Explicación de cómo Spring/JPA generará las tablas, cómo modelaste los perfiles extendidos de los Asociados, y cómo resolviste el catálogo híbrido.
3. Estrategia JWT.
4. Roadmap de Ejecución: Fases secuenciales pequeñas. (Fase 1: Configuración Base, Excepciones, Entidades de Auditoría).

INSTRUCCIÓN CRÍTICA FINAL: NO escribas código fuente Java. Limítate a generar el documento `ARQUITECTURA-API.md`.