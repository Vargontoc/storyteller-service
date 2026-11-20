# Sprint 03: agent-director - server

Estado: aprobado

## Alcance

- Tareas: TASK-001 a TASK-009 y TASK-014 a TASK-018 (de `specs/agent-director/tasks.md`).
- Dependencias internas: TASK-004 depende de TASK-003; TASK-005 de TASK-001, TASK-003 y TASK-004; TASK-006 de TASK-004; TASK-007 de TASK-001, TASK-003, TASK-005 y TASK-006; TASK-008 de TASK-003, TASK-004 y TASK-007; TASK-009 de TASK-008; TASK-014 de TASK-003, TASK-004 y TASK-005; TASK-016 de TASK-006; TASK-017 de TASK-005, TASK-007 y TASK-008; TASK-018 de TASK-008.
- Dependencias externas: TASK-015 (app, TASK-010): la interfaz de selección de protagonista y elección aleatoria debe estar implementada y su intercambio con backend definido antes de formalizar su contrato. La retirada física de `agents/director-story-agent/` y de su entrada en `agents/load_model.ps1` corresponde al módulo `agents`, que queda deprecado durante la migración; este sprint solo entrega la sustitución backend y la condición para coordinar dicha retirada.

## Origen SDD

- Requirements: 2
- Design: 1
- Tasks: 1

## Enfoque técnico

El backend concentrará la definición y el ciclo de vida del director: el nuevo recurso `api/src/main/resources/prompts/director.modelfile` será la fuente mantenida por `DirectorModelProvisioner`, siguiendo el patrón de `CharacterModelProvisioner`. Se extenderán los modelos, puertos, adaptadores JPA y migraciones Liquibase para preservar el guion contractual sin migrar cuentos anteriores. Los servicios existentes conservarán la orquestación y delegarán en puertos específicos para la escaleta, revisiones y edición de beats; la generación de páginas consumirá exclusivamente el beat aprobado. Se retirará el descubrimiento de personajes del flujo de páginas porque el reparto episódico no pertenece al catálogo. Las tareas de contrato se ejecutarán al final, por el documentador, contra la implementación entregada y tras releer los contratos vigentes.

## Cambios por tarea

### TASK-001 Redefinir el agente director para generar guion completo con escaleta contractual

- **Ficheros:** `api/src/main/resources/prompts/director.modelfile` (nuevo): definición cargable del director, con instrucciones y parámetros para generación, revisión global y validación de edad; `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/` (modificar, adaptador del director existente o nuevo): deserialización tipada y validación del resultado; `api/src/test/java/.../infrastructure/adapters/out/external/DirectorAgentAdapterTest.java` (nuevo).
- **Enfoque:** el contrato de generación contendrá título, sinopsis, reparto ligero y un beat por página interior; cada beat exigirá hecho, función narrativa, participantes, emoción/tono y nota visual. Para hints, exigirá escaleta completa del mismo cardinal que la vigente, aceptación y motivo de rechazo cuando aplique; para edición devolverá veredicto y motivo. El destino de mantenimiento es el recurso backend, no `agents/director-story-agent/Modelfile`.
- **Pruebas automáticas:** pruebas del adaptador/validador con respuesta completa, beat incompleto rechazado, revisión con cardinal conservado, hint rechazado con motivo y edición no apta; `mvn -f api/pom.xml test`.
- **Prueba manual:** con el modelo aprovisionado por backend, solicitar generación, hint admisible y hint no apto; comprobar los campos y los veredictos.

