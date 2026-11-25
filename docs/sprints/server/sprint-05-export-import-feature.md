# Sprint 05: export-import-feature - server

Estado: aprobado

## Alcance

- Tareas pendientes: TASK-003--005, TASK-007--012, TASK-014--015, TASK-024--030 y TASK-033--044, excepto TASK-023 retirada.
- Antecedentes completados: TASK-001, 002, 006, 013 y 022; se conservan como dependencias satisfechas.
- Dependencias internas: 003<-002; 004<-001,002; 005<-001; 007<-006; 008<-001,006; 009,010<-008; 011<-004,008; 012<-008,011; 014<-013; 015<-008; 024<-044; 025<-001,024; 029<-001,022,026,027,028; 030<-025,029. Los contratos dependen de su implementación.
- Dependencias externas: Ninguna. TASK-044 y componentes no mapeados se asignan a server por confirmación expresa.

## Origen SDD

- Requirements: 7
- Design: 2
- Tasks: 2

## Antecedentes completados

- TASK-001: persistencia de lugar por beat y planteamiento de portada, compatible con cuentos legacy.
- TASK-002 y 006: director y escritor ya trasladan lugar/tono de portada.
- TASK-013: ImageContentInspector decide por contenido si el fichero es imagen.
- TASK-022: StorySlugger proporciona slugs deterministas.

## Enfoque técnico

La página 0 se completa sin entrar en la cascada interior y se conserva el original importado. Nubi Game genera un zip síncrono no persistido que copia bytes originales; solo obtiene la extensión desde el formato real detectado al exportar, sin conversión, reorientación ni redimensionado. Servicios pequeños de formato, zip y validación visual serán coordinados por StoryExportService. Para JSON y validación acumulada se toma la guía 0.2.0-SNAPSHOT (JsonMapperFactory, Validator y ValidationResult); el POM declara 0.2.0, pero las APIs no están verificadas contra el release final.

## Cambios por tarea

### TASK-003 Modelfile del director con las reglas de lugar y de planteamiento de portada

- **Ficheros:** resources/prompts/director.modelfile, DirectorModelProvisioner.java y pruebas (modificar).
- **Enfoque:** reglas de beats/portada independientes y reprovisión del modelo existente.
- **Pruebas automáticas:** modelo previo y ausente; mvn -f api/pom.xml test.
- **Prueba manual:** generar cuatro beats y portada independientes.

### TASK-004 Validación del guion ampliado

- **Ficheros:** StoryService.java, validadores/resultados y StoryServiceTest (modificar).
- **Enfoque:** exigir valores no blancos en creación y revisión antes de persistir.
- **Pruebas automáticas:** completo, campos ausentes y espacios; mvn -f api/pom.xml test.
- **Prueba manual:** no aplica.

### TASK-005 Lugar de escena editable en la edición directa de un beat

- **Ficheros:** BeatEditorService.java, comando, StoryBeat y pruebas (modificar).
- **Enfoque:** editar y validar setting sin modificar portada.
- **Pruebas automáticas:** edición correcta y vacío sin persistencia; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-007 Modelfile del escritor con las reglas de la escena de portada

- **Ficheros:** resources/prompts/writer.modelfile, WriterModelProvisioner.java y pruebas (modificar).
- **Enfoque:** reglas de escena/hint de portada y reprovisión de modelo existente.
- **Pruebas automáticas:** modelo presente/ausente; mvn -f api/pom.xml test.
- **Prueba manual:** generar portada real con lugar y tono.

### TASK-008 Generación y revisión con hint de la portada

- **Ficheros:** StoryPageService.java, puertos, notifier y pruebas StoryPageService (modificar).
- **Enfoque:** reutilizar versiones/gate para página 0 y validar planteamiento antes del escritor.
- **Pruebas automáticas:** generar, aceptar, hints, fallo, legacy y gate ocupado; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-009 Página 1 bloqueada hasta aceptar la portada

- **Ficheros:** StoryPageService.java y pruebas de generación (modificar).
- **Enfoque:** admitir página 1 tras primera aceptación de portada, sin encadenarla.
- **Pruebas automáticas:** pendiente/sin escena/aceptada e hint posterior; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-010 Portada fuera de la cascada y retirada de imágenes al reaprobar

