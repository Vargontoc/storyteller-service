# Sprint 03: agent-director - app

Estado: aprobado

## Alcance

- Tareas: TASK-010, TASK-011, TASK-012, TASK-013 (de `specs/agent-director/tasks.md`).
- Dependencias internas: TASK-012 depende de TASK-011. TASK-013 depende de TASK-010, TASK-011 y TASK-012.
- Dependencias externas: TASK-010 (`server`, TASK-005): creación de cuento implementada y probada con catálogo de protagonistas aptos, elección aleatoria y rechazo cuando no exista retrato aprobado. TASK-011 (`server`, TASK-005, TASK-006 y TASK-007): API REST implementada y probada para consultar la escaleta/versionado y estado del guion, aprobarlo, solicitar y resolver una revisión global, y validar/aplicar o cancelar la edición de un beat con su aviso de páginas invalidadas. TASK-012 (`server`, TASK-005, TASK-006 y TASK-007): los DTOs definitivos deben distinguir cuentos con versión de escaleta de cuentos antiguos y exponer el estado del guion. TASK-013 (`server`, TASK-005 a TASK-009): entrega integrada y probada de creación, revisión/edición de escaleta, puerta de aprobación, eliminación de páginas y generación que consume el beat aprobado. Los contratos OpenAPI TASK-014 a TASK-018 se formalizarán posteriormente y no bloquean la implementación de la SPA.

## Origen SDD

- Requirements: 2
- Design: 1
- Tasks: 1

## Enfoque técnico

La SPA conservará el patrón vigente de componentes Vue, clientes tipados en `src/api/`, `ApiError` para los rechazos del servidor y `ConfirmModal` para acciones irreversibles. El servidor será la fuente de verdad de protagonistas aptos, estado/versionado del guion, diferencias de revisión, validación de edad e invalidación de páginas; la interfaz no inferirá dichos estados localmente.

`TopicSelectionModal.vue` pasará de protagonista opcional a selección obligatoria entre personajes con retrato `APPROVED`, con la alternativa explícita de selección aleatoria. `StoryScriptPanel.vue` (nuevo) sustituirá la interacción de revisión de guion genérica para los cuentos con escaleta: mostrará el contrato completo, gestionará aprobación, hint global, propuesta candidata/diff y edición de beats. Los cuentos históricos conservarán `StoryContent.vue` y su representación previa.

Las rutas, payloads y tipos definitivos se ajustarán al hand-off de `server`; hasta entonces se planifican las responsabilidades del cliente sin fijar endpoints ni formatos de error. No se modifican el director, el aprovisionamiento ni la documentación OpenAPI desde este módulo.

## Cambios por tarea

### TASK-010 Modificar la pantalla de creación de cuento para el protagonista obligatorio

- **Ficheros:** `app/src/components/TopicSelectionModal.vue` (modificar): listar exclusivamente protagonistas aptos, exigir una selección concreta o aleatoria, y bloquear/explicar la creación si no hay ninguno; `app/src/api/characters.ts` (modificar): adaptar el tipo/consulta de disponibilidad al DTO definitivo de `server`; `app/src/api/generations.ts` (modificar): adaptar el comando de creación para expresar elección concreta o aleatoria sin enviar un protagonista opcional ambiguo.
- **Enfoque:** recargar la disponibilidad al abrir el modal y ante los eventos/reconexiones de retratos ya soportados. Representar una opción visible de elección aleatoria distinta de cada identificador de catálogo; solo habilitar crear cuando tema, tamaño y una de ambas decisiones estén seleccionados. Si la lista apta queda vacía, mostrar que es necesario crear y aprobar un protagonista y no enviar solicitud. Un error definitivo del servidor se mostrará sin asumir que el estado local sigue siendo válido.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** abrir con varios retratos aprobados y verificar listado más opción aleatoria; crear seleccionando uno concreto y comprobar el protagonista del cuento; repetir con aleatorio y comprobar que el protagonista tiene retrato aprobado; abrir sin aptos y comprobar aviso y acción de crear deshabilitada.

### TASK-011 Construir la vista de guion con escaleta, aprobación y edición de beats

- **Ficheros:** `app/src/components/StoryScriptPanel.vue` (nuevo, propuesta): representación del guion contractual, acciones y estados de carga/error; `app/src/api/storyteller.ts` (modificar): tipos y consulta de cuento/guion con versión, estado, reparto y beats; `app/src/api/reviews.ts` (modificar): tipos y operaciones REST de revisión global, candidata, beats cambiados y aceptación/rechazo; `app/src/api/storyScript.ts` (nuevo, propuesta): cliente tipado de aprobación de guion y edición directa de beats, separado de las revisiones genéricas si el hand-off mantiene superficies distintas; `app/src/components/ConfirmModal.vue` (reutilizar, sin modificar salvo que el contenido estructurado del aviso lo requiera).
- **Enfoque:** renderizar título, sinopsis, reparto ligero (nombre y función) y beats ordenados por página con sus cinco campos. Aprobar actualizará el estado desde la respuesta del servidor. El hint abrirá la propuesta completa y diferenciará los beats marcados como modificados por el servidor; si no hay ninguno, mostrará la indicación explícita. Para un beat seleccionado, abrir edición del texto, mostrar el rechazo de edad sin alterar la vista vigente y refrescar el guion tras guardar. Si el servidor comunica páginas afectadas, solicitar confirmación antes de aplicar; cancelar no realizará la operación confirmada ni actualizará el estado local. La resolución de revisión y la edición confirmada recargarán guion, revisión y estado de páginas desde REST.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** abrir un guion pendiente y comprobar todos los datos; aprobar y verificar estado; enviar un hint con y sin cambios y verificar candidata/diff o aviso; editar un beat válido y uno rechazado verificando respectivamente refresco y motivo; con páginas generadas, confirmar y cancelar el aviso de invalidación, comprobando en ambos casos el resultado del servidor.

