# Sprint 01: character-inventory - app

Estado: aprobado

## Alcance

- Tareas: TASK-007, TASK-008, TASK-009, TASK-010, TASK-011 (de `specs/character-inventory/tasks.md`).
- Dependencias internas: TASK-008 depende de TASK-007. TASK-009, TASK-010 y TASK-011 no tienen dependencias internas entre sí y se ejecutan al final como tareas de documentación.
- Dependencias externas: TASK-007 (`server`, TASK-003 y TASK-004): el catálogo REST debe exponer y probar listado, detalle, alta automática/manual, rechazo con motivo, estado derivado, revisión pendiente y renombrado. TASK-008 (`server`, TASK-005 y TASK-006): deben estar expuestas y probadas la solicitud, consulta, aceptación y descarte de revisiones de catálogo, incluidas confirmación de sustitución, rechazo y aviso visual. TASK-009 (`server`, TASK-003 y TASK-004), TASK-010 (`server`, TASK-005 y TASK-006) y TASK-011 (`server`, TASK-004): las operaciones y DTOs REST definitivos deben estar implementados y comprobados para poder formalizar su contrato.

## Origen SDD

- Requirements: 1
- Design: 1
- Tasks: 1

## Enfoque técnico

La SPA conservará el patrón actual de vistas compuestas por componentes, llamadas tipadas en `src/api/` y feedback mediante el almacén de toasts. Se incorporará el catálogo como una vista independiente, integrada desde la aplicación sin introducir un router, y se reutilizarán `ConfirmModal` para decisiones irreversibles y `ApiError` para los mensajes del servidor.

La ficha seleccionada será la fuente de verdad local: tras una alta, renombrado o resolución de revisión se recargará el detalle y se actualizará el listado para reflejar estado borrador y badge pendiente. No se implementará gestión de retratos ni generación de assets. Las rutas, cuerpos y tipos definitivos del cliente se ajustarán a la API realmente entregada por `server`; no se inventarán endpoints antes de ese hand-off.

TASK-009 a TASK-011 son tareas de contrato y las ejecutará `documentador-app`, al final y con estrategia `implementation-first`; este sprint las conserva para ordenar la entrega, no para que el desarrollador de SPA modifique `docs/contracts/`.

## Cambios por tarea

### TASK-007 Construir la vista del catálogo y el modal de alta

- **Ficheros:** `app/src/api/characters.ts` (nuevo, propuesta): tipos del catálogo y operaciones HTTP ajustadas a los endpoints entregados; `app/src/views/CharacterCatalogView.vue` (nuevo, propuesta): listado, selección y recarga de la ficha; `app/src/components/CharacterCatalogDetail.vue` (nuevo, propuesta): detalle textual, estado borrador y badge de revisión; `app/src/components/CharacterFormModal.vue` (nuevo, propuesta): alta automática o manual y presentación del rechazo; `app/src/App.vue` (modificar): acceso a la vista de catálogo junto a la vista actual; `app/src/style.css` (modificar, si hace falta): estilos compartidos mínimos para navegación y estados.
- **Enfoque:** consumir listado y detalle del catálogo entregados por TASK-003/TASK-004. Mostrar un estado vacío inequívoco; cada elemento informará nombre, borrador y revisión pendiente. El modal permitirá escoger alta automática o enviar una frase manual, bloqueará envíos duplicados y mostrará el motivo devuelto para un rechazo sin añadir el elemento al listado. Al éxito, seleccionará y mostrará la ficha completa. Mantener el renombrado disponible en el detalle si forma parte de la operación REST entregada; preservar los cuentos existentes fuera de la UI de catálogo.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** comprobar catálogo vacío; comprobar nombres, borrador y badge con datos; seleccionar una ficha y verificar nombre y ambas descripciones; crear automáticamente, crear desde una frase válida y verificar la ficha; enviar una frase rechazada y verificar el motivo sin nuevo personaje; confirmar que el badge aparece tanto en listado como detalle.

