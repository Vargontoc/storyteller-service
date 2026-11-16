# Sprint 02: character-portraits - server

Estado: aprobado

## Alcance

- Tareas: TASK-001 a TASK-013, TASK-017, TASK-018 y TASK-019 (de `specs/character-portraits/tasks.md`).
- Dependencias internas: 002/003/009/010 dependen de 001; 006 de 002; 007 de 002 a 006; 008 de 001 y 003; 011 de 002, 005 y 006; 012 de 007 a 010; 013 de 006, 009 y 010. Los contratos 017/018 dependen de 012 y 019 de 013.
- Dependencias externas: ninguna para implementación server por reparto autorizado. TASK-014 a TASK-016 (app) dependen de las interfaces entregadas por 005, 007, 012 y 013. TASK-017 a TASK-019 se entregan al documentador-server al final de la implementación de la feature, después de TASK-014 a TASK-016 y de verificar sus interfaces.

## Origen SDD

- Requirements: 2
- Design: 2
- Tasks: 1

## Enfoque técnico

Se añadirán entidades de catálogo para retrato aprobado, candidatos, trabajos y forks, con migraciones Liquibase aditivas posteriores a `017`; los personajes de cuento conservarán sus datos históricos y tendrán una referencia nullable a `CatalogCharacter`. No se infiere ninguna correspondencia previa por nombre: cada nuevo uso del catálogo grabará la referencia explícitamente.

Un job persistido `RUNNING` es la única fuente del bloqueo, sin TTL ni timeout de generación; al arrancar, los jobs en vuelo se marcan `FAILED` y notifican el error. El adaptador de retratos enviará un único workflow ComfyUI que produzca exactamente cuatro resultados; el límite temporal existente del generador de escenas no se reutiliza como plazo de generación de retratos. Solo los fallos técnicos de transporte se traducen a fallo del job.

La implementación usa las APIs del framework realmente declarado, `es.vargontoc.framework` 0.1.0; la guía empaquetada está etiquetada 0.2.0-SNAPSHOT y el usuario confirmó su equivalencia. Se comprobarán las APIs presentes antes de codificar: `AutoIdJpaEntity` para entidades, `WriteTransactionGuard` en escrituras, `EntityMapperGuard` en mapeo y sus apoyos de pruebas. No se añadirá un segundo `EnableJpaAuditing`; los campos JSONB con `String` se declararán con tipo SQL explícito.

## Cambios por tarea

### TASK-001 Persistir retrato aprobado, candidatos, trabajos de generación y relación de fork

- **Ficheros:** migraciones `018_*` y siguientes en `api/src/main/resources/db/changelog/migrations/` (nuevas); entidades/repositorios JPA bajo `infrastructure/persistence/` (nuevos); puertos y modelos `portrait` bajo `application/ports/out/persistence/` y `domain/model/` (nuevos); `CatalogCharacterJpaEntity`, `CatalogCharacterJpaRepository`, `CharacterJpaEntity` y su creación de snapshots (modificar).
- **Enfoque:** crear tablas para retrato aprobado, candidato, job y relación de fork, con FK a catálogo y estado enumerado; añadir a `character` una FK nullable `catalog_character_id`. Preservar filas existentes con valor nulo y no realizar backfill por nombre. Las nuevas creaciones de personaje de cuento recibirán el catálogo seleccionado de forma explícita. Persistir rutas de assets como `VARCHAR`; si una carga JSONB se representa como texto, fijar `columnDefinition`/cast PostgreSQL y no dejar que Hibernate lo infiera.
- **Pruebas automáticas:** persistir/recuperar retrato; cuatro candidatos y borrado individual/conjunto; transiciones RUNNING→COMPLETED/FAILED; fork conserva original; migración deja snapshots heredados con FK nula y una creación nueva guarda la FK indicada. `mvn test` desde `api/`.
- **Prueba manual:** inspeccionar job, retrato y fork en BD y confirmar que un cuento anterior no fue modificado.

### TASK-002 Implementar el almacén de estado de los trabajos de generación

