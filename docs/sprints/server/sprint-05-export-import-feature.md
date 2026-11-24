# Sprint 05: export-import-feature - server

Estado: aprobado

## Alcance

- Tareas: TASK-001 a TASK-015, TASK-022 a TASK-030 y TASK-033 a TASK-043 (de `specs/export-import-feature/tasks.md`).
- Dependencias internas: 003<-002; 004<-001,002; 005<-001; 007<-006; 008<-001,006; 009,010<-008; 011<-004,008; 012<-008,011; 014<-013; 015<-008; 024<-023; 025<-001,024; 029<-001,022,026,027,028; 030<-025,029. Las tareas de contrato dependen de su implementación indicada en `tasks.md`.
- Dependencias externas: Ninguna. Las tareas 013, 022--030 y 040--043 se incorporan a `server` por confirmación expresa aunque el mapa aún no las registre.

## Origen SDD

- Requirements: 5
- Design: 1
- Tasks: 1

## Enfoque técnico

Se ampliará el modelo de escaleta y la página 0 sin convertir la portada en un beat ni incluirla en la cascada interior. La subida conservará el original y la exportación operará en memoria/temporales, entregando zips síncronos no persistidos. Se extraerán servicios deterministas pequeños para slug, inspección, conversión, composición y validación, coordinados por un único servicio de exportación. Para JSON y validación acumulada se toma como referencia `framework-json`/`Validator` de la guía 0.2.0-SNAPSHOT (Uso 1 de `JsonMapperFactory`, `Validator` y `ValidationResult`); el POM local declara 0.1.0 y el usuario confirma objetivo 0.2.0, por lo que las APIs no están verificadas ni para el 0.1.0 local ni para una versión final 0.2.0.

## Cambios por tarea

### TASK-001 Guion persistido con lugar de escena y planteamiento de portada

- **Ficheros:** `domain/model/StoryOutline.java`, `StoryBeat.java`, entidades/mappers/repositorios de outline y beats, migración Liquibase siguiente disponible y sus pruebas (modificar/nuevos).
- **Enfoque:** persistir `setting` por beat y un objeto de planteamiento de portada separado; tolerar nulos de filas antiguas.
- **Pruebas automáticas:** ampliar `StoryOutlineRepositoryAdapterTest` con separación portada/página 1 y lectura legacy; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-002 Director genera lugar por beat y planteamiento de portada

- **Ficheros:** `SpringAiDirectorAgent.java`, DTOs/resultados de director y `DirectorAgentAdapterTest` (modificar).
- **Enfoque:** deserializar los nuevos campos tanto para creación como hint sin rellenar ausencias.
- **Pruebas automáticas:** respuesta completa, revisión global y campo ausente; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-003/017.

### TASK-003 Modelfile del director con las reglas de lugar y de planteamiento de portada

- **Ficheros:** `resources/prompts/director.modelfile`, `DirectorModelProvisioner.java` y pruebas (modificar).
- **Enfoque:** actualizar reglas y revisar el provisionador para reemplazar/reprovisionar el modelo ya existente, según la API ya usada por `OllamaPort`.
- **Pruebas automáticas:** modelo previo y ausente; `mvn -f api/pom.xml test`.
- **Prueba manual:** arrancar con modelo anterior y comprobar guion de cuatro beats y portada independientes.

### TASK-004 Validación del guion ampliado

- **Ficheros:** `StoryService.java`, validadores/resultados de guion y `StoryServiceTest` (modificar).
- **Enfoque:** exigir valores no blancos al crear y revisar, antes de guardar; conservar el tratamiento actual de salida incompleta.
- **Pruebas automáticas:** completo, beat/portada incompletos y espacios; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-005 Lugar de escena editable en la edición directa de un beat

- **Ficheros:** `BeatEditorService.java`, comando de edición, `StoryBeat` y `BeatEditorServiceTest` (modificar).
- **Enfoque:** trasladar y validar `setting` sin tocar el planteamiento de portada.
- **Pruebas automáticas:** edición correcta y rechazo sin persistencia; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-017.

### TASK-006 Escritor redacta la escena de portada desde su lugar y su tono

- **Ficheros:** `SpringAiWriterAgent.java`, prompts `new_cover.st`/`review_cover.st`, puerto/resultado y `SpringAiWriterAgentTest` (modificar).
- **Enfoque:** incorporar lugar y tono de portada a generación e hint, preservando el resultado completo o rechazo motivado.
- **Pruebas automáticas:** petición inicial, hint aceptado y rechazado; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-007/021.

