# Sprint 05: agent-narrator - app

Estado: aprobado

## Alcance

- Tareas: TASK-026, TASK-027, TASK-028, TASK-029, TASK-030, TASK-031, TASK-032, TASK-033, TASK-034, TASK-035 (de `specs/agent-narrator/tasks.md`). Las porciones app de TASK-026 a TASK-028 comprenden los clientes y el tipado de las interfaces entregadas por `server`.
- Dependencias internas: TASK-030 depende de TASK-027; TASK-031 depende de TASK-027, TASK-028 y TASK-029; TASK-032 de TASK-031; TASK-033 de TASK-026, TASK-028 y TASK-029; TASK-034 de TASK-026, TASK-029 y TASK-033. TASK-035 no depende de otra tarea app.
- Dependencias externas: TASK-026 (`server`, porción server de TASK-026; indirectas TASK-019 y TASK-024): vista y detalle de páginas entregan `hasAudio`, `narrating`, audio sólo para interiores y el rechazo comunicable por narración en curso. TASK-027 (`server`, porción server de TASK-027; indirectas TASK-010 y TASK-011): operaciones REST probadas para biblioteca, idiomas, subida multipart, borrado y voces elegibles, con motivos estables. TASK-028 (`server`, porción server de TASK-028; indirectas TASK-019 a TASK-022): operaciones REST probadas para iniciar/renarrar, consultar estado y descargar audio interior, con motivos estables. TASK-029 (`server`, TASK-018 y TASK-023): `/storyteller` emite eventos de narración y la reconciliación comunica el resultado tras reinicio. TASK-035 (`server`, TASK-025): la respuesta de edición de beat señala el borrado de audio y el rechazo por narración en curso. Los contratos TASK-036 a TASK-042 se formalizan al cierre documental de `server`, tras sus dependencias de implementación; no son precondición para implementar app.

## Origen SDD

- Requirements: 1
- Design: 1
- Tasks: 1

## Enfoque técnico

La SPA centralizará los hand-offs REST en `voices.ts` y `storyNarration.ts`; los tipos conservarán los motivos y el servidor seguirá siendo fuente de verdad para elegibilidad, estado, bloqueo y audio. Las rutas son propuestas hasta que server entregue la interfaz equivalente implementada y probada.

`websocket-store` incorporará eventos de narración como señales tipadas para recarga, sin alterar los de retratos ni páginas. La biblioteca se integrará como tercera sección principal y reutilizará `ConfirmModal`, `ApiError` y los patrones de carga actuales.

El modal del cuento mostrará controles de narración exclusivamente cuando `scriptVersion` indique escaleta. El panel de páginas recibirá los indicadores autoritativos y reproducirá únicamente el interior visible mediante URL temporal; no creará ni conservará audio de forma optimista.

## Cambios por tarea

### TASK-026 Integración: vista de páginas con audio y narración en curso

- **Ficheros:** `app/src/api/storyPages.ts` (modificar): ampliar `StoryPagesView` y `PageDetail` con audio/narración y preservar el motivo de rechazo; `app/src/api/storyPages.spec.ts` (modificar): respuestas y errores de los nuevos campos.
- **Enfoque:** consumir los campos entregados por server, sin inferir audio por la voz ni narración por eventos. Mantener portada sin audio y propagar el `ApiError` de la mutación bloqueada para que los paneles lo comuniquen.
- **Pruebas automáticas:** cuento narrado con `hasAudio` global e interior, portada sin audio, cuento en curso y 409 con motivo de narración; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-033 y TASK-034.

### TASK-027 Integración: cliente de la biblioteca de voces con el servidor

- **Ficheros:** `app/src/api/voices.ts` (nuevo, propuesta): tipos y operaciones de biblioteca, idiomas, `FormData`, borrado y elegibles; `app/src/api/voices.spec.ts` (nuevo, propuesta): transporte simulado; `app/src/api/httpClient.ts` (modificar sólo si necesita no imponer JSON a multipart).
- **Enfoque:** aislar rutas y envelopes del servidor, enviar nombre, idioma, permiso y fichero como multipart sin fijar manualmente `Content-Type`; conservar los motivos del servidor. Tipar disponibilidad/motivo de la voz del sistema, marca de sistema y elegibilidad.
- **Pruebas automáticas:** listado, español por defecto en catálogo, subida válida multipart, cada rechazo ALT-001, protección de voz de sistema y filtro de elegibles; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-030.

