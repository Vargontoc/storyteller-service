# Sprint 04: agent-writer - app

Estado: aprobado

## Alcance

- Tareas: TASK-029, TASK-030, TASK-031, TASK-032, TASK-033, TASK-034, TASK-035 (de `specs/agent-writer/tasks.md`).
- Dependencias internas: TASK-031 depende de TASK-029; TASK-032 de TASK-031; TASK-033 de TASK-032; TASK-034 de TASK-030 y TASK-033; TASK-035 de TASK-034.
- Dependencias externas: TASK-029 (`server`, TASK-022): API REST implementada y probada para leer el estado de portada/páginas y solicitar generación, hint, regeneración, selección de versión, aceptación confirmada, descarte e imagen aceptada. La entrega debe usar la base `/api/v1/storyteller/{storyId}` y las operaciones propuestas `GET /pages`, `POST /cover/generate`, `POST /pages/{number}/generate`, `POST /hint`, `POST /regenerate`, `POST /versions/{versionId}/select`, `POST /accept`, `POST /proposals/discard` y `GET /pages/{number}/image`, o comunicar sus equivalencias definitivas. TASK-030 (`server`, TASK-009): canal `/storyteller` entregado con eventos de páginas tipados, `storyId`, `pageNumber` (0 para portada), `type` (`STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED`, `FAILED`) y `reason` cuando corresponda. TASK-031 a TASK-035 (`server`, TASK-022): DTOs definitivos que distingan contenido aceptado, versiones/propuestas, versión vigente, aviso de longitud, acciones habilitadas, número de páginas a eliminar y disponibilidad de imagen; los rechazos deben devolver un motivo comunicable.

## Origen SDD

- Requirements: 5
- Design: 2
- Tasks: 1

## Enfoque técnico

La SPA añadirá un cliente `storyPages` y un único `StoryPagesPanel` para cuentos con escaleta; el servidor seguirá siendo la fuente de verdad del estado, histórico, vigencia, disponibilidad de acciones y descarte en cascada. Las rutas indicadas en el alcance son el hand-off propuesto por `server`: se ajustarán sólo si éste entrega una interfaz equivalente, implementada y probada.

El panel separará inequívocamente contenido aceptado y propuestas: únicamente el contenido aceptado puede obtener y mostrar imagen; las versiones del histórico nunca. Reutilizará `ApiError`, `ConfirmModal` y los patrones de carga/recarga existentes, sin introducir edición manual de texto ni escena ni inferir reglas de secuencialidad localmente.

`websocket-store` tratará los eventos de páginas como señales tipadas de sincronización y conservará sin cambios la decodificación de retratos y notificaciones. Tras reconectar, el panel recargará por REST el estado del cuento abierto; los eventos de otro cuento no modificarán el panel actual.

Para cubrir las pruebas exigidas por las tareas, se incorporará soporte de pruebas unitarias de SPA con Vitest y Vue Test Utils, manteniendo `npm run build` como comprobación de tipos y empaquetado.

## Cambios por tarea

### TASK-029 Cliente REST de portada y páginas

- **Ficheros:** `app/src/api/storyPages.ts` (nuevo, propuesta): tipos del estado de portada/página, contenido aceptado, versión, avisos, acciones y respuestas/rechazos; consultas y comandos REST; `app/src/api/httpClient.ts` (reutilizar, modificar sólo si hace falta soportar respuesta binaria de imagen o un cuerpo vacío); `app/package.json` y configuración de Vitest del módulo (modificar/nuevo, propuesta): comando y dependencias de prueba; `app/src/api/storyPages.spec.ts` (nuevo, propuesta): pruebas del cliente con transporte simulado.
- **Enfoque:** encapsular en un cliente las operaciones de lectura, generación de portada/página, hint, regeneración sin hint, selección de vigente, aceptación con `confirmCascade`, descarte de propuestas e imagen. Modelar el resultado de aceptación que solicita confirmación sin convertirlo en una mutación local; devolver el motivo autoritativo de todos los rechazos. La imagen se solicitará únicamente para el contenido aceptado y se representará como URL/blob apto para el componente, nunca dentro de `PageVersion`.
- **Pruebas automáticas:** con Vitest, comprobar petición, método, ruta, payload y respuesta tipada de cada operación; comprobar que una aceptación sin `confirmCascade` conserva/expone el número de páginas que el servidor advierte que eliminaría; comprobar los rechazos por generación en curso, predecesor sin aceptar, hint vacío, aviso de frases y cuento completo. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica de forma aislada; se cubre desde TASK-031 a TASK-033.

### TASK-030 Eventos de generación de portada y página en el store WebSocket