### TASK-007 Modelfile del escritor con las reglas de la escena de portada

- **Ficheros:** `resources/prompts/writer.modelfile`, `WriterModelProvisioner.java` y pruebas (modificar).
- **Enfoque:** añadir las reglas de portada y garantizar reprovisión de modelo previo conforme a TASK-003.
- **Pruebas automáticas:** modelo presente/ausente; `mvn -f api/pom.xml test`.
- **Prueba manual:** generar portada real con lugar y tono definidos.

### TASK-008 Generación y revisión con hint de la portada

- **Ficheros:** `StoryPageService.java`, puertos de generación/aceptación, notifier y pruebas `StoryPageService*` (modificar).
- **Enfoque:** reutilizar el ciclo de versiones y gate para página 0, validar planteamiento antes de llamar al escritor y publicar los eventos existentes.
- **Pruebas automáticas:** generar, aceptar, hint pendiente/aceptada/rechazado/fallo, legacy y gate ocupado; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-021.

### TASK-009 Página 1 bloqueada hasta aceptar la portada

- **Ficheros:** `StoryPageService.java` y pruebas de generación (modificar).
- **Enfoque:** para cuentos con escaleta, admitir página 1 solo tras primera aceptación de portada; no encadenarla al aceptar.
- **Pruebas automáticas:** portada pendiente/sin escena/aceptada y hint posterior; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-021.

### TASK-010 Portada fuera de la cascada y retirada de imágenes al reaprobar

- **Ficheros:** `StoryPageService.java`, repositorio de página/asset y pruebas de aceptación/reapertura (modificar).
- **Enfoque:** separar explícitamente página 0 de la cascada interior; limpiar solo su imagen al reaceptar y mantener la limpieza de imágenes interiores en cascada.
- **Pruebas automáticas:** conservar interiores/audio, limpiar portada y regresión interior; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-020/021.

### TASK-011 Aprobar el guion lanza la generación de la portada

- **Ficheros:** `StoryService.java`, respuesta de aprobación y `StoryServiceTest` (modificar).
- **Enfoque:** aprobar primero y solicitar generación no bloqueante; devolver si arrancó y nunca reintentar ni crear página 1.
- **Pruebas automáticas:** turno libre, ocupado y escritor fallido; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-021.

### TASK-012 Rehacer el guion descarta la portada y su imagen

- **Ficheros:** `StoryService.java`, `StoryPageService.java`, repositorios y pruebas (modificar).
- **Enfoque:** conectar con el mecanismo vigente de rehacer guion para borrar versiones, aceptado e imagen de página 0, sin ampliar disparadores.
- **Pruebas automáticas:** portada aceptada/pendiente, regeneración posterior y no-disparadores; `mvn -f api/pom.xml test`.
- **Prueba manual:** rehacer y reaprobar un cuento con portada importada.

### TASK-013 Detección de imagen por su contenido

- **Ficheros:** `application/service/ImageContentInspector.java` (nuevo, propuesta) y prueba homónima (nueva).
- **Enfoque:** inspeccionar bytes mediante decodificación/firma, ignorando nombre, MIME, tamaño y dimensiones; no imponer límites.
- **Pruebas automáticas:** PNG, JPEG grande, BMP, texto disfrazado y PNG con extensión distinta; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-014 Subida de imagen en la portada y en las páginas aceptadas

- **Ficheros:** `StoryPageService.java`, puerto `ResourceStorage`, controlador/DTO multipart vigente y pruebas de servicio/REST (modificar).
- **Enfoque:** validar contenido y aceptación antes de sustituir el asset; conservar el anterior en cualquier rechazo y persistir la nueva ruta.
- **Pruebas automáticas:** formatos/tamaños permitidos, sustitución, rechazos y recarga; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-020.

### TASK-015 Descripción de escena completa de la portada y de las páginas en la vista

- **Ficheros:** `StoryPageQueryService.java`, `StoryPagesView.java`, DTO de detalle y pruebas (modificar).
- **Enfoque:** proyectar la escena aceptada completa de cada página, tratando portada como página 0 independiente.
- **Pruebas automáticas:** portada distinta de página 1 y página 2 sin truncado; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-019.

### TASK-022 Derivación de slugs deterministas

