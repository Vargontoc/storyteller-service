# Sprint 01: character-inventory - server

Estado: aprobado

## Alcance

- Tareas: TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006 (de `specs/character-inventory/tasks.md`).
- Dependencias internas: TASK-003 depende de TASK-001 y TASK-002; TASK-004 depende de TASK-002 y TASK-003; TASK-005 depende de TASK-001, TASK-002 y TASK-003; TASK-006 depende de TASK-004 y TASK-005.
- Dependencias externas: Ninguna por el reparto autorizado. TASK-009 a TASK-011 quedan para `app` después de entregar TASK-003 a TASK-006.

## Origen SDD

- Requirements: 1
- Design: 1
- Tasks: 1

## Enfoque técnico

Se separará el catálogo maestro de personajes de los `Actor` ligados a `Story`, conservando estos últimos como instantáneas de los cuentos ya creados. El caso de uso del catálogo se expondrá mediante puertos de entrada/salida propios, persistencia JPA y migraciones Liquibase; el controlador REST implementará las operaciones que luego formalizarán los contratos de TASK-009 a TASK-011.

El agente de personaje autorizado para este módulo se integrará como configuración, cliente y plantillas Spring AI en `api/`, sin crear ni modificar `agents/character-agent/Modelfile`. Las validaciones de los resultados del agente y de los comandos usarán `framework-core` 0.2.0-SNAPSHOT (`Validator`, `ValidationGuard`, usos 1 a 3); las operaciones de escritura, el mapeo y sus pruebas adoptarán respectivamente `WriteTransactionGuard` (usos 1 y 2), `EntityMapperGuard` (usos 1 y 2), `AutoIdJpaEntity` (uso 1), `InMemoryRepository` (usos 1 a 3) y `FakeChatModel` (usos 1 a 3) cuando correspondan. Esto requiere incorporar las dependencias del framework y su repositorio Maven antes de compilar.

El puerto de imágenes solo consulta condiciones; ni el adaptador, ni la generación, ni la selección, ni el almacenamiento de assets pertenecen a este sprint. Se mantiene KISS/YAGNI: DTOs y puertos mínimos para los flujos definidos, sin anticipar sincronización de snapshots ni gestión de retratos.

## Cambios por tarea

### TASK-001 Definir el agente de personaje y su contrato de salida textual

- **Ficheros:** `api/pom.xml` (modificar): declarar los módulos `framework-ai`, `framework-ai-testing`, `framework-core`, `framework-json` y `framework-testing` en versión `0.2.0-SNAPSHOT`, junto con la configuración Maven indicada por `CONSUMIR.md`; `api/src/main/resources/application.yaml` (modificar): registrar el modelo configurable del personaje y las reglas compartidas de edad y especies; `api/src/main/java/es/vargontoc/storyteller/shared/Constants.java` y `infrastructure/config/AgentsConfig.java` (modificar): declarar los nombres y el `ChatClient` cualificado del agente; `infrastructure/adapters/out/external/OllamaAdapter.java` (modificar): incluir el modelo en la comprobación y descarga de agentes; `api/src/main/resources/prompts/character_generate.st` y `character_review.st` (nuevos): prompts de alta automática/manual y revisión; `domain/response/CatalogCharacterAgentResult.java` y `CatalogCharacterReviewAgentResult.java` (nuevos): contratos de salida tipados; `application/ports/out/external/CharacterAgent.java` y `infrastructure/adapters/out/external/SpringAiCharacterAgent.java` (nuevos, propuesta): puerto/adaptador que aísla Spring AI y entrega salida tipada; pruebas en `api/src/test/java/.../infrastructure/adapters/out/external/` (nuevas).
- **Enfoque:** mover al servidor, por autorización expresa, la responsabilidad de llamar a un modelo Ollama ya cargado. El adaptador construirá peticiones inmutables con `AgentRequest` y resolverá una salida tipada con el contrato mínimo: aceptación, motivo de rechazo si aplica, nombre, descripciones narrativa y visual, y metadatos visuales. Centralizará la comprobación de disponibilidad antes de invocar; no duplicará el agente director ni confiará en respuestas incompletas. `FakeChatModel` guionizará respuestas válidas, rechazo y respuesta inválida sin requerir Ollama real.
- **Pruebas automáticas:** `CharacterAgentAdapterTest`: alta automática y manual válidas devuelven los tres campos obligatorios; entrada rechazada devuelve motivo y no ficha aplicable; revisión válida devuelve propuesta; JSON/campos ausentes o fallo del proveedor no se transforma en ficha. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** con el modelo configurado en Ollama, solicitar una ficha automática y una manual apta y confirmar nombre más ambas descripciones; enviar una frase no apta y confirmar únicamente el motivo de rechazo. Confirmar que no se modifica ningún Modelfile de `agents/`.