- **Ficheros:** StoryPageService.java, repositorio/asset y pruebas (modificar).
- **Enfoque:** aislar página 0 de cascada; limpiar solo su imagen al reaceptar.
- **Pruebas automáticas:** conservar interiores/audio, limpiar portada y regresión interior; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-011 Aprobar el guion lanza la generación de la portada

- **Ficheros:** StoryService.java, respuesta de aprobación y pruebas (modificar).
- **Enfoque:** aprobar primero, solicitar generación no bloqueante e informar inicio sin reintento.
- **Pruebas automáticas:** turno libre, ocupado y escritor fallido; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-012 Rehacer el guion descarta la portada y su imagen

- **Ficheros:** StoryService.java, StoryPageService.java, repositorios y pruebas (modificar).
- **Enfoque:** conectar el rehacer vigente con descarte de versiones, aceptado e imagen de página 0.
- **Pruebas automáticas:** portada aceptada/pendiente, regeneración y no-disparadores; mvn -f api/pom.xml test.
- **Prueba manual:** rehacer y reaprobar portada importada.

### TASK-014 Subida de imagen en la portada y en las páginas aceptadas

- **Ficheros:** StoryPageService.java, ResourceStorage, controlador/DTO multipart y pruebas (modificar).
- **Enfoque:** inspeccionar contenido y aceptación antes de sustituir; conservar asset previo en rechazo.
- **Pruebas automáticas:** formatos/tamaños, sustitución, rechazos y recarga; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-015 Descripción de escena completa de la portada y de las páginas en la vista

- **Ficheros:** StoryPageQueryService.java, StoryPagesView.java, detalle y pruebas (modificar).
- **Enfoque:** proyectar escena aceptada completa, incluida portada propia.
- **Pruebas automáticas:** portada distinta de página 1 e interior sin truncado; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-044 Identificación del formato real de una imagen por su contenido

- **Ficheros:** application/service/ImageContentInspector.java y ImageContentInspectorTest.java (modificar).
- **Enfoque:** devolver formato real y extensión habitual desde bytes, sin nombre/MIME y sin mutar el fichero; cada formato aceptado tiene extensión.
- **Pruebas automáticas:** PNG/JPEG/BMP, nombre falso, todos los formatos, texto y bytes inmutables; mvn -f api/pom.xml test.
- **Prueba manual:** no aplica.

### TASK-024 Zip para Nubi Game con referencias explícitas e imágenes originales

- **Ficheros:** application/service/NubiGameExporter.java (nuevo, propuesta), ResourceStorage.java/ResourceStorageAdapter.java, modelos y pruebas.
- **Enfoque:** sustituir zip heredado por ensamblado temporal con rutas explícitas; copiar imagen byte a byte con extensión de TASK-044, conservar audio interior y limpiar temporales.
- **Pruebas automáticas:** contenido/rutas/orden, extensiones reales, BMP original y ausencia de residuos; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-025 Exportación para Nubi Game con evaluación completa de faltas

- **Ficheros:** StoryExportService.java, puerto, StorytellerService.java/controlador y pruebas (nuevos/modificar).
- **Enfoque:** reemplazar operación heredada y acumular solo faltas de disponibilidad; formatos válidos no provocan rechazo.
- **Pruebas automáticas:** completo, legacy, faltas múltiples, audio de portada irrelevante y PNG/JPEG/BMP; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-026 Lectura de la instantánea del protagonista

- **Ficheros:** puerto StoryProtagonistSnapshotReader, adaptador sobre StoryJpaEntity/mapper y pruebas (nuevos/modificar).
- **Enfoque:** leer nombre, descripción y retrato de snapshot, nunca catálogo vivo.
- **Pruebas automáticas:** snapshot y catálogo editado; mvn -f api/pom.xml test.
- **Prueba manual:** no aplica.

### TASK-027 Eliminación del fondo del retrato con ComfyUI

- **Ficheros:** CharacterBackgroundRemover.java, puerto, workflow, ComfyUIClient.java y pruebas.
- **Enfoque:** procesar snapshot sin reescalar ni gate; devolver fallo recuperable.
- **Pruebas automáticas:** éxito, error/sin resultado y dimensiones; mvn -f api/pom.xml test.
- **Prueba manual:** workflow real con nodo disponible.

