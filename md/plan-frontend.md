Actúa como un Principal Software Architect experto en Spring Boot y Angular 21. 

Tu misión es utilizar la herramienta Graphify (o analizar el graph.json) para auditar la arquitectura actual de este backend de manera exhaustiva. Con base en ese análisis de la realidad actual del código (Controladores, DTOs, Entidades y Configuración de Seguridad), redactarás un documento maestro llamado `instrucciones-arquitectura-front.md`.

Este archivo será inyectado como el "System Prompt / Guía de Ejecución" en una nueva sesión de Claude encargada de desarrollar el frontend desde cero. No asumas nada; todo debe estar basado estrictamente en lo que el backend expone hoy.

El archivo `instrucciones-arquitectura-front.md` DEBE contener las siguientes secciones con un nivel de detalle extremo:

1. **MAPEO DE MÓDULOS Y ENDPOINTS (LA VERDAD ABSOLUTA)**:
   - Enumera los 4 módulos del negocio (1. Storefront Público, 2. Autenticación y Perfiles, 3. Checkout, 4. Dashboard Privado de Asociados).
   - Para CADA módulo, lista explícitamente los Endpoints del backend que lo alimentarán (ej. `GET /api/v1/products`, `POST /api/v1/auth/login`).
   - Define qué rol de Spring Security (ej. ROLE_USER, ROLE_ASSOCIATE, ROLE_ADMIN) tiene acceso a qué endpoint.

2. **POLÍTICA DE DISEÑO UI/UX (REGLA CRÍTICA INQUEBRANTABLE)**:
   - Incluye esta advertencia literal para el agente del frontend: *"ESTRICTAMENTE PROHIBIDO INVERTIR TIEMPO EN DISEÑO VISUAL. No se implementará nada UI, no se usarán utilidades complejas de Tailwind CSS, ni se intentará hacer la aplicación estéticamente agradable. El objetivo de esta fase es 100% FUNCIONAL. La UI será cruda y básica. Se permite el uso de PrimeNG únicamente para montar tablas de datos o formularios funcionales rápidos. Prioriza el Data-Binding, las llamadas HTTP y el enrutamiento."*

3. **ESTÁNDARES DE DESARROLLO EN ANGULAR 21**:
   - Exige explícitamente el uso exclusivo de **Standalone Components** (nada de NgModules).
   - Exige el uso del nuevo Control Flow de Angular (`@if`, `@for`, `@switch`).
   - Exige el uso de **Signals** (`signal()`, `computed()`, `effect()`) para el manejo del estado local.
   - Exige el uso de la función `inject()` para la inyección de dependencias en lugar del constructor tradicional.

4. **TIPADO FUERTE (CONTRATOS DE DATOS)**:
   - Analiza los DTOs de Request y Response de este backend y redacta en el documento las `interfaces` exactas de TypeScript que el frontend debe crear. 
   - Especifica las conversiones exactas (ej. un `Long` de Java será un `number` en TS, un `LocalDateTime` será un `string` o `Date`). Deben coincidir letra por letra con las propiedades de los DTOs de Spring.

5. **ARQUITECTURA DE CARPETAS Y SEGURIDAD**:
   - Define el árbol de carpetas exacto siguiendo una estructura Feature-Based (ej. `src/app/features/auth/...`, `src/app/core/interceptors/...`, `src/app/shared/...`).
   - Define cómo y dónde se almacenará el JWT token generado por este backend.
   - Especifica la creación de un `HttpInterceptor` para adjuntar el token a las peticiones seguras.
   - Especifica la creación de Guards funcionales (`canActivateFn`) basados en los roles detectados en el backend para proteger el Dashboard de Asociados y el Checkout.

6. **ROADMAP DE EJECUCIÓN (PLAN ATÓMICO PARA EL LLM)**:
   - Crea un plan de ejecución dividido en Fases muy pequeñas (ej. Fase 1: Setup y Core, Fase 2: Auth Feature, Fase 3: Storefront...).
   - Cada paso dentro de una fase debe ser atómico (una sola tarea a la vez) para que el LLM del frontend pueda ejecutar de a un paso por prompt sin alucinar ni perder el contexto.

Genera el documento `instrucciones-arquitectura-front.md` en la raíz de este proyecto con formato Markdown profesional.