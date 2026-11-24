# Sprint 06: export-import-feature - app

Estado: aprobado

## Alcance

- Tareas: TASK-016, TASK-017, TASK-018, TASK-019, TASK-020, TASK-021, TASK-031 y TASK-032 (de `specs/export-import-feature/tasks.md`). TASK-031 y TASK-032 se incorporan a `app` por confirmación expresa del usuario aunque aún no tengan ruta asignada en el mapa.
- Dependencias internas: TASK-017 depende de TASK-016; TASK-019, TASK-020 y TASK-021 dependen de TASK-018; TASK-032 depende de TASK-031.
- Dependencias externas: TASK-016 (`server`, TASK-004, TASK-005 y TASK-011): contrato REST implementado y probado que devuelve `setting` por beat y `coverSetting`/`coverTone`, recibe `setting` al editar un beat y comunica si la generación de portada se inició al aprobar. TASK-018 (`server`, TASK-008 y TASK-014): operaciones REST implementadas y probadas de portada/páginas que admiten hint sobre página 0 y subida multipart de una imagen asociada al contenido aceptado, con motivos de rechazo estables. TASK-019 (`server`, TASK-015): vista y detalle de página entregan la escena aceptada completa e independiente de portada y página 1. TASK-021 (`server`, TASK-009, TASK-010 y TASK-011): la página 0 respeta el bloqueo de página 1, la reaprobación conserva interiores y la aprobación comunica el arranque o no de la portada. TASK-031 (`server`, TASK-025 y TASK-030): dos endpoints de exportación síncrona implementados y probados, que devuelven zip o un rechazo estructurado con todas las faltas/reglas y su portada o página cuando aplique.

## Origen SDD

- Requirements: 7
- Design: 2
- Tasks: 2

## Enfoque técnico

La SPA conservará a `server` como fuente de verdad del estado de guion, portada, páginas, elegibilidad de subida y exportación. Se ampliarán los clientes tipados existentes para transportar los campos y errores sin rellenar valores de cuentos legacy ni mutar optimistamente la interfaz.

`StoryScriptPanel` presentará el planteamiento de la portada separado del beat 1 y extenderá el formulario del beat. `StoryPagesPanel` tratará la portada como página 0 en los flujos ya existentes: sus eventos WebSocket sólo provocarán recarga REST, y las operaciones se habilitarán según los datos autoritativos recibidos.

La descarga heredada de Nubi en `StorytellerView` se reemplazará por un cliente específico de exportaciones y dos acciones explícitas (Nubi Game y agente visual). Ambas descargarán el blob sólo en éxito; ante un rechazo mostrarán íntegramente la lista recibida, sin intentar prevalidarla en el navegador.

## Cambios por tarea

### TASK-016 Cliente del guion con los campos ampliados

- **Ficheros:** `app/src/api/storyScript.ts` (modificar): añadir `setting` a los campos de beat, el planteamiento tipado de portada al guion y el indicador de inicio de portada al resultado de aprobación; enviar `setting` en la edición. `app/src/api/storyScript.spec.ts` (nuevo, propuesta): cubrir deserialización, petición y aprobación.
- **Enfoque:** mantener como ausentes los campos no enviados por cuentos anteriores; no derivar lugar ni tono desde el beat 1. Ajustar el tipo de la respuesta de aprobación para exponer su indicador sin alterar el `Story` que usan otros consumidores.
- **Pruebas automáticas:** respuesta completa y legacy, cuerpo de edición con lugar, y aprobación iniciada/no iniciada; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-017.

### TASK-017 Escaleta con lugar por beat y planteamiento de portada