### TASK-002 Aprovisionar el modelo del director desde los recursos del backend

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisioner.java` (nuevo); `api/src/main/java/es/vargontoc/storyteller/shared/Constants.java` (modificar, si precisa bean/nombre); `api/src/main/resources/application.yaml` (modificar, propiedades `director-*-provisioning`); `api/src/test/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisionerTest.java` y `DirectorModelProvisionerContextTest.java` (nuevos).
- **Enfoque:** leer el Modelfile de classpath, consultar Ollama al arranque y crear `storyteller-director` solo si no existe, enviando el contenido como definición de modelo. Fallos de consulta o creación deben abortar el arranque con causa clara, como el provisionador de personaje. No se modifican rutas de `agents/`: al quedar esta sustitución probada, se comunica al responsable `agents` que retire la carpeta antigua y la llamada de `load_model.ps1` sin afectar topics, writer ni narrator.
- **Pruebas automáticas:** modelo ausente crea con nombre y contenido del recurso; modelo presente no crea; recurso resuelto desde classpath; constructor inyectable y fallo de Ollama propaga error; `mvn -f api/pom.xml test`.
- **Prueba manual:** arrancar con Ollama sin y con el modelo, verificar el registro único y coordinar la comprobación posterior de que el cargador heredado ya no invoca director.

### TASK-003 Persistir el guion con escaleta, reparto ligero y marcador de versión

- **Ficheros:** `api/src/main/resources/db/changelog/migrations/020_create_story_outline.xml` (nuevo, numeración a confirmar contra migraciones vigentes); `StoryJpaEntity.java`, `StoryRepositoryAdapter.java`, `StoryMapper.java` (modificar); entidades, repositorios y mappers de `StoryOutline`, `StoryBeat` y `EpisodicCastMember` bajo `api/src/main/java/es/vargontoc/storyteller/infrastructure/persistence/` (nuevos); pruebas de persistencia bajo `api/src/test/java/.../infrastructure/adapters/out/persistence/` (nuevas).
- **Enfoque:** crear la estructura relacional ordenada por número de página y el marcador de versión en `story`; tratar la ausencia del marcador/escaleta como cuento antiguo, sin backfill. El reparto ligero se almacena dentro de la escaleta y nunca mediante `CatalogCharacter` ni `CharacterJpaEntity` del catálogo.
- **Pruebas automáticas:** guardar/leer orden y cinco campos, reparto sin alta en catálogo, lectura diferenciada de cuento antiguo/nuevo y no alteración del antiguo; pruebas JPA/Liquibase existentes más `mvn -f api/pom.xml test`.
- **Prueba manual:** consultar un cuento previo y uno recién creado y comprobar convivencia sin migración.

### TASK-004 Definir el modelo de escaleta y el reparto ligero en el dominio

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/domain/model/StoryOutline.java`, `StoryBeat.java`, `EpisodicCastMember.java` y `StoryScriptVersion.java` (nuevos, propuesta); `Story.java` y puertos de persistencia de story (modificar); pruebas unitarias de dominio (nuevas).
- **Enfoque:** modelar valores de escaleta con los cinco campos obligatorios y secondary cast con solo nombre y función; exponer versión y estado de guion sin reinterpretar las listas antiguas de actores. La construcción incompleta deberá ser detectable antes de persistir.
- **Pruebas automáticas:** construcción/consulta válida, incompletitud, ausencia de identificador de catálogo y versión contractual en cuento nuevo; `mvn -f api/pom.xml test`.
- **Prueba manual:** revisar que los tipos no permiten acoplar secundarios al catálogo.

### TASK-005 Modificar el servicio de guion para el protagonista obligatorio y la escaleta

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java` (modificar); puertos y comandos de creación/revisión de story bajo `application/ports/` y `domain/command/` (modificar); adaptador del director, repositorio de catálogo y snapshots (modificar según interfaz vigente); `new_script.st` y `review_script.st` (modificar o sustituir por el recurso contractual); `StoryCatalogCharacterTest.java` y pruebas de `StoryService` (modificar/nuevas).
- **Enfoque:** exigir protagonista de catálogo con retrato aprobado, resolver aleatorio solo sobre disponibles y capturar su instantánea textual/visual; invocar al director con ella. Reintentar respuesta de escaleta inválida hasta el límite actual sin persistir cuento. Añadir aprobación explícita y revisión global: una candidata aceptada queda pendiente, una rechazada conserva vigente y motivo; no volver a usar director para revisar fichas de catálogo.
- **Pruebas automáticas:** selección concreta/aleatoria, ausencia de retratos, snapshot, parámetros al director, rechazo/reintento de beat incompleto, persistencia válida, aprobación e hipótesis de hints aceptado/rechazado sin altas de secundarios; `mvn -f api/pom.xml test`.
- **Prueba manual:** crear con y sin protagonista, aprobar guion y enviar hint no apto verificando que no cambia la escaleta.

### TASK-006 Modificar el repositorio de revisiones para la escaleta candidata

- **Ficheros:** `StoryReviewRepository.java`, `StoryReviewRepositoryAdapter.java`, `StoryReviewJpaEntity.java`, `StoryReviewMapper.java` y repositorio JPA (modificar); migración Liquibase siguiente a la de TASK-003 (nueva); modelos/respuestas de revisión (modificar); pruebas de adaptador JPA (nuevas).
- **Enfoque:** extender `StoryReview` conservando el ciclo PENDING/CONFIRMED/DISCARDED para guardar candidata completa, aceptación, motivo y números de beats realmente distintos frente a vigente. Al confirmar se publica como escaleta actual; al descartar no se cambia la vigente.
- **Pruebas automáticas:** recuperación completa, diff vacío ante igualdad, y transición confirmada/descartada sin pendiente; `mvn -f api/pom.xml test`.
- **Prueba manual:** generar hint y verificar candidata y marcas de cambio.

### TASK-007 Implementar la edición directa de un beat con validación de edad

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java` (nuevo, propuesta); puerto de entrada/comandos/resultados para edición y aviso (nuevos); puertos de escaleta y páginas (modificar); controlador REST correspondiente (modificar); pruebas de servicio/controlador (nuevas).
- **Enfoque:** consultar story y beat, pedir al director solo veredicto de edad y no mutar ante rechazo. Si es válido, determinar antes de aplicar si hay páginas generadas; devolver aviso con contador y exigir confirmación. Con confirmación, eliminar páginas mediante el servicio/repositorio responsable, aplicar política vigente de assets, actualizar beat y dejar `SCRIPT_PENDING`; con cancelación, no hay efectos.
- **Pruebas automáticas:** edición válida/no apta, vuelta a pendiente, aviso, confirmar/cancelar y ausencia de secundarios de catálogo; `mvn -f api/pom.xml test`.
- **Prueba manual:** cubrir edición de guion aprobado con y sin páginas y ambas decisiones del aviso.