- **Ficheros:** `domain/service/StorySlugger.java` y prueba (nuevos, propuesta).
- **Enfoque:** normalizar Unicode a ASCII, compactar separadores, truncar sin guion final y devolver resultado/validez sin inventar identificadores.
- **Pruebas automáticas:** acentos, ñ, corte, menos de tres y determinismo; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-023 Conversión de imágenes a WebP para la exportación

- **Ficheros:** `application/service/ExportImageConverter.java` (nuevo, propuesta) y prueba (nueva).
- **Enfoque:** usar exclusivamente capacidad ya disponible en runtime; decodificar, orientar, escalar y codificar una copia WebP 85 sin metadatos, devolviendo fallo por cada conversión imposible.
- **Pruebas automáticas:** PNG/BMP, EXIF, límites, original inmutable y decodificación fallida; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-032.

### TASK-024 Zip para Nubi Game con referencias explícitas

- **Ficheros:** `application/service/NubiGameExporter.java` (nuevo, propuesta), `ResourceStorage.java`/`ResourceStorageAdapter.java`, modelos de exportación y pruebas (modificar/nuevos).
- **Enfoque:** sustituir `downloadStory` heredado por ensamblado temporal con rutas explícitas, WebP y audio interior; borrar temporales siempre.
- **Pruebas automáticas:** contenido/rutas/orden/asociación y ausencia de residuos; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-032.

### TASK-025 Exportación para Nubi Game con evaluación completa de faltas

- **Ficheros:** `application/service/StoryExportService.java`, puerto de entrada, `StorytellerService.java`/controlador de descarga y pruebas (nuevos/modificar).
- **Enfoque:** reemplazar la operación heredada, reunir todas las faltas y conversiones antes de devolver zip o rechazo estructurado.
- **Pruebas automáticas:** completo, portada legacy, faltas múltiples, audio de portada irrelevante y conversiones parciales; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-032.

### TASK-026 Lectura de la instantánea del protagonista

- **Ficheros:** puerto `StoryProtagonistSnapshotReader` y adaptador sobre `StoryJpaEntity`/mapper existentes; pruebas de persistencia (nuevos/modificar, propuesta).
- **Enfoque:** exponer lectura de nombre, descripción y retrato de la instantánea, nunca del catálogo vivo.
- **Pruebas automáticas:** instantánea y catálogo editado después; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-027 Eliminación del fondo del retrato con ComfyUI

- **Ficheros:** `application/service/CharacterBackgroundRemover.java`, puerto, recurso workflow ComfyUI y `ComfyUIClient.java`/pruebas (nuevos/modificar, propuesta).
- **Enfoque:** subir bytes de snapshot, lanzar workflow, sondear y descargar PNG sin reescalar ni adquirir gate; expresar fallo recuperable de obtención.
- **Pruebas automáticas:** éxito, error/sin resultado y conservación de dimensiones; `mvn -f api/pom.xml test`.
- **Prueba manual:** ejecutar workflow contra ComfyUI y confirmar nodo instalado.

### TASK-028 Validador del contrato v0.1 del agente visual

- **Ficheros:** `application/service/VisualContractValidator.java` y pruebas (nuevos, propuesta).
- **Enfoque:** validar en una pasada JSON UTF-8 sin BOM/duplicados, estructura e imagen; usar `JsonMapperFactory` y `Validator`/`ValidationResult` de la guía 0.2.0-SNAPSHOT como referencia y acumular incumplimientos seguros.
- **Pruebas automáticas:** entrada conforme y todas las combinaciones de reglas, incluidas dos faltas; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-029 Composición del `story.json` y del zip para el agente visual

- **Ficheros:** `application/service/VisualAgentExporter.java`, DTOs JSON, `ResourceStorage` y pruebas (nuevos/modificar, propuesta).
- **Enfoque:** componer datos deterministas de guion, páginas y snapshot; extraer rasgos literales, usar tono trim como emoción, validar antes de zip y no persistir resultados.
- **Pruebas automáticas:** raíz, slug, portada, escenas, referencias, rasgos y rechazo de validador; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-032.

### TASK-030 Exportación para el agente visual con evaluación completa de condiciones

- **Ficheros:** `StoryExportService.java`, puerto/controlador de exportación y pruebas (modificar/nuevos).
- **Enfoque:** reunir estado de guion/portada/páginas/planteamiento antes de llamar al exportador; combinar faltas de disponibilidad, imagen y contrato en una respuesta.
- **Pruebas automáticas:** completo sin assets de página, legacy, faltas múltiples, retrato pequeño y ComfyUI/contrato fallidos; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-032.