- **Ficheros:** `application/ports/out/persistence/PortraitGenerationJobStore.java`, `infrastructure/adapters/out/persistence/PortraitGenerationJobStoreAdapter.java` y repositorio JPA (nuevos).
- **Enfoque:** exponer crear, consultar RUNNING por catálogo, finalizar y listar RUNNING. Garantizar como máximo un job RUNNING por personaje mediante restricción parcial/serialización transaccional; el estado no caduca ni se modifica al consultar.
- **Pruebas automáticas:** alta RUNNING, ambas transiciones finales, ausencia de job activo y listado filtrado. `mvn test`.
- **Prueba manual:** iniciar y finalizar una generación comprobando la transición persistida.

### TASK-003 Implementar el almacén de candidatos de retrato

- **Ficheros:** `PortraitCandidateStore` y adaptador/repositorio/entidad de candidato (nuevos); extensión del almacenamiento físico bajo `infrastructure/storage/` (modificar).
- **Enfoque:** guardar lote vigente con su job, listar, reemplazar atómicamente y borrar. Separar los metadatos de la ruta física y borrar assets descartados conforme a la política; nunca eliminar el retrato aprobado al regenerar.
- **Pruebas automáticas:** cuatro candidatos, lista vacía, reemplazo sin supervivientes antiguos y borrado de uno sin afectar el resto. `mvn test`.
- **Prueba manual:** regenerar y comprobar que solo quedan las cuatro rutas nuevas.

### TASK-004 Construir el adaptador de ComfyUI para lotes de 4 retratos cartoon

- **Ficheros:** `application/ports/out/external/PortraitGeneratorPort.java`, `infrastructure/adapters/out/external/PortraitComfyUiAdapter.java` y builder `PortraitWorkflowGraphBuilder.java` (nuevos); `ComfyUIClient` y configuración ComfyUI (modificar).
- **Enfoque:** reutilizar cliente HTTP y configuración cartoon, pero construir un workflow único con lote cuatro y semillas distintas. Validar que ComfyUI entrega exactamente cuatro imágenes, descargar las cuatro y propagar fallo técnico/incompleto; no aplicar el `timeoutSeconds` del polling de escenas como deadline de retrato.
- **Pruebas automáticas:** workflow único con cuatro semillas/resultados, prompt con descripción visual y configuración cartoon, rechazo de lote incompleto y fallo de transporte. `mvn test`.
- **Prueba manual:** comparar cuatro imágenes del mismo lote y verificar estilo y seguridad infantil.

### TASK-005 Implementar el notificador de eventos de retrato por WebSocket

- **Ficheros:** `PortraitWebsocketNotifier` (nuevo, aplicación), evento `PortraitGenerationEvent` (nuevo) y `WebsocketAdapter`/evento de notificación existente (modificar).
- **Enfoque:** emitir por `/storyteller` inicio, progreso disponible, éxito y error, siempre con `catalogCharacterId`, estado y motivo seguro. Mantener difusión pública existente; no crear canal por sesión.
- **Pruebas automáticas:** cada transición publica el evento tipado correcto y el error conserva el motivo comunicable. `mvn test`.
- **Prueba manual:** observar inicio, fin y error desde un cliente del canal existente.

### TASK-006 Implementar el bloqueo derivado del trabajo de generación

- **Ficheros:** `application/service/PortraitLockService.java` y puerto de lectura (nuevos); servicios de catálogo/revisión y punto de creación de cuento (modificar).
- **Enfoque:** `isLocked(id)` consulta exclusivamente RUNNING. Aplicar la guarda antes de renombrar, pedir/confirmar revisión y crear un cuento desde catálogo; consultas siguen permitidas. No almacenar lock, TTL ni limpieza por reloj.
- **Pruebas automáticas:** RUNNING bloquea, COMPLETED/FAILED libera, consulta no escribe y espera prolongada no libera. Añadir rechazos en renombrado, revisión y nuevo uso. `mvn test`.
- **Prueba manual:** comprobar ficha consultable y operaciones rechazadas durante ejecución.

### TASK-007 Implementar la orquestación de la generación de 4 candidatos

- **Ficheros:** `PortraitGenerationService` y puerto de entrada (nuevos); comandos/resultados (nuevos); stores, adaptador y notificador (modificar).
- **Enfoque:** en transacción corta crear job RUNNING y notificar; ejecutar asíncronamente fuera de la transacción, guardar exactamente cuatro candidatos, cerrar COMPLETED y notificar. En error limpiar candidatos del ciclo, marcar FAILED y notificar. Rechazar un segundo RUNNING; regeneración crea otro job solo después de cerrar el anterior y no tiene límite de repeticiones.
- **Pruebas automáticas:** camino completo, bloqueo concurrente, regeneraciones repetidas, error/incompleto y ausencia de deadline de generación. `mvn test`.
- **Prueba manual:** generar, regenerar y simular ComfyUI caído verificando liberación.

