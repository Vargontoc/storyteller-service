# Sprint 02: character-portraits - app

Estado: aprobado

## Alcance

- Tareas: TASK-014, TASK-015, TASK-016 (de `specs/character-portraits/tasks.md`).
- Dependencias internas: TASK-016 depende de TASK-014. TASK-015 no depende de otra tarea de `app`.
- Dependencias externas: TASK-014 (`server`, TASK-012): API REST implementada y probada para consultar estado/job y candidatos, iniciar/regenerar la generación y seleccionar el retrato vigente; las vistas de catálogo deben devolver retrato, candidatos, estado `RUNNING`/`COMPLETED`/`FAILED` y bloqueo derivado. TASK-015 (`server`, TASK-013): API REST implementada y probada para rechazo prioritario por lock, aviso de consistencia y decisión de fork (confirmar/cancelar), sin editar el original. TASK-016 (`server`, TASK-005, TASK-007 y TASK-012): eventos tipados de retrato difundidos por `/storyteller` y APIs REST de generación/consulta entregadas; TASK-012 también debe aceptar la referencia explícita de catálogo al crear un cuento y persistirla en snapshots nuevos.

## Origen SDD

- Requirements: 2
- Design: 2
- Tasks: 1

## Enfoque técnico

La SPA extenderá los componentes de catálogo existentes y `api/characters.ts`, dejando al servidor como fuente de verdad del job y del bloqueo: `RUNNING` deshabilita mutaciones y uso en cuentos, pero mantiene la ficha consultable. No se implementarán temporizadores, TTL ni desbloqueos locales; al reconectar o recibir un evento se recargará detalle, candidatos y listado.

La galería siempre representa el lote vigente devuelto por REST: muestra exactamente cuatro candidatos, se invalida antes de seleccionar si llega un evento de nueva generación y desaparece al aprobar uno. El canal compartido `/storyteller` se ampliará de forma compatible para distinguir eventos de retrato por `catalogCharacterId`, sin alterar la reconexión ya existente ni crear un canal adicional.

La integración mínima de crear cuento se hará en `TopicSelectionModal.vue`: cargar personajes de catálogo, permitir elegir solo uno no bloqueado y enviar su identificador explícito junto a la solicitud existente. Esto materializa la relación catálogo→snapshot autorizada, sin inventar un nuevo flujo de producto. El estado actual `portraitStatus: UNKNOWN` seguirá mostrándose como desconocido hasta que el proveedor de retratos real entregue `DRAFT` o `APPROVED`; no se inferirá un retrato aprobado.

## Cambios por tarea

### TASK-014 Modificar la vista del catálogo con la generación, la galería y el progreso

- **Ficheros:** `app/src/api/characters.ts` (modificar): tipos y llamadas de estado de retrato, job, candidatos, generación/regeneración y selección ajustados al hand-off REST; `app/src/views/CharacterCatalogView.vue` (modificar): recargas coordinadas de listado, ficha y galería para el personaje seleccionado; `app/src/components/CharacterCatalogDetail.vue` (modificar): retrato aprobado, estado/progreso, acciones y bloqueo de renombrado; `app/src/components/PortraitCandidateGallery.vue` (nuevo, propuesta): galería de cuatro candidatos, regeneración y selección; `app/src/stores/websocket.ts` (modificar): exponer de forma tipada los eventos de retrato sin romper las notificaciones genéricas.
- **Enfoque:** solicitar generación o regeneración una sola vez por interacción y reflejar el job `RUNNING` persistido como indicador de progreso. Mientras siga `RUNNING`, deshabilitar renombrado, solicitud/resolución de revisión, regeneración, selección de candidato y cualquier control que use al personaje, sin ocultar sus datos. Tras evento de éxito/progreso/error, y tras reconexión, consultar el estado REST y sincronizar ficha/listado/galería; el error libera controles solo cuando REST deje de informar `RUNNING`. Renderizar exclusivamente cuatro candidatos del lote vigente y, ante regeneración remota, invalidar los candidatos locales antes de permitir seleccionar. Al aprobar, actualizar retrato, estado borrador y ocultar la galería.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para verificar tipos y compilación.
- **Prueba manual:** generar desde una ficha y comprobar indicador `RUNNING`; al finalizar, comprobar cuatro propuestas; regenerar y comprobar sustitución por cuatro nuevas; aprobar una y comprobar retrato aprobado más desaparición de galería; durante `RUNNING`, comprobar ficha consultable y todas las mutaciones/botones de uso deshabilitados; regenerar desde otra sesión y confirmar que una galería abierta se invalida y se recarga.

### TASK-015 Modificar el panel de revisión con el aviso de fork y su confirmación

