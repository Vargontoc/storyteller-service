# Sprint 04: agent-narrator - server

Estado: aprobado

## Alcance

- Tareas: TASK-001 a TASK-025, porción server de TASK-026 a TASK-028 y TASK-036 a TASK-042 (de `specs/agent-narrator/tasks.md`).
- Dependencias internas: 003<-002; 009<-006,007; 010<-005..009; 011<-010; 013<-001; 014<-013; 019<-012; 020<-011,012,017,019; 021<-014,015,016,020; 023<-012,017; 024<-019; 025<-024; 026<-019,024; 027<-010,011; 028<-019..022; 036<-027; 037<-028; 038<-027,028; 039<-026; 040<-024,025; 041<-027,028; 042<-018,023.
- Dependencias externas: TASK-026 a TASK-028 requieren que app integre sus clientes y consuma las respuestas server; no bloquean las implementaciones server. TASK-036 a TASK-042 son del documentador-server, al cierre, tras las entregas server y la integración app de TASK-026 a TASK-028.

## Origen SDD

- Requirements: 1
- Design: 1
- Tasks: 1

## Enfoque técnico

Se conservará el comportamiento de cuentos antiguos de FUN-004, reutilizando solo las piezas compatibles que identifique TASK-001. El flujo nuevo se separará por puertos de biblioteca de voces, narrador, síntesis, persistencia y eventos, y se activará exclusivamente en cuentos con escaleta. La publicación de audios será atómica: los clips se preparan fuera de la vista pública y se exponen junto con el nombre copiado de la voz solo al completar todas las páginas interiores. `GenerationGate` seguirá siendo el árbitro global, y el servicio de páginas consultará la narración antes de mutar páginas. Las rutas REST y el formato de eventos se implementan primero; los contratos se formalizan por el documentador contra esas interfaces y después de la integración app.

## Cambios por tarea

### TASK-001 Verificar piezas de FUN-004 reutilizables

- **Ficheros:** `api/src/main/java/.../PageAudioAssetGenerator.java`, `AudioStoryGeneration`, `VoiceLibraryPort`, `AudioGeneratorPort` y recursos de prompts (revisar, sin cambiar salvo corrección imprescindible); anotación de decisión en `specs/agent-narrator/tasks.md` por el responsable autorizado.
- **Enfoque:** inventariar adaptador Chatterbox, flujo de audio y posible Modelfile/agente narrator; fijar qué se encapsula para el flujo con escaleta y qué queda intacto para cuentos antiguos.
- **Pruebas automáticas:** ejecutar las pruebas existentes de audio heredado; `mvn -f api/pom.xml test`.
- **Prueba manual:** reproducir audio de un cuento antiguo y comprobar que no aparece el panel/estado nuevo.