### TASK-008 Implementar la selección del retrato aprobado

- **Ficheros:** `PortraitSelectionService`, comando y puerto (nuevos); candidate/portrait stores y storage (modificar).
- **Enfoque:** bajo bloqueo transaccional validar candidato vigente y de su propietario, convertirlo en aprobado, eliminar tres candidatos y sus ficheros. Rechazar candidato antiguo, inexistente o invalidado sin tocar el retrato existente.
- **Pruebas automáticas:** selección conserva uno, elimina tres, cambia borrador y rechaza selección obsoleta. `mvn test`.
- **Prueba manual:** seleccionar y comprobar una única imagen física y ficha aprobada.

### TASK-009 Implementar el adaptador de condiciones visuales de personaje

- **Ficheros:** `infrastructure/adapters/out/persistence/PortraitConditionAdapter.java` (nuevo).
- **Enfoque:** implementar `CharacterImageConditionPort` con consultas de retrato aprobado y assets generados de las nuevas tablas; no añadir comandos al puerto.
- **Pruebas automáticas:** ambos predicados para catálogo con/sin retrato y candidatos/imagenes. `mvn test`.
- **Prueba manual:** revisar que solo se exponen las dos consultas.

### TASK-010 Implementar la creación del fork de personaje

- **Ficheros:** `PortraitForkService`, puerto `CatalogCharacterUsagePort` y adaptador JPA (nuevos); repositorio de catálogo (modificar).
- **Enfoque:** determinar uso mediante la FK explícita de snapshots; si existe, copiar ficha al nuevo catálogo, aplicar revisión visual, usar sufijo distintivo y no copiar retrato/candidatos. El original y las filas de cuento permanecen sin cambios; la FK nula histórica no se trata como uso ni se empareja por nombre.
- **Pruebas automáticas:** fork conserva original, aparece borrador, renombrado aislado y snapshot histórico sin referencia no altera decisión por inferencia. `mvn test`.
- **Prueba manual:** crear fork de personaje usado y comprobar original, retrato y cuento intactos.

### TASK-011 Implementar la reconciliación de trabajos huérfanos al arrancar

- **Ficheros:** `PortraitGenerationReconciliationService.java` y configuración de arranque (nuevos); store/notificador (modificar).
- **Enfoque:** reconciliar al arranque únicamente jobs RUNNING sin tarea viva asociada: marcar FAILED, limpiar candidatos parciales si existen y publicar error. Aunque el despliegue actual sea de proceso único, no asumir que todo RUNNING es huérfano si la reconciliación coincide con una tarea recién lanzada; coordinar la consulta/registro de tarea viva con el orquestador. No tocar COMPLETED/FAILED ni usar TTL.
- **Pruebas automáticas:** reconciliar RUNNING huérfano, liberar bloqueo y emitir evento; preservar estados finales y conservar sin alterar un RUNNING cuya tarea sigue viva. `mvn test`.
- **Prueba manual:** reiniciar a mitad de generación y confirmar desbloqueo/error.

### TASK-012 Modificar el servicio de catálogo para generación, candidatos y fork

- **Ficheros:** `CharacterCatalogService`, `CharacterCatalogUseCase`, `CharacterCatalogController`, DTOs/comandos/vistas y `StoryGenerateCommand`/adaptador de creación de cuento (modificar); nuevos puertos de retrato (integrar).
- **Enfoque:** exponer generar/regenerar, consultar candidatos, seleccionar aprobado y estado/progreso derivado. Sustituir el fallback `UNKNOWN` por el proveedor real de TASK-009. Rechazar renombrado y la selección de protagonista durante lock. Añadir identificador de catálogo opcional/explícito al flujo de creación de cuentos para que los nuevos snapshots graben FK; el contrato OpenAPI se formaliza después.
- **Pruebas automáticas:** delegación, consulta vacía/vigente, estado RUNNING, borrador real, bloqueos y renombrado de fork aislado; creación nueva preserva referencia catálogo. `mvn test`.
- **Prueba manual:** generar desde ficha, comprobar estado y rechazos durante lock, y crear un cuento seleccionando catálogo.

