# Sprint 04: agent-writer - server

Estado: aprobado

## Alcance

- Tareas: TASK-001–005, TASK-007–028 y TASK-036–043 (de `specs/agent-writer/tasks.md`).
- Dependencias internas: las indicadas en el orden; TASK-006 queda fuera del módulo.
- Dependencias externas: TASK-005 requiere retirada posterior coordinada por `agents`; TASK-036–043 son entrega final al documentador, tras la implementación. No hay bloqueantes externos para implementar.

## Origen SDD

- Requirements: 5
- Design: 2
- Tasks: 1

## Enfoque técnico

Se mantiene la arquitectura hexagonal existente: puertos de aplicación y persistencia, adaptadores JPA/Ollama/WebSocket y `StorytellerController` para REST. Se propone una puerta singleton de proceso, atómica y liberada en `finally`, para toda llamada a modelo o ComfyUI; no sustituye los bloqueos actuales de retratos. Las páginas con escaleta conservan separado el contenido aceptado y sus propuestas versionadas, por lo que los cuentos antiguos siguen usando el flujo y `StoryPageReview` actuales. Las generaciones se ejecutan asíncronamente tras conceder turno: REST confirma el inicio y `/storyteller` anuncia el resultado. Toda prueba se añade junto al componente y se ejecuta con `mvn -f api/pom.xml test`.

## Cambios por tarea

### TASK-001 Puerta de generación única en memoria

- **Ficheros:** `application/service/GenerationGate.java`, `application/ports/.../GenerationGatePort.java` y pruebas (nuevos, propuesta).
- **Enfoque:** turno atómico con tipo y `storyId` opcional; exponer adquirir/liberar y consulta de generación del cuento, empezando libre tras arranque.
- **Pruebas automáticas:** exclusión entre tipos, liberación por éxito/error, consulta por cuento y carrera de dos solicitudes.
- **Prueba manual:** no aplica.

### TASK-002 Validador de longitud del texto narrativo

- **Ficheros:** `domain/service/PageLengthValidator.java`, valor `PageLengthWarning` y pruebas (nuevos, propuesta).
- **Enfoque:** contar palabras y segmentos cerrados; normalizar `...`, `¿?` y `¡!` para que no sumen cierres duplicados; clasificar ninguno/palabras/frases/ambos.
- **Pruebas automáticas:** límites 66/67, 0/1/4/5 frases, diálogos, comillas y signos dobles.
- **Prueba manual:** no aplica.

### TASK-003 Histórico de versiones de portada y página

- **Ficheros:** migración Liquibase siguiente a `023`; `PageVersion`, entidad/repositorio/mapper JPA y `StoryPageVersionRepository` (nuevos, propuesta).
- **Enfoque:** versiones sin asset, ordenadas, con única vigente por página y operaciones transaccionales de añadir, elegir y borrar lote.
- **Pruebas automáticas:** alta, cambio de vigente, cuatro avisos, borrado aislado y 11 versiones.
- **Prueba manual:** no aplica.

### TASK-004 Estado, contenido aceptado e imagen importada de la página

- **Ficheros:** siguiente migración; `StoryPage`, `StoryPageJpaEntity`, mapper y repositorio existentes (modificar).
- **Enfoque:** añadir estado, beat, contenido aceptado y referencia nullable de imagen solo para cuentos con escaleta, conservando el mapeo heredado.
- **Pruebas automáticas:** pendiente, portada, aceptada con imagen preparada, desasociación y lectura de cuento antiguo.
- **Prueba manual:** no aplica.

### TASK-005 Modelfile del escritor y aprovisionamiento

- **Ficheros:** `resources/prompts/writer.modelfile`, `infrastructure/config/WriterModelProvisioner.java` y pruebas (nuevos, propuesta).
- **Enfoque:** reproducir el patrón de `DirectorModelProvisioner`, con `app.agents.writer-provisioning.enabled`, modelo de `Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL` e instrucciones de beat/edad/longitud.
- **Pruebas automáticas:** recurso no vacío, crea modelo ausente y omite el presente.
- **Prueba manual:** arrancar Ollama sin/con modelo y verificar una única creación.

### TASK-007 Adaptador del escritor