### TASK-008 Integrar el panel de revisión en el catálogo

- **Ficheros:** `app/src/api/characters.ts` (modificar): tipos y operaciones de revisión de catálogo alineados con TASK-005/TASK-006; `app/src/components/CharacterReviewPanel.vue` (nuevo, propuesta): solicitud, propuesta, diff, avisos y acciones de resolución; `app/src/components/CharacterCatalogDetail.vue` (modificar): incluir el panel en la ficha seleccionada; `app/src/views/CharacterCatalogView.vue` (modificar): refrescar listado y selección tras cada resultado; `app/src/components/ConfirmModal.vue` (reutilizar, sin modificar salvo necesidad justificada).
- **Enfoque:** permitir solicitar un cambio con `target` e `hint`, mostrar un rechazo sin crear propuesta y renderizar la propuesta pendiente con un diff legible respecto de la ficha actual. Ante una pendiente previa, abrir confirmación y repetir la solicitud solo si el usuario la acepta. Mostrar el aviso de consistencia únicamente cuando lo devuelva el servidor. Aceptar o descartar actualizará la ficha, eliminará la propuesta y hará desaparecer el badge tras confirmar la respuesta del servidor.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** enviar cambio válido y verificar propuesta/diff; verificar rechazo con motivo; crear una segunda solicitud con pendiente, confirmar el reemplazo y comprobar la nueva pendiente; verificar aviso visual cuando proceda; aceptar y descartar en casos separados, comprobando respectivamente actualización o conservación de ficha y desaparición del badge.

### TASK-009 Contrato: openapi - recurso de catálogo de personajes

- **Responsable de ejecución:** `documentador-app` (tarea de contrato; siempre después de TASK-007 y de la entrega de TASK-003/TASK-004).
- **Ficheros:** `docs/contracts/openapi/openapi.yaml` (modificar): registrar el recurso; `docs/contracts/openapi/paths/character/catalog-service.yaml` (nuevo, propuesta): listado y altas; `docs/contracts/openapi/paths/character/catalog-by-id-service.yaml` (nuevo, propuesta): detalle y renombrado; `docs/contracts/openapi/schemas/character*.yaml`, `schemas/requests/character*.yaml` y `schemas/responses/character*.yaml` (nuevos, propuesta): modelos de catálogo, comandos y envoltorios mínimos que coincidan con la implementación.
- **Enfoque:** formalizar, una vez implementada la API, listado, detalle, alta automática, alta manual y renombrado, incluidas respuestas de rechazo con motivo. Mantener los esquemas del catálogo separados de `actor.yaml` para no confundir el maestro con una instantánea de cuento. Referenciar los ficheros nuevos desde la raíz siguiendo la organización modular OpenAPI existente.
- **Pruebas automáticas:** validar la especificación OpenAPI con la herramienta de validación disponible en el repositorio; comprobar que las cinco operaciones y la respuesta de rechazo están referenciadas y son resolubles.
- **Prueba manual:** recorrer cada operación del contrato y contrastar método, entrada, salida y errores con el controlador/DTOs ya entregados por `server`.

### TASK-010 Contrato: openapi - adaptación de revisiones de personaje al catálogo