- **Ficheros:** `app/src/components/StoryScriptPanel.vue` (modificar): presentar lugar en cada beat, bloque propio de lugar y tono de portada y campo editable de lugar. `app/src/components/StoryScriptPanel.spec.ts` (modificar): ampliar montaje, formulario y envío.
- **Enfoque:** mostrar el planteamiento de portada fuera de la lista de beats y del beat 1; conservarlo como información de la escaleta que se aprueba. Cargar el lugar actual al entrar en edición y mostrar errores de servidor sin modificar localmente el guion.
- **Pruebas automáticas:** cuatro lugares visibles, planteamiento de portada separado y edición que transporta `setting`; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** crear cuento de cuatro páginas, comprobar los cuatro lugares y el planteamiento separado; editar el lugar del beat 2 y aprobar el guion.

### TASK-018 Cliente de portada y páginas: subida de imagen y hint de portada

- **Ficheros:** `app/src/api/storyPages.ts` (modificar): incorporar la subida multipart de imagen aceptada y soportar explícitamente el hint de página 0. `app/src/api/storyPages.spec.ts` (modificar): peticiones multipart, hint de portada y propagación de rechazos.
- **Enfoque:** reutilizar `postForm` para no fijar el `Content-Type`; el cliente pasa el fichero y conserva `ApiError` con el motivo estable que entrega el servidor. La portada se identifica por su `pageId` de página 0, sin ruta ni estado inventados en la SPA.
- **Pruebas automáticas:** subida a página 3 con fichero, rechazo «no es una imagen», hint sobre portada y rechazo por planteamiento ausente; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-020 y TASK-021.

### TASK-019 Descripción de escena completa visible en la portada y en las páginas

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): renderizar la escena completa del contenido aceptado tanto de página 0 como de interiores. `app/src/components/StoryPagesPanel.spec.ts` (modificar): diferenciar portada y página 1.
- **Enfoque:** consumir únicamente `accepted.scene` de cada detalle recibido; no truncar ni reutilizar la escena de otra página.
- **Pruebas automáticas:** escena completa en página 2 y escenas distintas para portada y página 1; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** abrir una página 2 aceptada y una portada aceptada con escena distinta de la página 1 y comprobar el texto completo en cada una.

### TASK-020 Subida de imagen desde el panel de portada y páginas

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): selector de fichero, acción de subida, mensaje de rechazo y recarga de la vista. `app/src/components/StoryPagesPanel.spec.ts` (modificar): visibilidad y desenlaces de subida.
- **Enfoque:** ofrecer la acción sólo cuando el detalle tiene contenido aceptado; tras éxito recargar el estado para mostrar la imagen y, ante fallo, mantener el estado ya cargado. Reutilizar la descarga de imagen aceptada y sus URLs temporales, liberándolas según el ciclo ya existente del panel.
- **Pruebas automáticas:** opción visible en portada aceptada, ausente en portada o página pendiente, imagen nueva tras éxito y error sin perder imagen previa; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** subir JPEG grande en portada, PNG pequeño en página 3, sustituirlo y rechazar un texto, verificando persistencia tras reabrir el cuento.

### TASK-021 Revisión de la portada en el panel y bloqueo de la página 1

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): flujo de página 0 para generar, aceptar, pedir hint, informar fallos y bloquear la generación de página 1. `app/src/components/StoryPagesPanel.spec.ts` (modificar): eventos, acciones y regresiones de portada. `app/src/stores/websocket.ts` y `app/src/stores/websocket.spec.ts` (modificar sólo si el tipado actual no acepta los eventos existentes con `pageNumber: 0`).
- **Enfoque:** reconocer los eventos `VERSION_PRESENTED`, `HINT_REJECTED` y `FAILED` de portada como señales de recarga, igual que en páginas interiores. No habilitar página 1 mientras la portada no esté aceptada; si la aprobación no inició la portada, mostrar la acción manual. Tras reaprobación, recargar y reflejar que sólo la imagen de portada desapareció.
- **Pruebas automáticas:** versión pendiente de portada, hint rechazado, fallo, bloqueo de página 1, aviso de cuento legacy y aprobación sin inicio automático; regresión de que el cambio de portada preserva páginas interiores; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** aprobar un guion, revisar la portada con hint, aceptarla y generar después la página 1; reaprobando portada con imágenes, confirmar conservación de interiores y pérdida de la imagen de portada; abrir un cuento anterior y comprobar el aviso.