- **Ficheros:** `app/src/stores/websocket.ts` (modificar): añadir el tipo `StoryPageGenerationEvent`, el último evento de páginas y la discriminación segura de mensajes; `app/src/stores/websocket.spec.ts` (nuevo, propuesta): prueba de decodificación y reconexión sin socket real.
- **Enfoque:** reconocer eventos de páginas por `storyId`, `pageNumber`, `type` y, para rechazo/fallo, `reason`; asignar un identificador local como en retratos para que los consumidores reaccionen a eventos consecutivos iguales. `STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED` y `FAILED` se expondrán sin reutilizar semánticas de `SUCCESS`/`ERROR` de retratos. Mantener intacta la notificación genérica, los retratos y el incremento de `connectionId` al abrir una conexión.
- **Pruebas automáticas:** simular `VERSION_PRESENTED` y verificar cuento/página disponibles; simular `HINT_REJECTED` y verificar motivo; simular un evento de retratos y comprobar que conserva su tratamiento vigente; comprobar que una nueva apertura incrementa la señal de reconexión. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** no aplica de forma aislada; se cubre en TASK-034.

### TASK-031 Panel de páginas: vista y acciones básicas

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (nuevo, propuesta): vista de portada y páginas de cuentos con escaleta, estado, acciones y errores; `app/src/api/storyPages.ts` (modificar): consumir los tipos/operaciones implementados; `app/src/components/StoryPagesPanel.spec.ts` (nuevo, propuesta): casos de renderizado e interacción básicos.
- **Enfoque:** cargar el estado REST al montar y después de cada mutación confirmada. Mostrar portada con título y escena, y página con texto y escena; cargar/mostrar imagen sólo para el contenido aceptado que la declare. Ofrecer generar portada o la siguiente página únicamente cuando el servidor lo indique; aceptar no generará la siguiente. El hint será obligatorio tras recortar espacios y no se enviará si está vacío; errores de servidor se mostrarán con su motivo. No se montarán controles editables de texto ni de escena.
- **Pruebas automáticas:** montar el panel con dobles del cliente y comprobar: aceptada con imagen; portada aceptada con imagen; aceptada sin imagen; pendiente con acciones; aceptada sin propuestas; siguiente generación y cuento completo; ausencia de campos editables; hint vacío y rechazo de servidor. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** abrir un cuento con guion aprobado, pedir portada y comprobar la propuesta pendiente; aceptarla y comprobar que se habilita generar página 1 sin iniciarla; confirmar que texto y escena no son editables.

### TASK-032 Panel de páginas: histórico, avisos, vigente y regeneración sin hint

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): representación del histórico, versión vigente y acciones condicionadas por aviso; `app/src/components/StoryPagesPanel.spec.ts` (modificar): cobertura de histórico y avisos.
- **Enfoque:** para portada o página pendiente, renderizar todas las versiones recibidas, destacar sólo la vigente y permitir seleccionar cualquiera de las restantes. Mostrar su aviso junto al contenido de cada versión sin solicitar imágenes. Si la vigente tiene aviso de frases, ocultar aceptar y ofrecer regenerar sin hint; con otros avisos conservar aceptar y cambio con hint. La decisión procede del aviso/acciones del servidor, no de recalcular longitud en cliente.
- **Pruebas automáticas:** verificar histórico completo de varias versiones y de 11 versiones; selección de otra vigente; aviso de exceso de palabras con aceptar y hint; aviso de frases con hint/regeneración y sin aceptar; ausencia de regeneración si el aviso está en una versión no vigente; ausencia de imágenes en todo histórico. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** con tres versiones con aviso, marcar la primera como vigente y comprobar contenido/aviso; con aviso de frases en la vigente, comprobar que no se puede aceptar y sí regenerar sin hint.

### TASK-033 Panel de páginas: reapertura, confirmación del descarte en cascada y descarte de propuestas

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): zona de contenido aceptado separada, propuestas de reapertura, descarte y confirmación; `app/src/components/ConfirmModal.vue` (reutilizar, modificar sólo si necesita expresar el número entregado por servidor); `app/src/components/StoryPagesPanel.spec.ts` (modificar): escenarios de reapertura y cascada.
- **Enfoque:** en una reapertura, mantener el contenido aceptado separado, con su imagen si existe, y mostrar las propuestas sin imagen. Al pedir aceptación, usar primero la respuesta/advertencia de `storyPages` para abrir `ConfirmModal` si hay páginas posteriores; sólo la confirmación enviará `confirmCascade: true`. Cancelar no realizará la mutación. El descarte eliminará únicamente propuestas y recargará el estado autoritativo; la versión aceptada no podrá marcarse como vigente.
- **Pruebas automáticas:** comprobar separación de aceptada/propuestas y su imagen; advertencia de dos páginas posteriores para página 2; cancelación sin petición de aceptación; confirmación que recarga el estado; portada sin confirmación; selección de vigente sólo entre propuestas. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** con páginas 1 a 4 aceptadas, reabrir la 2 y cancelar el aviso de dos descartes, comprobando que 3 y 4 permanecen; confirmar y comprobar que se ofrece generar la 3; reabrir otra y descartar propuestas, verificando que vuelve a verse la aceptada.