- **Ficheros:** `application/ports/out/external/WriterAgentPort.java`, `infrastructure/adapters/out/external/SpringAiWriterAgent.java`, prompts y pruebas (nuevos, propuesta); `StoryPageService.java` (modificar).
- **Enfoque:** separar portada, página y hint; validar resultado obligatorio y traducir rechazo motivado/fallo sin mezclar prompts con reglas de negocio.
- **Pruebas automáticas:** parámetros de contexto, respuesta completa, hint rechazado, campos vacíos y error Ollama.
- **Prueba manual:** beat con secundarios permitidos y hint fuera de beat rechazado.

### TASK-008 Notificador de eventos de generación

- **Ficheros:** `StoryPageNotifier.java`, puerto/evento `StoryPageGenerationEvent` y pruebas (nuevos, propuesta).
- **Enfoque:** publicar `STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED` y `FAILED` con `storyId`, `pageNumber` (0 portada) y `reason` solo cuando aplique.
- **Pruebas automáticas:** serialización/entrega de los cuatro eventos.
- **Prueba manual:** no aplica.

### TASK-009 Difusión WebSocket

- **Ficheros:** `WebsocketUseCase.java`, `WebsocketAdapter.java` y pruebas WebSocket (modificar).
- **Enfoque:** añadir la rama de evento de página a la difusión existente en `/storyteller`, sin cambiar notificaciones ni retratos.
- **Pruebas automáticas:** suscriptor recibe evento nuevo y el formato de retratos permanece.
- **Prueba manual:** portada produce `STARTED` y `VERSION_PRESENTED`.

### TASK-010 Flujo antiguo a través de la puerta

- **Ficheros:** `StoryPageService.java` y pruebas (modificar).
- **Enfoque:** bifurcar explícitamente por `hasOutline`; conservar deducción y `StoryPageReview` heredados, envolviendo su generación en la puerta.
- **Pruebas automáticas:** compatibilidad antigua, revisión pendiente, ocupación/fallo/liberación y exclusión de flujo antiguo en escaleta.
- **Prueba manual:** generar cuento antiguo con y sin retratos en curso.

### TASK-011 Generar escena de portada

- **Ficheros:** puertos/comandos/resultados REST de páginas (nuevos, propuesta), `StoryPageService.java` y controlador (modificar).
- **Enfoque:** validar guion aceptado y ausencia de portada, conceder turno y responder inicio; tarea asíncrona crea portada pendiente/versionada sin validar longitud y notifica resultado.
- **Pruebas automáticas:** éxito, guion/portada inválidos, puerta ocupada, fallo, liberación y eventos.
- **Prueba manual:** solicitar y consultar portada pendiente sin imagen.

### TASK-012 Aceptar portada o página pendiente

- **Ficheros:** `StoryPageService.java`, puertos REST y pruebas (modificar/nuevos).
- **Enfoque:** promover versión vigente a contenido aceptado sin imagen, borrar histórico y bloquear solo por aviso de frases o generación del mismo cuento.
- **Pruebas automáticas:** portada/página, aviso de palabras frente a frases, generación propia/ajena y ausencia de generación siguiente.
- **Prueba manual:** aceptar portada y verificar estado e histórico.

### TASK-013 Generar página N desde beat

- **Ficheros:** `StoryPageService.java`, Writer port y REST (modificar).
- **Enfoque:** exigir portada y anteriores aceptadas, validar N siguiente, construir contexto desde escaleta, snapshot y textos aceptados; persistir pendiente/versionada asíncronamente.
- **Pruebas automáticas:** secuencia, N inválida, contexto exacto, snapshot inmutable y puerta ocupada.
- **Prueba manual:** página 1 y rechazo por página anterior pendiente.

### TASK-014 Bucle de longitud

- **Ficheros:** `StoryPageService.java` y pruebas (modificar).
- **Enfoque:** máximo tres intentos bajo el mismo turno; guardar solo primera válida o las tres avisadas con última vigente; fallo intermedio revierte toda la solicitud.
- **Pruebas automáticas:** éxito primero/segundo, tres avisos, invisibilidad durante proceso, fallo segundo y puerta retenida.
- **Prueba manual:** escritor de prueba fuera de límite genera tres versiones.

### TASK-015 Cambio con hint pendiente

- **Ficheros:** `StoryPageService.java`, REST/comandos y pruebas (modificar/nuevos).
- **Enfoque:** rechazar hint vacío; usar vigente como base, conservarla ante rechazo/fallo y aplicar bucle salvo portada.
- **Pruebas automáticas:** base vigente, múltiples hints, portada, vacío, rechazo, fallo, 11 versiones y puerta ocupada.
- **Prueba manual:** hint válido y hint fuera de beat.