### TASK-028 Integración: cliente de narración con el servidor

- **Ficheros:** `app/src/api/storyNarration.ts` (nuevo, propuesta): inicio/renarración, estado y blob de audio interior; `app/src/api/storyNarration.spec.ts` (nuevo, propuesta): operaciones, rutas y errores; `app/src/api/httpClient.ts` (reutilizar `getForBlob`).
- **Enfoque:** encapsular estado habilitado/motivo, curso, audio, voz y resultado del último intento; iniciar es asíncrono y no modifica estado local. La descarga se limita a interiores y sus 404/motivos siguen siendo `ApiError`.
- **Pruebas automáticas:** inicio con voz elegible, ALT-003/004/005 comunicables, estado completo y blob/ausencia de audio; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-031 y TASK-033.

### TASK-029 Eventos de narración en el store de WebSocket

- **Ficheros:** `app/src/stores/websocket.ts` (modificar): tipo discriminado `NarrationEvent`, último evento y señal de reconexión; `app/src/stores/websocket.spec.ts` (modificar): decodificación y regresiones.
- **Enfoque:** reconocer `STARTED`, `COMPLETED` y `FAILED` por `storyId`, incluyendo voz sólo en éxito y motivo en error. Cada mensaje obtiene identificador local para que eventos iguales se observen; el cambio de `connectionId` sigue impulsando una recarga REST.
- **Pruebas automáticas:** tres desenlaces con cuento/campos, reconexión, y tratamiento sin cambios de retrato y página; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-031.

### TASK-030 Vista de la biblioteca de voces

- **Ficheros:** `app/src/views/VoiceLibraryView.vue` (nuevo, propuesta): listado, formulario, avisos y borrado; `app/src/views/VoiceLibraryView.spec.ts` (nuevo, propuesta); `app/src/App.vue` (modificar): navegación y montaje de la vista.
- **Enfoque:** cargar biblioteca e idiomas desde `voices.ts`; preseleccionar el idioma marcado por servidor; mostrar nombre, idioma, marca y aviso de sistema indisponible. El borrado sólo se ofrece a subidas y exige confirmación; los fallos no alteran optimistamente la lista.
- **Pruebas automáticas:** español inicial, marca y protección del sistema, aviso ALT-002, rechazo de subida sin cambiar lista y borrado confirmado; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** ejecutar los ocho pasos de TASK-030, incluidos límites 5/30 s, duplicado y sistema ausente.

### TASK-031 Panel de narración del cuento

- **Ficheros:** `app/src/components/StoryNarrationPanel.vue` (nuevo, propuesta): estado, selector, confirmación y resultados; `app/src/components/StoryNarrationPanel.spec.ts` (nuevo, propuesta); reutilizar `app/src/components/ConfirmModal.vue`.
- **Enfoque:** cargar estado y voces elegibles, preseleccionar sistema disponible y recargar sólo ante eventos/reconexión del cuento abierto. Mostrar acción y motivo autoritativo; al renarrar pedir confirmación antes del inicio. Nunca solicitar ni renderizar el guion de audio.
- **Pruebas automáticas:** deshabilitado por páginas, elegibles/preselección y ALT-002, cancelación de renarración, curso, éxito/error y ALT-003/004/005; ausencia del guion; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** ejecutar los nueve pasos de TASK-031, con Chatterbox/Ollama y conflicto de generación.

### TASK-032 Panel de narración solo en cuentos con escaleta

- **Ficheros:** `app/src/components/StoryModal.vue` (modificar): montar `StoryNarrationPanel` sólo con `hasOutline`; `app/src/components/StoryModal.spec.ts` (modificar): bifurcación por `scriptVersion`.
- **Enfoque:** reutilizar el marcador ya cargado como única condición; no cambiar componentes ni flujos de cuentos antiguos.
- **Pruebas automáticas:** cuento con escaleta monta el panel; cuento antiguo no lo monta y conserva la vista anterior; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** abrir un cuento de cada clase y verificar la presencia exclusiva indicada en TASK-032.