### TASK-013 Modificar revisiones para invalidación y aviso de fork

- **Ficheros:** `CharacterReviewService`, `CharacterReviewUseCase`, controlador/DTOs de revisión y resultados (modificar); integrar lock, condiciones y fork.
- **Enfoque:** rechazo inmediato de solicitud o resolución bajo lock. Fuera de lock, aviso de consistencia solo si el cambio es visual y hay imágenes. Al confirmar visual sin snapshots, invalidar retrato y candidatos/assets; con snapshots, devolver confirmación de fork sin cambiar el original y crear solo tras confirmar. El descarte/cancelación no escribe.
- **Pruebas automáticas:** rechazo bajo lock; aviso visual solo desbloqueado con imágenes; cambio visual sin uso invalida y vuelve a borrador; con uso exige confirmación, fork conserva original y cancelación no escribe. `mvn test`.
- **Prueba manual:** validar ambos caminos de cambio visual y cancelación del fork.

### TASK-017 Contrato: endpoints de generación y selección de retratos

- **Responsable de ejecución:** documentador-server, al final de la feature; requiere TASK-012 y cierre de TASK-014 a TASK-016.
- **Handoff:** formalizar en `docs/contracts/openapi/paths/character/` las operaciones REST ya implementadas: generar, regenerar, listar candidatos, seleccionar y sus rechazos por lock/candidato no vigente.

### TASK-018 Contrato: esquemas de retrato, candidato y estado

- **Responsable de ejecución:** documentador-server, al final de la feature; requiere TASK-012 y cierre de TASK-014 a TASK-016.
- **Handoff:** formalizar en `docs/contracts/openapi/schemas/character*.yaml` retrato, candidato, galería, job enum RUNNING/COMPLETED/FAILED y lock derivado.

### TASK-019 Contrato: revisiones con aviso de fork

- **Responsable de ejecución:** documentador-server, al final de la feature; requiere TASK-013 y cierre de TASK-014 a TASK-016.
- **Handoff:** actualizar `docs/contracts/openapi/paths/actor/` con rechazo por lock, aviso de consistencia, aviso/confirmación/cancelación de fork y referencia de catálogo.

## Orden de implementación

1. TASK-001, TASK-004 y TASK-005: base persistente, workflow y eventos.
2. TASK-002, TASK-003, TASK-009 y TASK-010: stores, condiciones y fork sobre esa base.
3. TASK-006 y TASK-011: política de lock y recuperación al arranque.
4. TASK-007 y TASK-008: generación y selección completas.
5. TASK-012 y TASK-013: exponer catálogo, nueva selección de protagonista y revisión/fork.
6. Handoff app: TASK-014 tras 012; TASK-015 tras 013; TASK-016 tras 005, 007, 012 y 014. App debe incorporar el parámetro de referencia a catálogo al iniciar un cuento; esa integración autorizada no tiene tarea app específica y debe registrarse junto a TASK-016.
7. Handoff documentador-server: TASK-017/018 (interfaces mínimas: TASK-012) y TASK-019 (interfaz mínima: TASK-013) se ejecutan siempre al final de la feature, una vez terminadas TASK-014, TASK-015 y TASK-016 de app y verificada la integración global.

## Riesgos y dudas técnicas

- La especificación presume snapshots que no existían. Se incorpora la relación autorizada, nullable para legados y explícita para usos nuevos; no habrá asociación histórica automática.
- Los casos de prueba de TASK-013 que piden aviso visual “con personaje bloqueado” contradicen REQ-006, que exige rechazar cualquier revisión durante el bloqueo. Se aplica REQ-006: el aviso se prueba para personaje desbloqueado; bajo lock se rechaza antes de calcularlo.
- La disponibilidad de ComfyUI puede tener timeout de conexión/transporte, pero no se convierte en un deadline de job ni libera un RUNNING por tiempo. El único recuperador de job persistido es la reconciliación al arranque.
- El campo de catálogo para nueva creación de cuento cambia una interfaz que consume app; se incluye el handoff mínimo autorizado, pero TASK-014/015 no lo nombran y TASK-016 deberá verificarlo.

## Validación

- Suite del módulo: pendiente (código de TASK-005 pendiente de validar con la suite completa; evidencia anterior: correcta tras la corrección de TASK-004 por confirmación del usuario con `api/mvn clean test`)
- Integración: pendiente
- Validación manual: pendiente