### TASK-031 Cliente REST de las exportaciones

- **Ficheros:** `app/src/api/storyExports.ts` (nuevo, propuesta): tipos de éxito/rechazo, operaciones Nubi Game y agente visual y lectura de zip. `app/src/api/storyExports.spec.ts` (nuevo, propuesta): respuestas binarias y rechazos acumulados. `app/src/api/httpClient.ts` (modificar sólo si es necesario para distinguir por estado/estructura un blob de un rechazo JSON).
- **Enfoque:** encapsular las dos rutas que entregue server y convertir la respuesta en una unión discriminada de descarga o rechazo con todas las incidencias. No deducir disponibilidad desde datos locales ni descartar incidencias; el contrato de rutas, nombres y cuerpo queda sujeto a la entrega REST de TASK-025 y TASK-030.
- **Pruebas automáticas:** zip Nubi y visual, y rechazo con varias faltas asociadas a portada o página; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica aislada; se cubre en TASK-032.

### TASK-032 Acciones de exportación en la interfaz

- **Ficheros:** `app/src/views/StorytellerView.vue` (modificar): reemplazar la descarga heredada por acciones Nubi Game y agente visual, descarga del zip y presentación completa de rechazos. `app/src/views/StorytellerView.spec.ts` (nuevo, propuesta): acciones únicas, errores y descarga. `app/src/api/storyteller.ts` (modificar): retirar el uso de `downloadStory` heredado si queda sin consumidores.
- **Enfoque:** ubicar las dos acciones en las opciones existentes de cada cuento; eliminar la única acción antigua para Nubi y no conservar dos alternativas. Generar y revocar la URL del blob sólo en éxito; presentar todas las faltas recibidas con su contexto, sin iniciar descarga ante un rechazo.
- **Pruebas automáticas:** una acción Nubi y una visual, rechazo múltiple visible y click de descarga ante zip; `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** ejecutar los siete pasos de TASK-032: zip Nubi con imágenes originales y audio (incluida su extensión según formato real), preservación del original, falta de imagen, zip visual y su contrato, y rechazo de cuento legacy.

## Orden de implementación

1. TASK-016 (habilitada por: TASK-004, TASK-005 y TASK-011 de server entregadas con sus contratos REST).
2. TASK-017 (habilitada por: TASK-016 integrada).
3. TASK-018 (habilitada por: TASK-008 y TASK-014 de server entregadas con hint y multipart operativos).
4. TASK-019 (habilitada por: TASK-018 integrada y TASK-015 de server entregada con escena completa).
5. TASK-020 (habilitada por: TASK-018 integrada).
6. TASK-021 (habilitada por: TASK-018 integrada y TASK-009, TASK-010 y TASK-011 de server entregadas).
7. TASK-031 (habilitada por: TASK-025 y TASK-030 de server entregadas con éxito binario y rechazo estructurado).
8. TASK-032 (habilitada por: TASK-031 integrada).

## Riesgos y dudas técnicas

- Las rutas, códigos HTTP y esquema del rechazo de exportación no están aún formalizados. Mitigación: TASK-031 se integra contra la interfaz REST implementada y probada por server; concentrar su adaptación en `storyExports.ts`.
- `httpClient` convierte las respuestas de error antes de devolver un blob. Mitigación: conservar ese comportamiento para los rechazos JSON y ampliar sólo lo imprescindible para que el cliente de exportación pueda distinguir éxito de error sin perder la lista de faltas.
- Un evento WebSocket puede perderse o llegar mientras el panel no está montado. Mitigación: usarlo sólo para disparar la recarga REST y mantener la recarga por `connectionId`.
- Las acciones de exportación no deben prometer localmente que el cuento es exportable. Mitigación: mostrar la lista autoritativa de server completa y no prevalidar condiciones en la vista.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