- **Ficheros:** `app/src/api/characters.ts` (modificar): resultados y comandos de revisión/fork conforme al contrato REST entregado; `app/src/components/CharacterReviewPanel.vue` (modificar): aviso visual, resultado de fork y acciones de confirmar/cancelar; `app/src/components/ConfirmModal.vue` (reutilizar, sin modificar salvo que el formato de confirmación REST lo exija); `app/src/components/CharacterCatalogDetail.vue` y `app/src/views/CharacterCatalogView.vue` (modificar): propagar el lock, refrescar ficha/listado y seleccionar el fork creado cuando proceda.
- **Enfoque:** mostrar el aviso de consistencia únicamente con una propuesta permitida cuyo resultado lo indique. Si el servidor comunica que el cambio visual de un personaje en uso requiere fork, abrir confirmación explicativa con dos únicas salidas: confirmar la creación del derivado o cancelar; no mostrar ni habilitar una vía para editar el original. Cancelar no hará llamadas de escritura posteriores ni alterará la ficha. Durante `RUNNING`, el panel deshabilitará solicitar, reemplazar, aceptar y descartar y presentará el estado de generación; un rechazo del servidor prevalece sobre cualquier aviso visual, sin ofrecer bypass.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para verificar tipos y compilación.
- **Prueba manual:** solicitar cambio visual con imágenes y comprobar aviso de consistencia; repetir sin imágenes y comprobar su ausencia; en personaje usado, aceptar la propuesta y comprobar aviso de fork; confirmar y verificar personaje derivado en borrador con original intacto; cancelar y comprobar que no se crea nada ni se ofrece edición del original; durante una generación, comprobar controles de revisión deshabilitados y rechazo visible si se fuerza la operación.

### TASK-016 Integrar el flujo completo de generación de retratos

- **Ficheros:** `app/src/stores/websocket.ts` (modificar): decodificar eventos de retrato con identificador, estado, progreso y motivo comunicable, preservando toast y reconexión; `app/src/views/CharacterCatalogView.vue` y `app/src/components/CharacterCatalogDetail.vue` (modificar): suscribirse al personaje seleccionado, refrescar por evento y recuperar estado al volver a conectar; `app/src/components/PortraitCandidateGallery.vue` (modificar): invalidación/recarga de lote; `app/src/components/TopicSelectionModal.vue` (modificar): selector de `CatalogCharacter` disponible y paso del identificador al comando existente de creación de cuento; `app/src/api/generations.ts` y `app/src/api/characters.ts` (modificar): tipos y petición de creación de cuento/catálogo alineados con la interfaz final de TASK-012.
- **Enfoque:** comprobar extremo a extremo solicitud REST → job `RUNNING` → evento `/storyteller` → consulta REST de candidatos → selección o error. El websocket es una señal de refresco, no la fuente de estado: al recibir inicio, progreso, éxito, error o tras reconectar se consulta REST; así se recuperan eventos perdidos. No crear polling de timeout ni liberar controles por tiempo. La creación de cuento permitirá seleccionar un catálogo disponible desde el modal actual; un personaje `RUNNING` se verá no seleccionable y la petición incluirá su id explícito. La operación seguirá delegando validaciones definitivas al servidor, y un rechazo por lock volverá a dejar el modal utilizable sin iniciar cuento.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para verificar tipos y compilación.
- **Prueba manual:** con la app abierta, generar y comprobar eventos de inicio/finalización por `/storyteller`, progreso visible, cuatro candidatos y liberación al terminar; dejar una generación en curso sin interacción prolongada y confirmar que sigue `RUNNING` hasta evento/estado final, sin timeout UI; simular ComfyUI no disponible y verificar evento de error más controles liberados tras el estado REST `FAILED`; cerrar/reabrir o esperar reconexión y verificar recuperación desde REST; iniciar la creación de un cuento y comprobar que permite seleccionar un catálogo no bloqueado, rechaza/deshabilita uno `RUNNING` y conserva la selección al continuar el flujo existente.

## Orden de implementación

1. TASK-014 (habilitada por: TASK-012 de `server` entrega endpoints, DTOs y estados reales de retrato/job/candidatos, incluidos errores de lock y selección obsoleta).
2. TASK-015 (habilitada por: TASK-013 de `server` entrega resultados definitivos de revisión, bloqueo, aviso de consistencia y confirmación/cancelación de fork).
3. TASK-016 (habilitada por: TASK-014 integrada y TASK-005, TASK-007 y TASK-012 de `server` entregadas con eventos `/storyteller`, consulta REST recuperable y parámetro explícito de catálogo al crear cuento).

## Riesgos y dudas técnicas

- Las rutas, cuerpos y DTOs de REST/WebSocket todavía no están implementados. Mitigación: tiparlos solo después del hand-off de `server`; no fijar nombres de endpoints ni formato de evento en este sprint.
- Un evento puede perderse durante una desconexión. Mitigación: reconexión existente más recarga REST de job/candidatos; no se basa el desbloqueo ni la galería solo en el websocket.
- Los requisitos de pruebas de TASK-013 contienen un caso de aviso visual bajo lock que contradice REQ-006. Mitigación UI: el bloqueo es prioritario, por lo que se deshabilita la revisión y no se muestra aviso ni bypass hasta que el servidor deje de informar `RUNNING`.
- El catálogo actual puede devolver `portraitStatus: UNKNOWN`. Mitigación: mantener el indicador de estado no disponible; solo `DRAFT`/`APPROVED` entregados por el proveedor real habilitan la semántica de borrador/retrato.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