### TASK-012 Orquestar las pestañas del cuento según el estado del guion

- **Ficheros:** `app/src/components/StoryModal.vue` (modificar): seleccionar el panel de escaleta para cuentos nuevos, conservar el panel legado para antiguos y restringir las pestañas/generación por estado de guion; `app/src/components/content/StoryContent.vue` (modificar): mantenerlo como representación histórica y retirar de los cuentos con escaleta las acciones de revisión genérica que pasan a `StoryScriptPanel`; `app/src/api/storyteller.ts` (modificar): exponer al modal el marcador de versión y el estado entregados por servidor.
- **Enfoque:** decidir la representación únicamente mediante el marcador de versión del guion: sin versión de escaleta, conservar exactamente la UI previa de guion y personajes; con ella, montar `StoryScriptPanel`. Mientras el servidor informe estado pendiente, dirigir la apertura al guion y deshabilitar/ocultar el acceso operativo a portada y páginas, sin sustituir la validación backend. Tras la aprobación o una modificación que devuelve el guion a pendiente, recargar el cuento, pestañas y páginas para que la navegación coincida con el estado persistido.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** abrir un cuento nuevo pendiente y comprobar panel de escaleta y bloqueo de escritura; abrir un cuento histórico y comprobar UI previa sin escaleta; aprobar el nuevo y verificar que se habilita la generación de páginas; editar un beat y confirmar que se vuelve a bloquear hasta aprobar de nuevo.

### TASK-013 Integrar el flujo completo de creación, aprobación y escritura

- **Ficheros:** `app/src/components/TopicSelectionModal.vue` (modificar): conservar la selección y mostrar errores finales de creación; `app/src/components/StoryScriptPanel.vue` (modificar): notificar los cambios confirmados al modal padre; `app/src/components/StoryModal.vue` (modificar): recargar coherentemente cuento, páginas y pestañas tras creación, aprobación, revisión, edición e intento de generación; `app/src/api/generations.ts`, `app/src/api/storyteller.ts`, `app/src/api/reviews.ts` y `app/src/api/storyScript.ts` (modificar): ajustar el flujo a los DTOs/errores definitivos de los casos de uso del servidor; `app/src/views/StorytellerView.vue` (modificar): refrescar la fila y abrir el cuento creado en el estado que entregue el servidor.
- **Enfoque:** recorrer el flujo real sin duplicar reglas de negocio: creación con protagonista, visualización de escaleta, bloqueo visible de portada/página antes de aprobar, aprobación, generación, revisión global y edición. Cada mutación refrescará la representación desde las respuestas/consultas REST; los rechazos de aprobación, edad o generación se comunicarán al usuario y no se convertirán en estados locales alternativos. La generación de página seguirá delegada al servidor, que garantiza la entrega del beat aprobado al escritor.
- **Pruebas automáticas:** no aplica según `tasks.md`; ejecutar `npm run build` desde `app/` para comprobar tipos y compilación.
- **Prueba manual:** crear con protagonista y comprobar escaleta/reparto; intentar portada pendiente y verificar rechazo; aprobar, generar portada y página interior y comprobar la narración asociada al beat; aceptar una revisión global y verificar que la escaleta queda pendiente; editar un beat y verificar que se bloquea escribir hasta una nueva aprobación.

## Orden de implementación

1. TASK-010 (habilitada por: TASK-005 de `server` entregada con protagonistas aptos, elección aleatoria y rechazo de catálogo vacío).
2. TASK-011 (habilitada por: TASK-005, TASK-006 y TASK-007 de `server` entregadas con DTOs y operaciones REST finales de guion, revisión y edición).
3. TASK-012 (habilitada por: TASK-011 integrada y el marcador de versión/estado de guion de TASK-005 a TASK-007 de `server` disponible).
4. TASK-013 (habilitada por: TASK-010, TASK-011 y TASK-012 integradas, y TASK-005 a TASK-009 de `server` entregadas para el flujo extremo a extremo). Las tareas de contrato TASK-014 a TASK-018 no condicionan este orden.

## Riesgos y dudas técnicas

- Las rutas, comandos y DTOs REST de escaleta aún se formalizarán después mediante TASK-014 a TASK-018. Mitigación: adaptar los clientes solo al hand-off implementado y probado de `server`; no inventar contratos desde `app`.
- La SPA no dispone de pruebas de componentes. Mitigación: `npm run build` y las pruebas manuales por tarea, incluyendo los dos desenlaces de invalidación.
- La coexistencia de cuento histórico y nuevo puede llevar a interpretar datos parciales como escaleta. Mitigación: usar exclusivamente el marcador de versión proporcionado por el servidor, no la mera presencia de campos opcionales.
- El estado puede cambiar entre carga e interacción. Mitigación: mostrar el error autoritativo de servidor y recargar cuento, revisión y páginas después de cada mutación.

## Validación

- Suite del módulo: correcta. Origen: confirmación del usuario en la sesión del desarrollador-router. Alcance: `npm run build` (tipos y compilación) desde `app/` tras TASK-011 (incluye TASK-010); el módulo no tiene pruebas automáticas.
- Integración: pendiente
- Validación manual: correcta. Origen: confirmación del usuario. Alcance: TASK-010 (selección de protagonista concreto o aleatorio con varios aptos, y aviso con catálogo sin aptos). Pendiente: la prueba manual de TASK-011; el usuario la hará más tarde, tras montar el panel en TASK-012.