### TASK-002 Reglas de onomatopeyas en el Modelfile del escritor

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/WriterModelProvisioner.java` y su recurso Modelfile de escritor (modificar); prueba de provisionador/recurso (modificar).
- **Enfoque:** añadir las cuatro reglas PDEC-030 al recurso mantenido por backend, sin alterar la semántica de aprovisionamiento existente.
- **Pruebas automáticas:** recurso con máximo una, condicionado al beat, forma pronunciable y prohibición en portada; ausente crea/presente no recrea; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-003.

### TASK-003 Instrucciones de página y portada con reglas de onomatopeyas

- **Ficheros:** `api/src/main/java/.../SpringAiWriterAgent.java`, plantillas de página/portada y pruebas del adaptador (modificar).
- **Enfoque:** inyectar reglas ligadas al beat para interior y prohibición explícita para portada, preservando objetivo de palabras.
- **Pruebas automáticas:** capturar prompts de interior/portada y verificar reglas, beat y límite; `mvn -f api/pom.xml test`.
- **Prueba manual:** generar beat sonoro y silencioso y portada; validar forma, cardinalidad y ausencia donde corresponde.

### TASK-004 Onomatopeya contada como palabra en el aviso de longitud

- **Ficheros:** `api/src/main/java/.../domain/service/PageLengthValidator.java` (sin cambio esperado); `PageLengthValidatorTest.java` (modificar).
- **Enfoque:** confirmar que la tokenización vigente incluye la onomatopeya como palabra y no cambiar el validador si ya cumple.
- **Pruebas automáticas:** «¡Guau!» y límites exacto/excedido; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-005 Catálogo de idiomas de voz

- **Ficheros:** `api/src/main/java/.../domain/model/VoiceLanguage.java` y `application/service/VoiceLanguageCatalog.java` (nuevos, propuesta); pruebas unitarias (nuevas).
- **Enfoque:** catálogo fijo de Chatterbox V3 con español por defecto y predicado de admisibilidad.
- **Pruebas automáticas:** español/default, idioma permitido, vacío y no permitido; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-006 Inspección del audio de voz subido

- **Ficheros:** `application/service/VoiceAudioInspector.java` (nuevo, propuesta), puerto/adaptador de metadatos de audio si es necesario y pruebas (nuevos).
- **Enfoque:** detectar contenido real WAV/MP3 y duración inclusiva 5..30 s, devolviendo causa diferenciada sin confiar en extensión.
- **Pruebas automáticas:** WAV/MP3 válidos, OGG/texto falso, 4/31 y extremos 5/30; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-027/app.

### TASK-007 Persistencia de voces subidas

- **Ficheros:** puerto `VoiceRepository`, entidades/repositorios/mappers JPA de voz y migración Liquibase siguiente disponible (nuevos, propuesta); pruebas de adaptador (nuevas).
- **Enfoque:** persistir nombre único, idioma y permiso; listar, buscar y borrar sin enlazar narraciones históricas.
- **Pruebas automáticas:** CRUD, búsqueda y restricción única; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-008 Almacén del audio de referencia de voces subidas

- **Ficheros:** puerto `VoiceSampleStore` y adaptador sobre `ResourceStorage` (nuevos, propuesta); pruebas (nuevas).
- **Enfoque:** almacenar, recuperar idéntico y eliminar muestra bajo el identificador de voz, con ausencia explícita.
- **Pruebas automáticas:** guardar/leer/eliminar; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-009 Voz del sistema desde la configuración

- **Ficheros:** `application/service/SystemVoiceProvider.java`, propiedades tipadas de `application.yaml` y validación de arranque (nuevos/modificar); pruebas de contexto (nuevas).
- **Enfoque:** leer nombre y muestra sin fallback; disponibilizar motivo de ausencia y detectar colisión con voz subida sin impedir arranque.
- **Pruebas automáticas:** configuración válida, cambio al reiniciar contexto, ausente/inválida y colisión; `mvn -f api/pom.xml test`.
- **Prueba manual:** arrancar sin voz configurada.

### TASK-010 Listado de la biblioteca y subida de voces

- **Ficheros:** `application/service/VoiceLibraryService.java`, comandos/respuestas/puertos de entrada (nuevos, propuesta) y pruebas (nuevas).
- **Enfoque:** coordinar catálogo, inspector, repositorio, almacén y voz de sistema; validar todo antes de persistir para que un rechazo no deje registro ni asset.
- **Pruebas automáticas:** listado con/sin sistema, WAV/MP3 y todos los rechazos ALT-001 con compensación; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-027 y app.

### TASK-011 Borrado de voces y voces elegibles para narrar

- **Ficheros:** `VoiceLibraryService.java` y pruebas (modificar); puerto de muestra (modificar si procede).
- **Enfoque:** borrar registro+muestra de subida, proteger sistema y devolver solo sistema disponible y subidas españolas con su muestra.
- **Pruebas automáticas:** borrado, protección, idioma, ausencia del sistema y persistencia de narración histórica; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por app.

### TASK-012 Persistencia de la narración del cuento

- **Ficheros:** puerto `NarrationRepository`, entidades/mappers/repositorios JPA `StoryNarration`/audio por página, migración Liquibase y pruebas (nuevos, propuesta).
- **Enfoque:** estado, copia de voz y audios interiores con operación transaccional de publicación/borrado; prohibir portada y limpiar voz/audio ante error.
- **Pruebas automáticas:** curso, publicación de ocho atómica, portada rechazada, borrado, error y copia de voz; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-013 Modelfile y provisionador del narrador

- **Ficheros:** `resources/prompts/narrator.modelfile` y `infrastructure/config/NarratorModelProvisioner.java` (nuevos o reutilizados según TASK-001); pruebas (nuevas/modificar).
- **Enfoque:** provisionar solo si falta un modelo que segmenta texto sin modificar palabras y devuelve expresividad por fragmento.
- **Pruebas automáticas:** presente/ausente y contenido contractual del recurso; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-031.

### TASK-014 Agente narrador

- **Ficheros:** puerto `NarratorAgent`, modelos de guion/fragmento y adaptador Ollama (nuevos o reutilizados según TASK-001); pruebas (nuevas).
- **Enfoque:** enviar interiores ordenados, deserializar guion completo y convertir respuesta/Ollama inválidos en error de dominio.
- **Pruebas automáticas:** ocho textos sin portada, respuesta válida y respuestas incompletas/malformadas/fallo; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-031.

### TASK-015 Validador del guion de audio

- **Ficheros:** `domain/service/AudioScriptValidator.java` y pruebas (nuevos, propuesta).
- **Enfoque:** normalizar solo mayúsculas y puntuación y comparar secuencia de palabras por página, señalando el número fallido.
- **Pruebas automáticas:** igualdad, puntuación, onomatopeya, añadir/quitar/reordenar/sustituir y fallo en una de ocho; `mvn -f api/pom.xml test`.
- **Prueba manual:** no aplica.

### TASK-016 Adaptador de síntesis de Chatterbox

- **Ficheros:** adaptador de `AudioGeneratorPort`/`ChatterboxPort`, `AudioGenerationRequest` y pruebas (modificar o extraer según TASK-001).
- **Enfoque:** aislar llamada V3 con español, muestra y parámetros por fragmento; mapear vacío, timeout y fallo a error explícito sin cambiar FUN-004.
- **Pruebas automáticas:** petición completa, éxito, motor/error/timeout/vacío; `mvn -f api/pom.xml test`.
- **Prueba manual:** sintetizar fragmento con contenedor Chatterbox.

### TASK-017 Notificador de eventos de narración

- **Ficheros:** `domain/events/NarrationEvent.java`, ampliación de `WebsocketUseCase` y notificador de aplicación (nuevos/modificar); pruebas (nuevas).
- **Enfoque:** modelar STARTED, COMPLETED con voz y FAILED siempre con storyId.
- **Pruebas automáticas:** publicar los tres eventos y campos; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-018.

### TASK-018 Integración: notificador de narración con websocket-adapter

- **Ficheros:** `infrastructure/adapters/in/websocket/WebsocketAdapter.java` y pruebas de websocket (modificar/nuevas).
- **Enfoque:** emitir `NarrationEvent` por `/storyteller` sin modificar la difusión de retratos/páginas.
- **Pruebas automáticas:** suscriptor recibe tres eventos y regresión de eventos existentes; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por app.

### TASK-019 Estado de narración del cuento y borrado del audio a petición

- **Ficheros:** `application/service/StoryNarrationService.java`, puertos/respuestas de estado y pruebas (nuevos, propuesta).
- **Enfoque:** calcular habilitación desde escaleta y páginas aceptadas actuales; exponer consultas internas de curso/audio y borrado conjunto.
- **Pruebas automáticas:** cuento antiguo, páginas incompletas, portada pendiente, curso, audio/voz y borrado; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por app.

### TASK-020 Inicio de la narración y la renarración

- **Ficheros:** `StoryNarrationService.java`, comandos y puertos a `GenerationGate`, voces y eventos (modificar/nuevos); pruebas (nuevas).
- **Enfoque:** validar antes de adquirir turno; al concederlo capturar muestra, borrar previo, marcar curso y publicar inicio. Todo rechazo mantiene audio previo.
- **Pruebas automáticas:** inicio/renarración, puerta ocupada, sin voces, no elegible, escaleta/páginas/curso inválidos; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por TASK-031.

### TASK-021 Ejecución de la narración en segundo plano

- **Ficheros:** `StoryNarrationService.java`, ejecutor configurado, ensamblador de audio y pruebas de orquestación (modificar/nuevos).
- **Enfoque:** narrar, validar y sintetizar en memoria; publicar todos los audios atómicamente solo al final. En cualquier fallo limpiar, marcar error, notificar y liberar puerta siempre.
- **Pruebas automáticas:** ocho audios, no parcial/no guion persistido/texto inmutable, fallos de narrador-validador-página 4, renarración fallida y borrado concurrente de voz; `mvn -f api/pom.xml test`.
- **Prueba manual:** extremo a extremo desde app.

### TASK-022 Descarga del audio de una página interior

- **Ficheros:** `StoryNarrationService.java`, puerto REST/respuesta binaria posterior TASK-028 y pruebas (modificar/nuevos).
- **Enfoque:** resolver exclusivamente audio interior publicado y responder no encontrado para portada/ausencia.
- **Pruebas automáticas:** página 2, portada/sin audio y voz eliminada; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por reproductor app.

### TASK-023 Reconciliación de narraciones al arrancar

- **Ficheros:** `NarrationReconciliationService.java` y configuración de arranque/pruebas (nuevos, propuesta).
- **Enfoque:** convertir registros en curso a error, limpiar material parcial, notificar y no tocar terminadas; la puerta no se persiste.
- **Pruebas automáticas:** curso, terminada y nueva adquisición tras reconciliar; `mvn -f api/pom.xml test`.
- **Prueba manual:** reiniciar durante una narración.

### TASK-024 Bloqueo de páginas durante la narración y borrado del audio al reabrir

- **Ficheros:** `application/service/StoryPageService.java` y pruebas; puertos a `StoryNarrationService` (modificar).
- **Enfoque:** rechazar mutaciones interiores/invalidadas durante curso; tras éxito de mutación, borrar narración en la misma unidad transaccional, nunca antes.
- **Pruebas automáticas:** reapertura/cambio/invalida bloqueados, cascada con audio, rollback conservando audio y sin audio; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por app.

### TASK-025 Aviso de borrado de audio al invalidar páginas por un beat

- **Ficheros:** `application/service/BeatEditorService.java`, respuesta de confirmación y pruebas (modificar).
- **Enfoque:** ampliar aviso solo si hay audio y reutilizar protección de narración en curso; confirmar borra audio junto a invalidación.
- **Pruebas automáticas:** con/sin audio, confirmación y curso; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubierta por app.

### TASK-026 Integración: vista de páginas con audio y narración en curso (porción server)

- **Ficheros:** respuestas/controladores que alimentan la vista y detalle de páginas, `StoryPageService.java` y pruebas REST (modificar; nombres concretos según DTO vigente).
- **Enfoque:** añadir `hasAudio`, `narrating` y audio por interior, sin audio para portada, y mantener el conflicto de narración con su motivo para el cliente.
- **Pruebas automáticas:** cuento narrado/en curso, portada y propagación 409; `mvn -f api/pom.xml test`.
- **Prueba manual:** la parte cliente se verifica en TASK-033/034.

### TASK-027 Integración: cliente de biblioteca de voces con el servidor (porción server)

- **Ficheros:** `infrastructure/adapters/in/rest/web/VoiceLibraryController.java`, DTOs y mapeadores REST (nuevos, propuesta); pruebas controller (nuevas).
- **Enfoque:** exponer listado, idiomas, multipart de subida, elegibles y borrado; traducir errores de servicio a respuestas estables sin anticipar el contrato documental.
- **Pruebas automáticas:** rutas de éxito y cada ALT-001, sistema no borrable y filtrado elegible; `mvn -f api/pom.xml test`.
- **Prueba manual:** integración app TASK-030.

### TASK-028 Integración: cliente de narración con el servidor (porción server)

- **Ficheros:** `StoryNarrationController.java`, DTOs de iniciar/estado y descarga de recurso, pruebas REST (nuevos, propuesta).
- **Enfoque:** publicar inicio/renarración asíncrono, estado y audio interior, preservando causas de ALT-003 a ALT-005 y ausencia de audio.
- **Pruebas automáticas:** inicio, rechazos, todos los campos de estado y descarga/404; `mvn -f api/pom.xml test`.
- **Prueba manual:** integración app TASK-031/033.

### TASK-036 Contrato: openapi - operaciones de la biblioteca de voces

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/paths/voice/` (nuevos) y raíz OpenAPI (modificar).
- **Enfoque y pruebas:** formalizar rutas implementadas, multipart y rechazos de formato/duración/idioma/permiso/nombre/sistema; validar OpenAPI y contrastar con controller.
- **Prueba manual:** comparar todas las operaciones con la implementación final.