- **Responsable de ejecución:** `documentador-app` (tarea de contrato; siempre después de TASK-008 y de la entrega de TASK-005/TASK-006).
- **Ficheros:** `docs/contracts/openapi/paths/actor/review-service.yaml`, `review-by-id-service.yaml` y `confirm-review-service.yaml` (modificar o sustituir por rutas `paths/character/` si esa es la superficie REST final): operaciones de revisión sobre catálogo; `docs/contracts/openapi/schemas/requests/actor-review-command.yaml` y `confirm-review-request.yaml` (modificar o separar en esquemas de catálogo): eliminar la exigencia de `storyId` y usar el identificador de catálogo; `docs/contracts/openapi/schemas/actor-review.yaml` y sus respuestas (modificar o crear equivalentes de catálogo): propuesta, estado, rechazo, confirmación de reemplazo y aviso visual; `docs/contracts/openapi/openapi.yaml` (modificar si cambian rutas).
- **Enfoque:** reflejar estrictamente las cuatro interfaces REST de revisión entregadas por el servidor: solicitar, consultar, aceptar y descartar. El contrato identificará el `CatalogCharacter`, no un actor de cuento ni `storyId`, y describirá los estados alternativos relevantes sin afirmar formatos no entregados por la implementación.
- **Pruebas automáticas:** validar la especificación OpenAPI afectada; comprobar por referencia que el comando de revisión no requiere `storyId`, que usa identificador de catálogo y que documenta rechazo, confirmación de descarte previo y aviso de consistencia.
- **Prueba manual:** contrastar las cuatro operaciones, identificadores, respuestas y errores con el controlador y DTOs finales de TASK-005/TASK-006.

### TASK-011 Contrato: openapi - referencia al catálogo y separación del snapshot de cuento

- **Responsable de ejecución:** `documentador-app` (tarea de contrato; siempre al final y después de la entrega de TASK-004).
- **Ficheros:** `docs/contracts/openapi/schemas/actor.yaml` (modificar): modelar la instantánea de cuento y su referencia opcional o requerida al catálogo según la API entregada; `docs/contracts/openapi/schemas/character*.yaml` (nuevo o modificar): mantener separado el maestro del catálogo; `docs/contracts/openapi/schemas/actor-summary.yaml`, respuestas y esquemas que lo referencian (modificar solo si la implementación expone la relación).
- **Enfoque:** distinguir explícitamente la ficha maestra reutilizable de `StoryActorSnapshot`, conservando en el segundo los textos/nombre usados al crear el cuento y una referencia al catálogo cuando la exponga el servidor. No convertir retrospectivamente actores históricos ni introducir una propagación de renombrados que contradiga DEC-001 y DEC-005.
- **Pruebas automáticas:** validar la especificación OpenAPI; comprobar que los esquemas diferencian maestro y snapshot, y que este último contiene los datos necesarios para conservar el nombre previo.
- **Prueba manual:** revisar los esquemas y cotejarlos con los DTOs de TASK-004, verificando la separación y las propiedades conservadas.

## Orden de implementación

1. TASK-007 (habilitada por: TASK-003 y TASK-004 de `server` entregadas con endpoints y DTOs REST probados).
2. TASK-008 (habilitada por: TASK-007 integrada y TASK-005/TASK-006 de `server` entregadas con el ciclo de revisión REST probado).
3. TASK-009 — `documentador-app` (habilitada por: TASK-003/TASK-004 entregadas y las rutas/DTOs finales disponibles; formalizar después de la SPA).
4. TASK-010 — `documentador-app` (habilitada por: TASK-005/TASK-006 entregadas y las rutas/DTOs finales disponibles; formalizar después de TASK-008).
5. TASK-011 — `documentador-app` (habilitada por: TASK-004 entregada; formalizar al final junto con los demás contratos).

## Riesgos y dudas técnicas

- El servidor aún está planificado, no entregado; rutas, DTOs y semántica exacta de conflictos/rechazos no deben anticiparse en el cliente ni en OpenAPI. Mitigación: bloquear cada lote hasta su condición de entrega y contrastar la implementación antes de formalizar contratos.
- La aplicación no dispone de framework de pruebas de componentes. Mitigación: `npm run build` como comprobación automática y ejecución de todas las pruebas manuales explícitas de TASK-007 y TASK-008.
- `app/` no contiene router; añadirlo para esta feature ampliaría innecesariamente la arquitectura. Mitigación: integrar la vista con el patrón de navegación local existente y no introducir dependencias nuevas salvo necesidad validada.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