### TASK-016 Regenerar sin hint

- **Ficheros:** `StoryPageService.java`, REST y pruebas (modificar/nuevos).
- **Enfoque:** habilitar exclusivamente si la vigente avisa de número de frases; reutilizar generación por beat y bucle.
- **Pruebas automáticas:** elegibilidad por vigente, avisos/bucle y rechazo sin llamar escritor.
- **Prueba manual:** regenerar versión bloqueada.

### TASK-017 Elegir versión vigente

- **Ficheros:** `StoryPageService.java`, repositorio de versiones, REST y pruebas (modificar).
- **Enfoque:** seleccionar solo dentro de histórico pendiente/propuestas; bloquear si el turno pertenece al cuento.
- **Pruebas automáticas:** actualización de contenido/aviso, aceptación posterior, reapertura y bloqueo concurrente.
- **Prueba manual:** elegir primera de tres.

### TASK-018 Reabrir con hint

- **Ficheros:** `StoryPageService.java` y pruebas (modificar).
- **Enfoque:** extender TASK-015 a aceptadas, dejando contenido e imagen aceptados fuera del histórico de propuestas.
- **Pruebas automáticas:** páginas posteriores intactas, bases de hints, portada, imagen y avisos.
- **Prueba manual:** consultar aceptada y propuesta separadas.

### TASK-019 Aceptar propuesta reabierta

- **Ficheros:** `StoryPageService.java`, repositorios y REST de confirmación (modificar).
- **Enfoque:** calcular cascada antes de mutar; sin confirmación devolver contador, con confirmación reemplazar K, limpiar imagen/histórico y borrar posteriores; portada no cascada.
- **Pruebas automáticas:** contador, confirmación, assets preparados, portada, sin posteriores y bloqueo concurrente.
- **Prueba manual:** aceptar con/sin confirmación.

### TASK-020 Descartar propuestas reabiertas

- **Ficheros:** `StoryPageService.java`, REST y pruebas (modificar).
- **Enfoque:** borrar únicamente propuestas y conservar aceptada, imagen y posteriores; bloquear durante generación del cuento.
- **Pruebas automáticas:** página/portada, aviso, imagen y concurrencia.
- **Prueba manual:** descartar dos propuestas.

### TASK-021 Estado listo para audio

- **Ficheros:** `StoryStatus`, persistencia/mappers de `Story` y pruebas (modificar).
- **Enfoque:** al aceptar último beat, persistir estado nuevo y negar N superior o nueva página.
- **Pruebas automáticas:** transición final/no final y rechazos de cuento completo/límite.
- **Prueba manual:** consultar cuento completado.

### TASK-022 Consulta y recursos REST de páginas

- **Ficheros:** DTOs/mappers de respuesta, `StoryPageService.java`, `StorytellerController.java` y pruebas MVC (modificar/nuevos).
- **Enfoque:** exponer consulta solo para escaleta con acciones calculadas, aceptada separada de propuestas, versiones sin imagen y descarga limitada a imagen aceptada; no crear endpoint de edición manual.
- **Pruebas automáticas:** matriz de acciones, histórico, reapertura, siguiente/completo, imagen existente/ausente y no edición.
- **Prueba manual:** datos preparados con imagen aceptada.

### TASK-023 Invalidación de páginas por beat

- **Ficheros:** `BeatEditorService.java`, `StoryPageService.java`, repositorios y pruebas (modificar).
- **Enfoque:** borrar las páginas invalidadas junto con versiones/propuestas en la misma operación de dominio.
- **Pruebas automáticas:** edición de beat elimina páginas e históricos sin afectar anteriores.
- **Prueba manual:** confirmar edición con páginas.

### TASK-024 Turno de validación de beat

- **Ficheros:** `BeatEditorService.java` y pruebas (modificar).
- **Enfoque:** adquirir/liberar puerta alrededor del director, incluidos fallos.
- **Pruebas automáticas:** ocupada, exclusión mientras valida y liberación.
- **Prueba manual:** editar beat durante página en curso.

### TASK-025 Turno de guion