### TASK-028 Validador del contrato v0.1 del agente visual

- **Ficheros:** VisualContractValidator.java y pruebas (nuevos).
- **Enfoque:** validar en una pasada JSON, estructura e imagen, acumulando incumplimientos con referencia a la guía framework.
- **Pruebas automáticas:** conforme, cada regla y faltas combinadas; mvn -f api/pom.xml test.
- **Prueba manual:** no aplica.

### TASK-029 Composición del story.json y del zip para el agente visual

- **Ficheros:** VisualAgentExporter.java, DTOs JSON, ResourceStorage y pruebas.
- **Enfoque:** composición determinista desde guion/páginas/snapshot, validación antes de zip no persistido.
- **Pruebas automáticas:** raíz, slug, portada, escenas, rasgos, referencias y rechazo; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-030 Exportación para el agente visual con evaluación completa de condiciones

- **Ficheros:** StoryExportService.java, puerto/controlador y pruebas.
- **Enfoque:** acumular condiciones de guion, portada, páginas, imagen y contrato antes de exportar.
- **Pruebas automáticas:** completo, legacy, faltas múltiples, retrato pequeño y fallos ComfyUI/contrato; mvn -f api/pom.xml test.
- **Prueba manual:** cubierta por app.

### TASK-033 Contrato: openapi - lugar de escena en el beat

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** docs/contracts/openapi/schemas/story-beat.yaml.
- **Enfoque y pruebas:** documentar setting obligatorio, validar OpenAPI y contrastar respuesta.

### TASK-034 Contrato: openapi - lugar y tono de la portada en el guion

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** story-outline.yaml, story-script.yaml.
- **Enfoque y pruebas:** describir legacy y validar OpenAPI contra DTO final.

### TASK-035 Contrato: openapi - lugar de escena en la edición de beat

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths/story/beat-edit-service.yaml, requests/beat-edit-request.yaml.
- **Enfoque y pruebas:** campo y rechazo vacío; validar y contrastar controlador.

### TASK-036 Contrato: openapi - aprobar el guion lanza la portada

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths/story/approve-service.yaml.
- **Enfoque y pruebas:** inicio/no inicio independiente de aprobación; validar OpenAPI.

### TASK-037 Contrato: openapi - generación de portada sin planteamiento

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths/story/cover-generate-service.yaml.
- **Enfoque y pruebas:** rechazo y disparo tras aprobación; validar y contrastar.

### TASK-038 Contrato: openapi - hint de portada, bloqueo de página 1 y aceptación sin cascada

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths page-hint, page-generate, page-accept y page-accept-proposal.
- **Enfoque y pruebas:** formalizar variantes; validar OpenAPI y respuestas.

### TASK-039 Contrato: openapi - subida de imagen de portada o página

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path de imagen por definir tras TASK-014.
- **Enfoque y pruebas:** multipart, sustitución y rechazos; validar contra implementación.

### TASK-040 Contrato: openapi - exportación para Nubi Game

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path/esquemas nuevos tras TASK-025.
- **Enfoque y pruebas:** descarga/lista de faltas, sin rechazo por formato; validar OpenAPI.

### TASK-041 Contrato: openapi - exportación para el agente visual

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** path/esquemas nuevos tras TASK-030.
- **Enfoque y pruebas:** descarga y rechazos acumulados; validar y contrastar.

### TASK-042 Contrato: file/json - zip para Nubi Game

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** docs/contracts/nubi-game/story-export.md (nuevo).
- **Enfoque y pruebas:** rutas y originales con extensión real, sin conversión/campo de formato; comprobar zip final.

### TASK-043 Contrato: file/json - StoryTeller como productor del agente visual

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** docs/contracts/agent-visual/input.md.
- **Enfoque y pruebas:** actualizar solo estado/trazabilidad y preservar v0.1.

## Orden de implementación

1. TASK-003--005, 007--008, 014--015, 026--028 y 044 (habilitadas por antecedentes/dependencias satisfechos).
2. TASK-009--011 y TASK-024 (habilitadas por TASK-008 o TASK-044).
3. TASK-012 y TASK-029 (habilitadas por portada y servicios previos).
4. TASK-025 y TASK-030 (habilitadas por exportadores terminados).
5. TASK-033--043 (habilitadas por implementación efectiva; las ejecuta documentador-server).