### TASK-008 Modificar el servicio de páginas para exigir el guion aprobado y entregar el beat al escritor

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (modificar); puertos/repositorio de páginas y plantilla `new_page.st` (modificar); pruebas de `StoryPageService` (nuevas).
- **Enfoque:** antes de portada o interior exigir `SCRIPT_ACCEPTED`. Para interiores, resolver el beat por número de página y pasarlo íntegro como contrato del escritor; portada sigue usando título, sinopsis y protagonista y no consume beat. Exponer la eliminación de páginas invalidadas como operación interna invocable desde TASK-007 y devolver el número retirado.
- **Pruebas automáticas:** bloqueo de portada/interior pendiente, éxito aceptado, beat correcto por N, portada sin beat y eliminación con contador; `mvn -f api/pom.xml test`.
- **Prueba manual:** intentar portada pendiente, aprobar y generar portada/interior, después confirmar invalidación.

### TASK-009 Eliminar el descubrimiento de personajes y el prompt asociado

- **Ficheros:** `StoryPageService.java` y sus pruebas (modificar); `api/src/main/java/es/vargontoc/storyteller/application/service/DiscoverCharacterService.java` y pruebas asociadas (retirar si quedan sin consumidores); `api/src/main/resources/prompts/discover_characcter.st` (retirar); configuraciones/puertos usados únicamente por dicho flujo (retirar tras verificar referencias).
- **Enfoque:** tras TASK-008, suprimir inyección y llamadas de descubrimiento en generación y confirmación de revisiones de página. Los secundarios pasan a ser contexto narrativo del beat, sin fichas visuales ni entradas de catálogo.
- **Pruebas automáticas:** actualizar pruebas de páginas para confirmar que generar/revisar no invoca descubrimiento ni crea personajes; comprobación de compilación y `mvn -f api/pom.xml test`.
- **Prueba manual:** generar una página con secundarios y verificar que no aparecen en el catálogo.

### TASK-014 Contrato: openapi - esquemas de escaleta, reparto ligero y guion del cuento

- **Ficheros:** `docs/contracts/openapi/schemas/story-outline.yaml`, `story-beat.yaml`, `episodic-cast-member.yaml`, y esquema de story vigente (nuevos/modificar, propuesta); `docs/contracts/openapi/openapi.yaml` (modificar).
- **Enfoque:** tarea del documentador, al final y después de TASK-003 a TASK-005. Formalizar exactamente la representación implementada, incluyendo compatibilidad de cuento antiguo sin escaleta y sin identificador de catálogo en reparto.
- **Pruebas automáticas:** validación OpenAPI configurada en el proyecto; comprobar obligatorios y referencias; comando de validación documentado por el módulo cuando exista.
- **Prueba manual:** contrastar esquema y respuestas backend contra la implementación final.

### TASK-015 Contrato: openapi - comando de generación de cuento con protagonista obligatorio