- **Ficheros:** `StoryService.java` y pruebas (modificar).
- **Enfoque:** proteger generación y revisión global por hint con la misma puerta.
- **Pruebas automáticas:** rechazos con puerta ocupada, exclusión y liberación en éxito/error.
- **Prueba manual:** crear cuento durante retratos.

### TASK-026 Turno en alta y retratos

- **Ficheros:** `CharacterCatalogService.java` y pruebas (modificar).
- **Enfoque:** alta con agente adquiere/libera; solicitud de retratos adquiere y transfiere custodia al servicio asíncrono.
- **Pruebas automáticas:** rechazos, no creación de job, liberación de alta y turno ocupado al aceptar retratos.
- **Prueba manual:** alta automática durante página.

### TASK-027 Turno en revisión de fichas

- **Ficheros:** `CharacterReviewService.java` y pruebas (modificar).
- **Enfoque:** envolver la llamada al agente de revisión, liberando ante éxito y excepción.
- **Pruebas automáticas:** guion ocupado y ambos finales.
- **Prueba manual:** revisión durante guion.

### TASK-028 Turno durante retratos

- **Ficheros:** `PortraitGenerationService.java` y pruebas (modificar).
- **Enfoque:** conservar turno desde job creado hasta éxito/error, sin cambiar `PortraitLockService`.
- **Pruebas automáticas:** rechazo de página, liberación en ambos finales y bloqueo por personaje vigente.
- **Prueba manual:** página durante/después de retratos.

### TASK-036 Contrato OpenAPI: página

- **Ficheros:** entrega al documentador: `docs/contracts/openapi/schemas/story-page.yaml`.
- **Enfoque:** formalizar estado, beat, aceptada, historial/propuestas, aviso, vigente, imagen y acciones conforme a TASK-022.
- **Pruebas automáticas:** validar OpenAPI y referencias.
- **Prueba manual:** contrastar respuesta real.

### TASK-037 Contrato OpenAPI: versión

- **Ficheros:** entrega al documentador: `docs/contracts/openapi/schemas/story-page-version.yaml` (nuevo, propuesta).
- **Enfoque:** texto nullable para portada, escena, aviso y vigente; prohibir imagen en versión.
- **Pruebas automáticas:** validar esquema/referencias.
- **Prueba manual:** contrastar versión real.

### TASK-038 Contrato OpenAPI: listo para audio

- **Ficheros:** entrega al documentador: `docs/contracts/openapi/schemas/enums/story-status.yaml`.
- **Enfoque:** publicar el literal efectivamente implementado para estado final.
- **Pruebas automáticas:** validar enumerado.
- **Prueba manual:** compararlo con API.

### TASK-039 Contrato OpenAPI: operaciones de portada y página

- **Ficheros:** entrega al documentador: `docs/contracts/openapi/paths/story/story-pages-service.yaml` (nuevo, propuesta) y paths específicos si se dividen.
- **Enfoque:** documentar consultar, generar, hint, regenerar, seleccionar, aceptar/confirmar, descartar e imagen, incluidos rechazos funcionales y de puerta.
- **Pruebas automáticas:** validar OpenAPI y todas las respuestas de rechazo.
- **Prueba manual:** recorrer operaciones reales.

### TASK-040 Registro OpenAPI

- **Ficheros:** entrega al documentador: `docs/contracts/openapi/openapi.yaml`.
- **Enfoque:** registrar paths/esquemas de TASK-036, 037 y 039 sin romper referencias existentes.
- **Pruebas automáticas:** validación raíz.
- **Prueba manual:** visor OpenAPI.

### TASK-041 Contrato rechazo en guion/beat

- **Ficheros:** entrega al documentador: paths `generation-service.yaml`, `review-service.yaml`, `beat-edit-service.yaml`.
- **Enfoque:** añadir respuesta uniforme de generación global ocupada después de TASK-024/025.
- **Pruebas automáticas:** validación OpenAPI.
- **Prueba manual:** provocar rechazos.

### TASK-042 Contrato rechazo en personajes/retratos

- **Ficheros:** entrega al documentador: paths de catálogo, revisión y retratos existentes.
- **Enfoque:** reflejar la misma respuesta tras TASK-026–028.
- **Pruebas automáticas:** validación OpenAPI.
- **Prueba manual:** provocar rechazos.

### TASK-043 Contrato WebSocket