### TASK-037 Contrato: openapi - operaciones de narración

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/paths/story/` (nuevos/modificar).
- **Enfoque y pruebas:** documentar inicio, estado y descarga con conflictos y 404 realmente implementados; validar OpenAPI y contrastar controller.
- **Prueba manual:** comparar operaciones de narración con backend final.

### TASK-038 Contrato: openapi - esquemas de voz, biblioteca, idioma y narración

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/schemas/` (nuevos).
- **Enfoque y pruebas:** esquemas de voz/biblioteca/idioma/estado, incluidos resultado, motivo, curso, audio y voz; validar referencias OpenAPI.
- **Prueba manual:** contrastar cada campo de respuesta.

### TASK-039 Contrato: openapi - audio y narración en la vista de páginas

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/openapi/schemas/story-pages-view.yaml` y `page-detail.yaml` (modificar).
- **Enfoque y pruebas:** reflejar `hasAudio`, `narrating` y audio interior, incluida excepción de portada; validar OpenAPI.
- **Prueba manual:** contrastar respuestas reales.

### TASK-040 Contrato: openapi - borrado de audio y rechazo por narración en páginas y beats

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** paths de reapertura/cambio y `beat-edit-service.yaml` (modificar).
- **Enfoque y pruebas:** describir borrado derivado y 409 de narración según handlers finales; validar OpenAPI.
- **Prueba manual:** contrastar mutaciones y error.

### TASK-041 Contrato: openapi - registro de las operaciones de voces y narración

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** documento raíz `docs/contracts/openapi/openapi.yaml` y referencias de paths/schemas (modificar).
- **Enfoque y pruebas:** registrar rutas y componentes de TASK-036 a TASK-040 en el índice OpenAPI sin referencias huérfanas; validar el documento completo.
- **Prueba manual:** comprobar descubribilidad desde la raíz.

### TASK-042 Contrato: websocket - eventos de narración

- **Responsable de ejecución:** documentador-server, al final; **Ficheros:** `docs/contracts/websocket/` (nuevo/modificar, propuesta).
- **Enfoque y pruebas:** formalizar canal `/storyteller`, discriminadores STARTED/COMPLETED/FAILED, storyId y voz solo en éxito, tras TASK-018/023; validar la convención contractual vigente.
- **Prueba manual:** comparar payload de una suscripción real.

## Orden de implementación

1. TASK-001, TASK-004, TASK-005, TASK-006, TASK-007, TASK-008, TASK-012, TASK-015 y TASK-017 (habilitadas por: sin dependencias).
2. TASK-002; TASK-009; TASK-013; TASK-019; TASK-023 (habilitadas por: sus dependencias internas entregadas).
3. TASK-003, TASK-010, TASK-011, TASK-014, TASK-016 y TASK-018 (habilitadas por: capas previas respectivas).
4. TASK-020, TASK-022 y TASK-024; después TASK-021 y TASK-025 (habilitadas por: servicios, puerta y validadores entregados).
5. Porción server de TASK-026, TASK-027 y TASK-028 (habilitada por: servicios server cerrados; entregar interfaces a app sin esperar sus pantallas).
6. TASK-036 a TASK-042 (habilitadas por: implementación server terminada y cierre de integración app TASK-026 a TASK-028; las ejecuta documentador-server).

## Riesgos y dudas técnicas

- La inspección de duración/formato exige una dependencia o utilidad capaz de leer metadatos reales: seleccionar la ya presente en `api/pom.xml` o añadir la mínima al implementar.
- La migración de voz/narración se numera al implementar después de la última migración real; no se fija un número en este plan.
- FUN-004 conserva rutas y comportamiento antiguos: TASK-001 debe documentar reutilización antes de sustituir piezas heredadas.
- Los nombres y cuerpos REST son propuestas internas hasta TASK-027/028; los contratos solo se redactan contra la interfaz integrada.

## Validación

- Suite del módulo: correcta (origen: confirmación "success" del usuario tras ejecutar la suite completa, incluido contextLoads, con TASK-024 entregada; alcance: `mvn -f api/pom.xml test`); correcta también tras TASK-021 (origen: confirmación "success" del usuario tras ejecutar la suite completa con TASK-021 entregada; alcance: `mvn -f api/pom.xml test`)
- Integración: pendiente
- Validación manual: correcta solo para TASK-001 (origen: confirmación "success" del usuario; alcance: cuento antiguo con audio sin cambios y sin panel nuevo); no aplica para TASK-004, TASK-005, TASK-015 ni TASK-017; TASK-013, TASK-014, TASK-018, TASK-019 y TASK-020 quedan cubiertas por la prueba manual de TASK-031 (pendiente); TASK-010 se probará desde la vista (TASK-030), pendiente; TASK-011 no aplica aparte, se probará desde TASK-030/031 (pendiente); PENDIENTE (migraciones 026 y 027): comprobación contra PostgreSQL de tablas, restricciones `uq_voice_name`, `uq_story_narration_audio_page`, `ck_story_narration_audio_interior` y claves foráneas (el usuario no la ha confirmado); PENDIENTE (TASK-003, que cubre TASK-002): generar un beat sonoro, uno silencioso y la portada y comprobar las onomatopeyas (el usuario no ha confirmado explícitamente la prueba manual); PENDIENTE (TASK-016): prueba manual con el arnés ChatterboxSpeechSynthesisManualHarness contra el contenedor, ruta `/audio/speech/upload` y campos asumidos (el usuario no ha confirmado explícitamente esta prueba); PENDIENTE (TASK-009): arrancar la aplicación sin voz del sistema configurada (el usuario no ha confirmado explícitamente esta prueba); DIFERIDA (TASK-023): reiniciar durante una narración, hasta disponer de TASK-020/021/031 (pendiente, no correcta); PENDIENTE (TASK-022): se probará desde el reproductor (TASK-033); PENDIENTE (TASK-024): se probará desde la vista (TASK-034); PENDIENTE (TASK-021): la prueba de extremo a extremo se hará desde la app (TASK-031); el resto, pendiente. Decisión vigente (el usuario no la objetó): el audio se borra al lanzar la pista (startHint), no en acceptProposal