- **Ficheros:** `docs/contracts/openapi/schemas/requests/story-generate-command.yaml` (modificar) y path de creación vigente si debe describir el rechazo (modificar).
- **Enfoque:** tarea del documentador tras TASK-005 y entrega de TASK-010 de app. Recoger identificador de protagonista y elección aleatoria, además del rechazo sin retratos aprobados, con los nombres/campos efectivamente implementados; no anticipar el contrato antes de la interfaz app-backend acordada.
- **Pruebas automáticas:** validación OpenAPI y aserciones de campos/rechazo configuradas; comando de validación del proyecto.
- **Prueba manual:** comparar peticiones creadas desde app y respuestas reales para protagonista concreto y aleatorio.

### TASK-016 Contrato: openapi - revisión de guion con escaleta candidata

- **Ficheros:** `docs/contracts/openapi/schemas/story-review.yaml` (modificar) y esquemas referenciados (modificar).
- **Enfoque:** tarea del documentador tras TASK-006, diferida al bloque documental final. Describir candidata completa, beats cambiados, aceptación y motivo de rechazo sin alterar el ciclo de estados existente.
- **Pruebas automáticas:** validación OpenAPI y comprobación de referencias/campos requeridos.
- **Prueba manual:** comparar respuesta de hint aceptado y rechazado con contrato.

### TASK-017 Contrato: openapi - paths de guion, aprobación y edición de beat

- **Ficheros:** `docs/contracts/openapi/paths/story/script-generate.yaml`, `script-review.yaml`, `script-approval.yaml`, `beat-edit.yaml` (nuevos, propuesta) y `openapi.yaml` (modificar).
- **Enfoque:** tarea del documentador tras TASK-005, TASK-007 y TASK-008, diferida al final. Registrar método, entrada, salida y errores de generación, hint, consulta/confirmación/descarte, aprobación y edición/invalidación; el rechazo de página con guion pendiente se documenta en el path de páginas afectado.
- **Pruebas automáticas:** validación OpenAPI, referencias desde raíz y cobertura de errores relevantes.
- **Prueba manual:** contrastar todos los endpoints con controladores y flujos finales.

### TASK-018 Contrato: openapi - esquema de página y entrega del beat al escritor

- **Ficheros:** `docs/contracts/openapi/schemas/story-page.yaml` (modificar).
- **Enfoque:** tarea del documentador tras TASK-008, diferida al final. Reflejar solo la página pública resultante; documentar que el beat se entrega internamente al escritor sin exponerlo como campo editable o de respuesta.
- **Pruebas automáticas:** validación OpenAPI y comprobación de que no existe campo público de beat en página.
- **Prueba manual:** contrastar el esquema con la respuesta de página final.

## Orden de implementación

1. TASK-001 (habilitada por: sin dependencias; recurso backend definido en este sprint).
2. TASK-002 (habilitada por: TASK-001 entrega el Modelfile de classpath).
3. TASK-003 (habilitada por: sin dependencias).
4. TASK-004 (habilitada por: persistencia base de TASK-003 disponible).
5. TASK-006 (habilitada por: modelo de escaleta de TASK-004 disponible).
6. TASK-005 (habilitada por: TASK-001, TASK-003 y TASK-004 entregadas).
7. TASK-007 (habilitada por: TASK-001, TASK-003, TASK-005 y TASK-006 entregadas).
8. TASK-008 (habilitada por: TASK-003, TASK-004 y TASK-007 entregadas).
9. TASK-009 (habilitada por: TASK-008 entrega el flujo basado en beats).
10. TASK-014, TASK-016, TASK-017 y TASK-018 (habilitadas por: implementación de sus dependencias cerrada; documentación al final).
11. TASK-015 (habilitada por: TASK-005 y entrega externa de TASK-010 del módulo app; se ejecuta dentro del bloque documental final).

## Riesgos y dudas técnicas

- La migración debe numerarse después del último fichero presente en el momento de implementar; se propone `020` solo según el estado analizado.
- El Modelfile heredado sigue existiendo hasta la retirada coordinada por `agents`; no debe volver a ser fuente de ejecución tras validar TASK-002.
- La estructura exacta de mensajes REST y nombres definitivos de nuevos paths se formaliza después de implementar; las rutas propuestas son guía técnica, no contrato anticipado.
- La eliminación física de assets de páginas se delega en la política de producto existente; la operación debe permanecer transaccional respecto al beat y no aplicar cambios ante cancelación.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