- **Ficheros:** entrega al documentador: `docs/contracts/websocket/story-page-generation-events.yaml` (nuevo, propuesta).
- **Enfoque:** canal `/storyteller`, emisor/receptor y discriminante `type`; `storyId`, `pageNumber` y `reason` en rechazo.
- **Pruebas automáticas:** validar el formato elegido y payloads serializados.
- **Prueba manual:** capturar los cuatro eventos.

## Orden de implementación

1. TASK-001, TASK-002, TASK-003, TASK-004 y TASK-008 (sin dependencias).
2. TASK-005; TASK-007 (habilitada por TASK-005); TASK-009 (por TASK-008).
3. TASK-010; TASK-011 (001,003,004,007,008,010); TASK-012; TASK-013; TASK-014.
4. TASK-015 y TASK-016 (por TASK-014); TASK-017 (003,012); TASK-018; TASK-020; TASK-019; TASK-021; TASK-022.
5. TASK-023; TASK-024 (001,023); TASK-025; TASK-026; TASK-027; TASK-028.
6. Tras implementación estable, transferir TASK-036–043 al documentador en orden 036–040, 041–042 y 043.

## Riesgos y dudas técnicas

- Migraciones: asignar números solo tras releer el directorio; hoy la última es `023`.
- Asincronía: la liberación de puerta y publicación de fallo deben estar en `finally`; el reinicio deja turno libre y la app recarga al reconectar.
- REST propuesto: bajo `/api/v1/storyteller/{storyId}`, `GET /pages`, `POST /cover/generate`, y para `pages/{number}` generar, hint, regenerar, seleccionar versión, aceptar/confirmar, descartar e imagen; se confirma en TASK-039 según controladores implementados.
- WebSocket propuesto: payload con `type=STARTED|VERSION_PRESENTED|HINT_REJECTED|FAILED`, `storyId`, `pageNumber` y `reason` solo para rechazo; app debe recargar `GET /pages` tras evento.
- Coordinación agents: después de TASK-005, retirar su Modelfile/cargador heredado únicamente tras verificar el aprovisionador backend; no tocar `agents/` desde este sprint.

## Validación

- Suite del módulo: correcta. Origen: confirmación del usuario ("success (tests y pruebas)") en la sesión del desarrollador-router tras TASK-014 y la corrección del defecto de beat_id de TASK-013. Alcance: `mvn -f api/pom.xml test` completa tras TASK-014 y esa corrección (incluye TASK-001 a TASK-005 y TASK-007 a TASK-013).
- Integración: pendiente
- Validación manual: migraciones 024 y 025 comprobadas por el usuario (alcance: TASK-003 y TASK-004); TASK-005 correcta (origen: confirmación del usuario "success (test, pruebas)"; alcance: pruebas manuales de la tarea, sin desglose por parte del usuario; las propuestas eran arrancar sin/con modelo con una única creación y revisión del Modelfile); TASK-007 correcta (origen: confirmación del usuario "success (test y prueba del harness)"; alcance: ejecución del arnés SpringAiWriterAgentManualHarness contra Ollama real; el usuario no desglosó el resultado de cada caso); TASK-001, TASK-002 y TASK-008 no aplica; el resto, pendiente. La manual de TASK-011 (portada: 202, STARTED y VERSION_PRESENTED con pageNumber 0, 409 por portada existente y por generación en curso, consulta de la portada pendiente) queda pendiente, igual que la de TASK-009 asociada a ella (STARTED y VERSION_PRESENTED en la portada); el usuario no desglosó pruebas manuales de TASK-011 ni de TASK-009 y no se registran como validadas manualmente. La manual de TASK-010 (cuento antiguo con y sin retratos en curso) queda pendiente: el usuario no desglosó pruebas manuales de TASK-010 y no se registra como validada manualmente; el paso con retratos depende de TASK-026 y TASK-028. La manual de TASK-012 (aceptar portada: 200 ACCEPTED con pageNumber 0, estado e histórico en base de datos, 409 al repetir y con generación en curso) queda pendiente: el usuario no desglosó pruebas manuales de TASK-012 y no se registra como validada manualmente. TASK-013 y TASK-014 correctas (origen: confirmación del usuario "success (tests y pruebas)"; alcance: las pruebas manuales propuestas para ambas, página 1 sin error de FK con beat_id correcto y tres versiones fuera de límite con la tercera vigente; el usuario no desglosó el resultado por caso).