### TASK-033 Reproductor del audio de la página que se ve

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): control de reproducción del interior activo; `app/src/components/PathAudio.vue` (reutilizar/modificar sólo si el origen blob de narración requiere una prop o cargador); `app/src/components/StoryPagesPanel.spec.ts` (modificar).
- **Enfoque:** pedir el blob mediante `storyNarration.ts` sólo para la página interior visible declarada con audio, liberar URL al cambiar/desmontar y no encadenar páginas al evento `ended`. Recargar por evento de éxito para exponer el control sin intervención manual.
- **Pruebas automáticas:** reproductor/carga de interior, parada sin siguiente, ausencia en portada/sin audio y aparición tras éxito; preservar reproducción después de borrar voz; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** ejecutar los tres pasos de TASK-033.

### TASK-034 Aviso de borrado de audio y bloqueo de páginas durante la narración

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): texto de confirmación y bloqueo; `app/src/components/StoryPagesPanel.spec.ts` (modificar).
- **Enfoque:** ampliar el aviso de reapertura/aceptación de propuesta sólo si `hasAudio`; cancelar sigue sin enviar mutación. Deshabilitar acciones cuando `narrating` sea cierto y presentar el motivo retornado ante una carrera; tras confirmar, recargar el estado en vez de borrar audio local.
- **Pruebas automáticas:** aviso con/sin audio, cancelación sin petición, bloqueo/aviso en curso y estado recargado que mantiene narración inhabilitada hasta reaceptar; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** ejecutar los cuatro pasos de TASK-034.

### TASK-035 Aviso de borrado de audio al editar un beat

- **Ficheros:** `app/src/components/StoryScriptPanel.vue` (modificar): texto derivado de resultado del servidor y error de curso; `app/src/components/StoryScriptPanel.spec.ts` (nuevo, propuesta) o ampliar la prueba vigente equivalente.
- **Enfoque:** conservar la confirmación en dos pasos y añadir la consecuencia de audio sólo cuando TASK-025 la informe; no deducirla por estado local. Mostrar el motivo de narración en curso sin aplicar ni recargar como si la edición hubiera tenido éxito.
- **Pruebas automáticas:** aviso con audio, aviso existente sin audio y 409/resultado de curso; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** editar un beat de cuento narrado, confirmar y verificar ausencia de audio y voz.

## Orden de implementación

1. TASK-026, TASK-027 y TASK-028 (habilitadas por: las porciones server correspondientes entregadas e interfaces REST probadas; no por la formalización posterior de TASK-036 a TASK-042).
2. TASK-029 (habilitada por: TASK-018 y TASK-023 de server entregadas con eventos de narración y reconciliación).
3. TASK-030 (habilitada por: TASK-027 app y su interfaz server de biblioteca).
4. TASK-031 (habilitada por: TASK-027, TASK-028 y TASK-029 app integradas).
5. TASK-032 (habilitada por: TASK-031 integrada y `scriptVersion` disponible).
6. TASK-033 (habilitada por: TASK-026, TASK-028 y TASK-029 integradas).
7. TASK-034 (habilitada por: TASK-026, TASK-029 y TASK-033 integradas).
8. TASK-035 (habilitada por: TASK-025 de server entregada con aviso/resultado de edición de beat).

## Riesgos y dudas técnicas

- Las rutas, payloads y discriminadores definitivos no están documentados todavía. Mitigación: `server` entrega interfaces REST/WebSocket implementadas y probadas; concentrar el ajuste en los clientes y store. TASK-036 a TASK-042 no bloquean este sprint.
- `httpClient` fija JSON cuando hay cuerpo. Mitigación: comprobar y adaptar sólo su caso multipart para no romper clientes JSON existentes.
- Un evento puede perderse durante desconexión o llegar fuera de orden. Mitigación: eventos sólo disparan recarga REST y `connectionId` obliga sincronización.
- El estado puede variar entre la confirmación y la mutación de página/beat. Mitigación: no mutar optimistamente; presentar el rechazo autoritativo y recargar tras éxito.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