## Riesgos y dudas técnicas

- TASK-023 y DEC-007 están retiradas: no hay WebP ni dependencia adicional; Nubi Game asume formatos originales.
- Todo formato aceptado necesita extensión en TASK-044; formatos no soportados por Nubi quedan fuera de alcance.
- Imágenes originales grandes se copian síncronamente y pueden consumir memoria.
- ComfyUI debe disponer del nodo de eliminación de fondo; no se usa generation-gate.
- Las rutas/cuerpos de TASK-039--041 se derivan de controladores terminados.
- La guía es 0.2.0-SNAPSHOT y POM 0.2.0; APIs pendientes de verificar contra release final.

## Validación

- Suite del módulo: correcta (origen: confirmación del usuario "success, siguiente tarea" tras TASK-025; alcance: mvn -f api/pom.xml test en el estado tras TASK-025, que incluye la retirada de la exportación heredada). Actualización: correcta (origen: confirmación del usuario "success, continuar con el sprint de app" tras TASK-030 y la corrección posterior del guion/revisión global; alcance: mvn -f api/pom.xml test, 782 tests en la última ejecución, con exposición de `setting` por beat y `cover` en StoryScript, revisión global con setting/cover, migración 029 y corrección de compilación de GenerationsControllerBeatTest).
- Integración: pendiente
- Validación manual: la prueba manual de TASK-003 (modelo antiguo registrado, ollama show, cuento de 4 páginas con lugares y portada, revisión global por hint) correcta según la confirmación del usuario tras TASK-003; la prueba manual de TASK-007 (modelo antiguo del escritor reprovisionado, escena de portada coherente con lugar y tono, hint) correcta según la confirmación del usuario "success(test, manual), siguiente tarea" tras TASK-007; TASK-004 y TASK-005 no tienen prueba manual propia (se verifican en TASK-017); TASK-008 no tiene prueba manual propia (se verifica en TASK-021); TASK-009 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-021); TASK-010 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-020/021); TASK-011 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-021, incluido el comportamiento transaccional de approve sin transacción propia); TASK-014 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-020); TASK-015 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-019); TASK-026 no tiene prueba manual propia (se verifica en TASK-032); la prueba manual de TASK-027 (eliminación de fondo con ComfyUI sobre un retrato) correcta según la confirmación del usuario "success(test, manual), siguiente tarea" tras TASK-027; TASK-028 no tiene prueba manual propia (su efecto se verifica en TASK-032); TASK-044 no tiene prueba manual propia (se verifica en TASK-032); TASK-024 no tiene prueba manual propia (se verifica en TASK-032); TASK-029 no tiene prueba manual propia (se verifica en TASK-032); TASK-025 no tiene prueba manual propia en server (se verifica desde la interfaz en TASK-032); la prueba manual de TASK-012 queda PENDIENTE (el usuario aún no tiene imágenes ni la herramienta correspondiente; las pruebas manuales relacionadas con imágenes, TASK-012 y las de subida/retirada de imagen de TASK-010/014 que se verifican desde la interfaz, se harán cuando esté disponible); el resto de pruebas manuales del sprint, pendientes. Tras TASK-030 y la corrección del guion/revisión global el usuario no desglosó pruebas manuales, por lo que siguen PENDIENTES: migraciones 028 y 029 contra PostgreSQL; revisión global con guion nuevo (crear, consultar, confirmar); prueba de TASK-012 y pruebas con imágenes (TASK-010/012/014), a la espera de la herramienta de imágenes (rembg ya correcta en TASK-027). Los antecedentes sin prueba manual propia conservan su condición; el resto se valida con integración.

Asuntos abiertos conocidos (sin resolver):
- BeatEditorService.deleteInvalidatedPages borra la portada pese a lo que dice el diseño.
- Páginas interiores que quedan tras confirmReview.
- Transacción abierta durante la llamada a ComfyUI en StoryExportService.
- Seis+dos códigos de error propios del validador/exportador visual que no están en el contrato v0.1.
- TASK-027 (eliminación de fondo con ComfyUI) queda sin uso por decisión expresa del usuario: la exportación visual usa el retrato de la instantánea, ya transparente; REQ-025/DEC-008, TASK-027/029/030 y el contrato de TASK-041 requieren revisión documental.