### TASK-002 Definir el puerto de condiciones visuales de personaje

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterImageConditionPort.java` (nuevo, propuesta); pruebas de contrato del consumidor en `api/src/test/java/.../application/service/` (nuevas).
- **Enfoque:** definir dos consultas booleanas por identificador de `CatalogCharacter`: retrato aprobado e imágenes generadas. El puerto no expondrá comandos ni tipos de persistencia/ComfyUI. Al no existir todavía proveedor de assets, los servicios dependerán del puerto y las pruebas suministrarán un doble explícito; no se añadirá implementación productiva ni valor por defecto que oculte la ausencia de assets.
- **Pruebas automáticas:** pruebas unitarias del consumidor con dobles para ambas combinaciones de condiciones, verificando que puede calcular borrador/aviso sin invocar operaciones de escritura. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** inspeccionar el puerto y confirmar que expone exclusivamente ambas lecturas, por id de catálogo, sin generar, elegir, guardar ni modificar imágenes.

### TASK-003 Implementar el alta y la validación de fichas en el catálogo

- **Ficheros:** `api/src/main/resources/db/changelog/migrations/014_create_catalog_character.xml` y `015_create_catalog_character_review.xml` (nuevos, propuesta): tablas independientes para maestro y revisiones, sin `story_id`; `domain/model/CatalogCharacter.java`, `CatalogCharacterReview.java` y comandos de alta (nuevos); `application/ports/in/character/CharacterCatalogUseCase.java`, `application/ports/out/persistence/CatalogCharacterRepository.java`, `application/service/CharacterCatalogService.java` (nuevos); entidades, repositorios Spring Data, mappers y adaptadores equivalentes bajo `infrastructure/persistence`, `infrastructure/mappers` e `infrastructure/adapters/out/persistence` (nuevos); validador del contrato en `infrastructure/validations/CatalogCharacterAgentResultValidator.java` (nuevo); DTOs y controlador de catálogo bajo `infrastructure/adapters/in/rest/` (nuevos); pruebas unitarias y de persistencia correspondientes (nuevas).
- **Enfoque:** recibir modo automático o frase manual, delegar en `CharacterAgent`, pasar la salida por `Validator` y `ValidationGuard`, y persistir solo una ficha aceptada, completa y consistente. La migración crea el catálogo maestro sin migrar automáticamente los `character` históricos: siguen ligados a `story` y son snapshots. Las escrituras del adaptador se protegerán con `WriteTransactionGuard`; el mapeo entidad-dominio con `EntityMapperGuard`; los ids JPA nuevos usarán `AutoIdJpaEntity`. El endpoint devolverá el rechazo con su motivo sin crear entidad; sus detalles se formalizarán posteriormente en TASK-009.
- **Pruebas automáticas:** `CharacterCatalogServiceTest` con `FakeChatModel`/dobles: alta automática aceptada, manual aceptada, rechazo de cada modo sin escritura y salida incompleta sin escritura. `CatalogCharacterRepositoryAdapterTest` o `@DataJpaTest`: persistencia y lectura de los tres campos obligatorios y aislamiento respecto de `CharacterJpaEntity`. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** crear un personaje automático y otro manual apto, comprobando ficha completa; intentar frase no apta y comprobar el motivo sin nueva fila ni entrada en catálogo.

### TASK-004 Implementar el listado, detalle, estado derivado y renombrado del catálogo

- **Ficheros:** `CharacterCatalogUseCase.java`, `CharacterCatalogService.java`, `CatalogCharacterRepository.java`, su adaptador/mapeador/repositorio JPA y el controlador/DTOs de catálogo (modificar); modelos de vista de catálogo y detalle (nuevos, propuesta); migración de índices o restricciones de nombre solo si la consulta lo precisa (nuevo, propuesta); pruebas de servicio, persistencia y web (nuevas).
- **Enfoque:** listar todos los maestros y recuperar uno por id, añadiendo revisión pendiente y `draft = !hasApprovedPortrait(id)` como proyecciones derivadas, nunca como columna persistida. El renombrado escribe únicamente `CatalogCharacter`; no consulta, actualiza ni recrea `CharacterJpaEntity` de cuentos. La lectura de condiciones visuales pasa por TASK-002 y los caminos de escritura se ejecutan en transacción. No se implementa propagación de cambios, reconstrucción de cuentos ni gestión del retrato.
- **Pruebas automáticas:** catálogo vacío y con elementos; detalle completo; retrato ausente/presente cambia el derivado borrador; renombrado actualiza solo el maestro y conserva una entidad `CharacterJpaEntity` histórica sin cambio; revisión pendiente aparece en listado/detalle. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** con dos fichas, revisar nombres, estado y detalle; comprobar estado vacío sin datos; renombrar y verificar el nuevo nombre del catálogo y la preservación de un cuento existente.

### TASK-005 Adaptar el servicio de revisiones al catálogo y al agente de personaje

- **Ficheros:** `application/ports/in/character/CharacterReviewUseCase.java`, `application/service/CharacterReviewService.java`, `application/ports/out/persistence/CatalogCharacterReviewRepository.java` y adaptador JPA (nuevos, propuesta); `CatalogCharacterReview.java`, comandos/resultados de revisión y DTOs REST (nuevos); controlador de revisiones de catálogo (nuevo o modificar `ReviewsController.java`); `CharacterImageConditionPort.java`, `CharacterAgent.java`, prompts y validadores (modificar); pruebas de servicio, persistencia y web (nuevas). `CharacterService.java`, `CharacterReviewRepository.java` y sus adaptadores actuales se mantienen para los actores de cuento.
- **Enfoque:** no reutilizar la revisión actual de `Actor`, pues presupone `storyId`, sinopsis y borra páginas/assets; crear el flujo de catálogo independiente. Ante pendiente existente, devolver la confirmación requerida sin descartarla; solo tras la confirmación descartar la anterior y crear la nueva. El agente valida el hint y propone cambios; un rechazo no crea pendiente. Si el target contiene visual y `hasGeneratedImages(id)` es verdadero, adjuntar el aviso de consistencia; no consultarlo para cambio exclusivamente narrativo. El modelo admite una sola pendiente por catálogo, reforzada por consulta/índice y transacción.
- **Pruebas automáticas:** sin pendiente crea propuesta pendiente; con pendiente exige confirmación y, confirmada, descarta la anterior; hint rechazado no persiste; cambio válido persiste propuesta; aviso aparece solo para cambio visual con imágenes generadas. Añadir carrera de dos peticiones para comprobar que persiste como máximo una pendiente. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** solicitar revisión válida; repetirla y confirmar el reemplazo; enviar cambio no apto; para una ficha con imágenes generadas solicitar cambio visual y comprobar el aviso, y comprobar su ausencia para cambio narrativo.

### TASK-006 Implementar la aceptación y el descarte de revisiones del catálogo

- **Ficheros:** `CharacterReviewUseCase.java`, `CharacterReviewService.java`, `CatalogCharacterReviewRepository.java`, adaptador JPA y controlador/DTOs de revisiones (modificar); pruebas de servicio y persistencia (nuevas).
- **Enfoque:** recuperar exclusivamente la revisión pendiente del catálogo. Al aceptar, aplicar solo las propiedades indicadas por el target al maestro y marcar la revisión `CONFIRMED` en una misma transacción; al descartar, conservar la ficha y marcar `DISCARDED`. Rechazar ids inexistentes o revisiones no pendientes con el error del framework normalizado por el límite web. No tocar personajes/snapshots de cuentos ni crear nuevas imágenes.
- **Pruebas automáticas:** aceptación narrativa actualiza solo narrativa y confirma; aceptación visual actualiza solo visual y confirma; descarte no modifica nombre ni descripciones y descarta; segunda aceptación o descarte de una revisión ya resuelta falla sin cambios. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** aceptar una propuesta narrativa y comprobar texto y desaparición de pendiente; descartar otra y comprobar ficha inalterada y desaparición de pendiente.

## Orden de implementación

1. TASK-001 (habilitada por: configuración Maven del framework 0.2.0-SNAPSHOT y acceso de Maven a GitHub Packages).
2. TASK-002 (habilitada por: sin dependencias; se entrega solo la abstracción).
3. TASK-003 (habilitada por: TASK-001 entrega el `CharacterAgent` probado y TASK-002 el puerto de consultas).
4. TASK-004 (habilitada por: TASK-003 entrega el catálogo persistente y TASK-002 el puerto de retrato aprobado).
5. TASK-005 (habilitada por: TASK-001, TASK-002 y TASK-003 entregadas; las revisiones se anclan al catálogo maestro).
6. TASK-006 (habilitada por: TASK-004 y TASK-005 entregadas; existe ficha maestra y una revisión pendiente).
7. Hand-off a `app`: TASK-007 a TASK-011 se habilitan cuando TASK-003 a TASK-006 expongan y prueben sus operaciones REST; TASK-009 a TASK-011 formalizan el contrato posteriormente según reparto autorizado.

## Riesgos y dudas técnicas

- El diseño y TASK-001 ubican el Modelfile en `agents/`, pero el reparto autorizado traslada su responsabilidad al servidor y prohíbe tocarlo. Mitigación: el sprint solo integra la llamada al modelo ya cargado mediante configuración/prompt/adaptador en `api/`; debe alinearse `design.md` y `tasks.md` en la siguiente revisión SDD para reflejarlo.
- `api/pom.xml` no declara `es.vargontoc.framework`; el uso solicitado exige fijar `0.2.0-SNAPSHOT`, declarar el repositorio/credenciales descritos en `CONSUMIR.md` y verificar resolución antes de implementar. Si no se resuelve, no se deben sustituir las APIs de la guía por equivalentes propios.
- La separación de catálogo y snapshot exige nuevas tablas y no permite reutilizar `CharacterJpaEntity`, que tiene `story_id` no nulo. Mitigación: tablas y puertos de catálogo independientes; los datos históricos se preservan y una migración de adopción de protagonistas existentes queda fuera hasta que exista una regla de correspondencia aprobada.
- Assets aún no provee adaptador de `CharacterImageConditionPort`. Mitigación: doble en pruebas y dependencia de puerto; el despliegue necesita un proveedor real antes de que los estados visuales sean fiables.

## Validación

- Suite del módulo: pendiente (prueba específica TASK-001 correcta, confirmada por el usuario: `mvn -U test -Dtest=CharacterAgentAdapterTest`)
- Integración: pendiente
- Validación manual: pendiente