### TASK-033 Contrato: openapi - lugar de escena en el beat

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/schemas/story-beat.yaml`.
- **Enfoque y pruebas:** documentar `setting` obligatorio según TASK-004 y validar OpenAPI completo; contraste manual con respuesta implementada.

### TASK-034 Contrato: openapi - lugar y tono de la portada en el guion

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/schemas/story-outline.yaml`, `story-script.yaml`.
- **Enfoque y pruebas:** describir planteamiento opcional en cuentos legacy y validar OpenAPI completo; contraste manual con DTO final.

### TASK-035 Contrato: openapi - lugar de escena en la edición de beat

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `paths/story/beat-edit-service.yaml`, `requests/beat-edit-request.yaml`.
- **Enfoque y pruebas:** formalizar campo y rechazo por vacío tras TASK-005; validar OpenAPI y contrastar controlador.

### TASK-036 Contrato: openapi - aprobar el guion lanza la portada

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `paths/story/approve-service.yaml`.
- **Enfoque y pruebas:** reflejar indicador de inicio y aprobación independiente del fallo; validar OpenAPI y respuesta real.

### TASK-037 Contrato: openapi - generación de portada sin planteamiento

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `paths/story/cover-generate-service.yaml`.
- **Enfoque y pruebas:** documentar rechazo por planteamiento ausente y disparo tras aprobación; validar y contrastar.

### TASK-038 Contrato: openapi - hint de portada, bloqueo de página 1 y aceptación sin cascada

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths `page-hint`, `page-generate`, `page-accept` y `page-accept-proposal`.
- **Enfoque y pruebas:** formalizar las cuatro variantes finales; validar OpenAPI y contrastar respuestas.

### TASK-039 Contrato: openapi - subida de imagen de portada o página

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path OpenAPI de imagen por definir tras TASK-014.
- **Enfoque y pruebas:** documentar multipart, sustitución y rechazos sin límite propio; validar y contrastar implementación.

### TASK-040 Contrato: openapi - exportación para Nubi Game

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path/esquemas OpenAPI nuevos, propuestos tras TASK-025.
- **Enfoque y pruebas:** documentar descarga y lista completa de faltas, retirando la operación anterior; validar OpenAPI.

### TASK-041 Contrato: openapi - exportación para el agente visual

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path/esquemas OpenAPI nuevos, propuestos tras TASK-030.
- **Enfoque y pruebas:** formalizar descarga y rechazos acumulados; validar OpenAPI y contraste final.

### TASK-042 Contrato: file/json - zip para Nubi Game

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/nubi-game/story-export.md` (nuevo).
- **Enfoque y pruebas:** documentar zip/rutas/WebP según TASK-024; comprobar `story.json` exportado frente al contrato.

### TASK-043 Contrato: file/json - StoryTeller como productor del agente visual

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/agent-visual/input.md`.
- **Enfoque y pruebas:** actualizar solo estado/trazabilidad a la feature, preservar v0.1; repaso manual de estructura intacta.

## Orden de implementación

1. TASK-001, 002, 006, 013, 022, 023, 026, 027 y 028 (habilitadas por: sin dependencias).
2. TASK-003, 004, 005, 007, 014, 024 y 029 (habilitadas por: entregas internas indicadas).
3. TASK-008 y 015; después TASK-009, 010 y 011 (habilitadas por: portada persistida/escritor y servicio de páginas).
4. TASK-012, 025 y 030 (habilitadas por: los servicios de exportación y portada finalizados).
5. TASK-033 a TASK-043 (habilitadas por: implementación efectiva de sus tareas; las ejecuta documentador-server).

## Riesgos y dudas técnicas

- La conversión WebP sin nuevas dependencias exige comprobar en TASK-023 que el runtime actual dispone de un codificador WebP compatible; si no, no se añade dependencia por este sprint y se informa del bloqueo técnico.
- El workflow/nodo de eliminación de fondo debe estar disponible en ComfyUI; su ausencia impide la exportación visual pero no justifica usar `generation-gate`.
- La actualización real de `vargontoc-framework` a 0.2.0 no forma parte de tareas aprobadas. La guía 0.2.0-SNAPSHOT se usa por confirmación del usuario; APIs no verificadas contra 0.2.0 final ni contra 0.1.0 local.
- TASK-039--041 requieren definir rutas y cuerpos REST desde los controladores terminados; no se adelanta un contrato distinto de la implementación.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