### TASK-034 Integración: recarga del panel por eventos de generación

- **Ficheros:** `app/src/components/StoryPagesPanel.vue` (modificar): suscripción filtrada al evento de páginas y a `connectionId`, indicador de operación y recarga; `app/src/stores/websocket.ts` (modificar sólo si se requiere una señal de evento/reconexión adicional); `app/src/components/StoryPagesPanel.spec.ts` (modificar): pruebas de integración panel-store.
- **Enfoque:** mientras llega `STARTED` del cuento y página abiertos, mostrar generación en curso sin suplantar el estado del servidor. Ante `VERSION_PRESENTED`, recargar exactamente portada (`pageNumber: 0`) o página afectada; ante `HINT_REJECTED`, conservar contenido y presentar `reason`; ante `FAILED`, mostrar error y liberar la interacción cuando la recarga confirme el estado. Ignorar eventos de otros cuentos. Una reconexión detectada por el cambio de `connectionId` recargará el panel completo, cubriendo reinicios del servidor o eventos perdidos.
- **Pruebas automáticas:** evento presentado del cuento abierto recarga la página afectada; evento de otro cuento no recarga; hint rechazado presenta motivo y no altera contenido; fallo permite reintento tras sincronizar; reconexión ejecuta recarga. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** pedir una página y esperar su aparición sin recargar; detener Ollama y comprobar error/reintento; reiniciar servidor durante generación y comprobar, al reconectar, estado real y posibilidad de pedir de nuevo.

### TASK-035 Modal del cuento: pestaña de páginas según escaleta

- **Ficheros:** `app/src/components/StoryModal.vue` (modificar): montar `StoryPagesPanel` para la pestaña de páginas de cuentos con escaleta y conservar `PagesContent` para cuentos históricos; `app/src/components/StoryModal.spec.ts` (nuevo, propuesta): selección de componente por `scriptVersion`.
- **Enfoque:** reutilizar el marcador `scriptVersion` ya cargado por el modal como única decisión de bifurcación. Para cuentos con escaleta, la navegación a páginas usará el panel nuevo y sus recargas; los cuentos sin escaleta mantienen íntegramente la vista anterior, sus acciones y rutas. No deducir el tipo de cuento por la presencia de páginas ni por campos opcionales.
- **Pruebas automáticas:** comprobar que un cuento con escaleta monta `StoryPagesPanel` y que uno sin escaleta mantiene `PagesContent`. Ejecutar `npm run test -- --run` y `npm run build` desde `app/`.
- **Prueba manual:** abrir un cuento con escaleta y uno antiguo; comprobar respectivamente panel nuevo y vista de páginas previa.

## Orden de implementación

1. TASK-029 (habilitada por: TASK-022 de `server` entregada con operaciones REST, DTOs y rechazos de portada/páginas).
2. TASK-030 (habilitada por: TASK-009 de `server` entregada con el evento WebSocket tipado en `/storyteller`).
3. TASK-031 (habilitada por: TASK-029 integrada y datos REST que expresen estado, contenido aceptado, propuestas y acciones).
4. TASK-032 (habilitada por: TASK-031 integrada y DTOs de histórico, vigente y avisos entregados por `server`).
5. TASK-033 (habilitada por: TASK-032 integrada y aceptación/descarte con advertencia de cascada disponible en TASK-022 de `server`).
6. TASK-034 (habilitada por: TASK-030 y TASK-033 integradas; el WebSocket debe publicar todos los desenlaces de generación).
7. TASK-035 (habilitada por: TASK-034 integrada y el marcador `scriptVersion` existente en el DTO de guion).

## Riesgos y dudas técnicas

- El contrato OpenAPI de estas operaciones se formaliza después en TASK-036 a TASK-040. Mitigación: implementar contra el hand-off REST/WebSocket probado de `server`, manteniendo las rutas de este sprint como propuesta y centralizadas en `storyPages.ts`.
- El formato vigente de eventos de retrato usa otros campos y estados. Mitigación: discriminar eventos de páginas por la presencia y tipos de `storyId`/`pageNumber`, sin alterar la ruta de retratos.
- La confirmación de cascada es una interacción en dos pasos y el estado puede cambiar entre ambos. Mitigación: el servidor calcula y valida definitivamente el descarte; la SPA no borra páginas de forma optimista y recarga tras la respuesta.
- La imagen importada puede no existir y sólo pertenece al contenido aceptado. Mitigación: tratarla como recurso opcional y no solicitarla/renderizarla en propuestas ni en versiones pendientes.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
