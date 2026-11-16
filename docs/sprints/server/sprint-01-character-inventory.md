# Sprint 01: character-inventory - server

Estado: aprobado

## Alcance

- Tareas: TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006 (de `specs/character-inventory/tasks.md`).
- Dependencias internas: TASK-003 depende de TASK-001 y TASK-002; TASK-004 depende de TASK-002 y TASK-003; TASK-005 depende de TASK-001, TASK-002 y TASK-003; TASK-006 depende de TASK-004 y TASK-005.
- Dependencias externas: Ninguna por el reparto autorizado. TASK-009 a TASK-011 quedan para `app` despuÃ©s de entregar TASK-003 a TASK-006.

## Origen SDD

- Requirements: 1
- Design: 1
- Tasks: 1

## Enfoque tÃ©cnico

Se separarÃ¡ el catÃ¡logo maestro de personajes de los `Actor` ligados a `Story`, conservando estos Ãºltimos como instantÃ¡neas de los cuentos ya creados. El caso de uso del catÃ¡logo se expondrÃ¡ mediante puertos de entrada/salida propios, persistencia JPA y migraciones Liquibase; el controlador REST implementarÃ¡ las operaciones que luego formalizarÃ¡n los contratos de TASK-009 a TASK-011.

El agente de personaje autorizado para este mÃ³dulo se integrarÃ¡ como configuraciÃ³n, cliente y plantillas Spring AI en `api/`, sin crear ni modificar `agents/character-agent/Modelfile`. Las validaciones de los resultados del agente y de los comandos usarÃ¡n `framework-core` 0.2.0-SNAPSHOT (`Validator`, `ValidationGuard`, usos 1 a 3); las operaciones de escritura, el mapeo y sus pruebas adoptarÃ¡n respectivamente `WriteTransactionGuard` (usos 1 y 2), `EntityMapperGuard` (usos 1 y 2), `AutoIdJpaEntity` (uso 1), `InMemoryRepository` (usos 1 a 3) y `FakeChatModel` (usos 1 a 3) cuando correspondan. Esto requiere incorporar las dependencias del framework y su repositorio Maven antes de compilar.

El puerto de imÃ¡genes solo consulta condiciones; ni el adaptador, ni la generaciÃ³n, ni la selecciÃ³n, ni el almacenamiento de assets pertenecen a este sprint. Se mantiene KISS/YAGNI: DTOs y puertos mÃ­nimos para los flujos definidos, sin anticipar sincronizaciÃ³n de snapshots ni gestiÃ³n de retratos.

## Cambios por tarea

### TASK-001 Definir el agente de personaje y su contrato de salida textual

- **Ficheros:** `api/pom.xml` (modificar): declarar los mÃ³dulos `framework-ai`, `framework-ai-testing`, `framework-core`, `framework-json` y `framework-testing` en versiÃ³n `0.2.0-SNAPSHOT`, junto con la configuraciÃ³n Maven indicada por `CONSUMIR.md`; `api/src/main/resources/application.yaml` (modificar): registrar el modelo configurable del personaje y las reglas compartidas de edad y especies; `api/src/main/java/es/vargontoc/storyteller/shared/Constants.java` y `infrastructure/config/AgentsConfig.java` (modificar): declarar los nombres y el `ChatClient` cualificado del agente; `infrastructure/adapters/out/external/OllamaAdapter.java` (modificar): incluir el modelo en la comprobaciÃ³n y descarga de agentes; `api/src/main/resources/prompts/character_generate.st` y `character_review.st` (nuevos): prompts de alta automÃ¡tica/manual y revisiÃ³n; `domain/response/CatalogCharacterAgentResult.java` y `CatalogCharacterReviewAgentResult.java` (nuevos): contratos de salida tipados; `application/ports/out/external/CharacterAgent.java` y `infrastructure/adapters/out/external/SpringAiCharacterAgent.java` (nuevos, propuesta): puerto/adaptador que aÃ­sla Spring AI y entrega salida tipada; pruebas en `api/src/test/java/.../infrastructure/adapters/out/external/` (nuevas).
- **Enfoque:** mover al servidor, por autorizaciÃ³n expresa, la responsabilidad de llamar a un modelo Ollama ya cargado. El adaptador construirÃ¡ peticiones inmutables con `AgentRequest` y resolverÃ¡ una salida tipada con el contrato mÃ­nimo: aceptaciÃ³n, motivo de rechazo si aplica, nombre, descripciones narrativa y visual, y metadatos visuales. CentralizarÃ¡ la comprobaciÃ³n de disponibilidad antes de invocar; no duplicarÃ¡ el agente director ni confiarÃ¡ en respuestas incompletas. `FakeChatModel` guionizarÃ¡ respuestas vÃ¡lidas, rechazo y respuesta invÃ¡lida sin requerir Ollama real.
- **Pruebas automÃ¡ticas:** `CharacterAgentAdapterTest`: alta automÃ¡tica y manual vÃ¡lidas devuelven los tres campos obligatorios; entrada rechazada devuelve motivo y no ficha aplicable; revisiÃ³n vÃ¡lida devuelve propuesta; JSON/campos ausentes o fallo del proveedor no se transforma en ficha. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** con el modelo configurado en Ollama, solicitar una ficha automÃ¡tica y una manual apta y confirmar nombre mÃ¡s ambas descripciones; enviar una frase no apta y confirmar Ãºnicamente el motivo de rechazo. Confirmar que no se modifica ningÃºn Modelfile de `agents/`.

### TASK-002 Definir el puerto de condiciones visuales de personaje

- **Ficheros:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterImageConditionPort.java` (nuevo, propuesta); pruebas de contrato del consumidor en `api/src/test/java/.../application/service/` (nuevas).
- **Enfoque:** definir dos consultas booleanas por identificador de `CatalogCharacter`: retrato aprobado e imÃ¡genes generadas. El puerto no expondrÃ¡ comandos ni tipos de persistencia/ComfyUI. Al no existir todavÃ­a proveedor de assets, los servicios dependerÃ¡n del puerto y las pruebas suministrarÃ¡n un doble explÃ­cito; no se aÃ±adirÃ¡ implementaciÃ³n productiva ni valor por defecto que oculte la ausencia de assets.
- **Pruebas automÃ¡ticas:** pruebas unitarias del consumidor con dobles para ambas combinaciones de condiciones, verificando que puede calcular borrador/aviso sin invocar operaciones de escritura. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** inspeccionar el puerto y confirmar que expone exclusivamente ambas lecturas, por id de catÃ¡logo, sin generar, elegir, guardar ni modificar imÃ¡genes.

### TASK-003 Implementar el alta y la validaciÃ³n de fichas en el catÃ¡logo

- **Ficheros:** `api/src/main/resources/db/changelog/migrations/014_create_catalog_character.xml` y `015_create_catalog_character_review.xml` (nuevos, propuesta): tablas independientes para maestro y revisiones, sin `story_id`; `domain/model/CatalogCharacter.java`, `CatalogCharacterReview.java` y comandos de alta (nuevos); `application/ports/in/character/CharacterCatalogUseCase.java`, `application/ports/out/persistence/CatalogCharacterRepository.java`, `application/service/CharacterCatalogService.java` (nuevos); entidades, repositorios Spring Data, mappers y adaptadores equivalentes bajo `infrastructure/persistence`, `infrastructure/mappers` e `infrastructure/adapters/out/persistence` (nuevos); validador del contrato en `infrastructure/validations/CatalogCharacterAgentResultValidator.java` (nuevo); DTOs y controlador de catÃ¡logo bajo `infrastructure/adapters/in/rest/` (nuevos); pruebas unitarias y de persistencia correspondientes (nuevas).
- **Enfoque:** recibir modo automÃ¡tico o frase manual, delegar en `CharacterAgent`, pasar la salida por `Validator` y `ValidationGuard`, y persistir solo una ficha aceptada, completa y consistente. La migraciÃ³n crea el catÃ¡logo maestro sin migrar automÃ¡ticamente los `character` histÃ³ricos: siguen ligados a `story` y son snapshots. Las escrituras del adaptador se protegerÃ¡n con `WriteTransactionGuard`; el mapeo entidad-dominio con `EntityMapperGuard`; los ids JPA nuevos usarÃ¡n `AutoIdJpaEntity`. El endpoint devolverÃ¡ el rechazo con su motivo sin crear entidad; sus detalles se formalizarÃ¡n posteriormente en TASK-009.
- **Pruebas automÃ¡ticas:** `CharacterCatalogServiceTest` con `FakeChatModel`/dobles: alta automÃ¡tica aceptada, manual aceptada, rechazo de cada modo sin escritura y salida incompleta sin escritura. `CatalogCharacterRepositoryAdapterTest` o `@DataJpaTest`: persistencia y lectura de los tres campos obligatorios y aislamiento respecto de `CharacterJpaEntity`. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** crear un personaje automÃ¡tico y otro manual apto, comprobando ficha completa; intentar frase no apta y comprobar el motivo sin nueva fila ni entrada en catÃ¡logo.

### TASK-004 Implementar el listado, detalle, estado derivado y renombrado del catÃ¡logo

- **Ficheros:** `CharacterCatalogUseCase.java`, `CharacterCatalogService.java`, `CatalogCharacterRepository.java`, su adaptador/mapeador/repositorio JPA y el controlador/DTOs de catÃ¡logo (modificar); modelos de vista de catÃ¡logo y detalle (nuevos, propuesta); migraciÃ³n de Ã­ndices o restricciones de nombre solo si la consulta lo precisa (nuevo, propuesta); pruebas de servicio, persistencia y web (nuevas).
- **Enfoque:** listar todos los maestros y recuperar uno por id, aÃ±adiendo revisiÃ³n pendiente y `draft = !hasApprovedPortrait(id)` como proyecciones derivadas, nunca como columna persistida. El renombrado escribe Ãºnicamente `CatalogCharacter`; no consulta, actualiza ni recrea `CharacterJpaEntity` de cuentos. La lectura de condiciones visuales pasa por TASK-002 y los caminos de escritura se ejecutan en transacciÃ³n. No se implementa propagaciÃ³n de cambios, reconstrucciÃ³n de cuentos ni gestiÃ³n del retrato.
- **Pruebas automÃ¡ticas:** catÃ¡logo vacÃ­o y con elementos; detalle completo; retrato ausente/presente cambia el derivado borrador; renombrado actualiza solo el maestro y conserva una entidad `CharacterJpaEntity` histÃ³rica sin cambio; revisiÃ³n pendiente aparece en listado/detalle. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** con dos fichas, revisar nombres, estado y detalle; comprobar estado vacÃ­o sin datos; renombrar y verificar el nuevo nombre del catÃ¡logo y la preservaciÃ³n de un cuento existente.

### TASK-005 Adaptar el servicio de revisiones al catÃ¡logo y al agente de personaje

- **Ficheros:** `application/ports/in/character/CharacterReviewUseCase.java`, `application/service/CharacterReviewService.java`, `application/ports/out/persistence/CatalogCharacterReviewRepository.java` y adaptador JPA (nuevos, propuesta); `CatalogCharacterReview.java`, comandos/resultados de revisiÃ³n y DTOs REST (nuevos); controlador de revisiones de catÃ¡logo (nuevo o modificar `ReviewsController.java`); `CharacterImageConditionPort.java`, `CharacterAgent.java`, prompts y validadores (modificar); pruebas de servicio, persistencia y web (nuevas). `CharacterService.java`, `CharacterReviewRepository.java` y sus adaptadores actuales se mantienen para los actores de cuento.
- **Enfoque:** no reutilizar la revisiÃ³n actual de `Actor`, pues presupone `storyId`, sinopsis y borra pÃ¡ginas/assets; crear el flujo de catÃ¡logo independiente. Ante pendiente existente, devolver la confirmaciÃ³n requerida sin descartarla; solo tras la confirmaciÃ³n descartar la anterior y crear la nueva. El agente valida el hint y propone cambios; un rechazo no crea pendiente. Si el target contiene visual y `hasGeneratedImages(id)` es verdadero, adjuntar el aviso de consistencia; no consultarlo para cambio exclusivamente narrativo. El modelo admite una sola pendiente por catÃ¡logo, reforzada por consulta/Ã­ndice y transacciÃ³n.
- **Pruebas automÃ¡ticas:** sin pendiente crea propuesta pendiente; con pendiente exige confirmaciÃ³n y, confirmada, descarta la anterior; hint rechazado no persiste; cambio vÃ¡lido persiste propuesta; aviso aparece solo para cambio visual con imÃ¡genes generadas. AÃ±adir carrera de dos peticiones para comprobar que persiste como mÃ¡ximo una pendiente. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** solicitar revisiÃ³n vÃ¡lida; repetirla y confirmar el reemplazo; enviar cambio no apto; para una ficha con imÃ¡genes generadas solicitar cambio visual y comprobar el aviso, y comprobar su ausencia para cambio narrativo.

### TASK-006 Implementar la aceptaciÃ³n y el descarte de revisiones del catÃ¡logo

- **Ficheros:** `CharacterReviewUseCase.java`, `CharacterReviewService.java`, `CatalogCharacterReviewRepository.java`, adaptador JPA y controlador/DTOs de revisiones (modificar); pruebas de servicio y persistencia (nuevas).
- **Enfoque:** recuperar exclusivamente la revisiÃ³n pendiente del catÃ¡logo. Al aceptar, aplicar solo las propiedades indicadas por el target al maestro y marcar la revisiÃ³n `CONFIRMED` en una misma transacciÃ³n; al descartar, conservar la ficha y marcar `DISCARDED`. Rechazar ids inexistentes o revisiones no pendientes con el error del framework normalizado por el lÃ­mite web. No tocar personajes/snapshots de cuentos ni crear nuevas imÃ¡genes.
- **Pruebas automÃ¡ticas:** aceptaciÃ³n narrativa actualiza solo narrativa y confirma; aceptaciÃ³n visual actualiza solo visual y confirma; descarte no modifica nombre ni descripciones y descarta; segunda aceptaciÃ³n o descarte de una revisiÃ³n ya resuelta falla sin cambios. Ejecutar `mvn test` desde `api/`.
- **Prueba manual:** aceptar una propuesta narrativa y comprobar texto y desapariciÃ³n de pendiente; descartar otra y comprobar ficha inalterada y desapariciÃ³n de pendiente.

## Orden de implementaciÃ³n

1. TASK-001 (habilitada por: configuraciÃ³n Maven del framework 0.2.0-SNAPSHOT y acceso de Maven a GitHub Packages).
2. TASK-002 (habilitada por: sin dependencias; se entrega solo la abstracciÃ³n).
3. TASK-003 (habilitada por: TASK-001 entrega el `CharacterAgent` probado y TASK-002 el puerto de consultas).
4. TASK-004 (habilitada por: TASK-003 entrega el catÃ¡logo persistente y TASK-002 el puerto de retrato aprobado).
5. TASK-005 (habilitada por: TASK-001, TASK-002 y TASK-003 entregadas; las revisiones se anclan al catÃ¡logo maestro).
6. TASK-006 (habilitada por: TASK-004 y TASK-005 entregadas; existe ficha maestra y una revisiÃ³n pendiente).
7. Hand-off a `app`: TASK-007 a TASK-011 se habilitan cuando TASK-003 a TASK-006 expongan y prueben sus operaciones REST; TASK-009 a TASK-011 formalizan el contrato posteriormente segÃºn reparto autorizado.

## Riesgos y dudas tÃ©cnicas

- El diseÃ±o y TASK-001 ubican el Modelfile en `agents/`, pero el reparto autorizado traslada su responsabilidad al servidor y prohÃ­be tocarlo. MitigaciÃ³n: el sprint solo integra la llamada al modelo ya cargado mediante configuraciÃ³n/prompt/adaptador en `api/`; debe alinearse `design.md` y `tasks.md` en la siguiente revisiÃ³n SDD para reflejarlo.
- `api/pom.xml` no declara `es.vargontoc.framework`; el uso solicitado exige fijar `0.2.0-SNAPSHOT`, declarar el repositorio/credenciales descritos en `CONSUMIR.md` y verificar resoluciÃ³n antes de implementar. Si no se resuelve, no se deben sustituir las APIs de la guÃ­a por equivalentes propios.
- La separaciÃ³n de catÃ¡logo y snapshot exige nuevas tablas y no permite reutilizar `CharacterJpaEntity`, que tiene `story_id` no nulo. MitigaciÃ³n: tablas y puertos de catÃ¡logo independientes; los datos histÃ³ricos se preservan y una migraciÃ³n de adopciÃ³n de protagonistas existentes queda fuera hasta que exista una regla de correspondencia aprobada.
- Assets aÃºn no provee adaptador de `CharacterImageConditionPort`. MitigaciÃ³n: doble en pruebas y dependencia de puerto; el despliegue necesita un proveedor real antes de que los estados visuales sean fiables.

## ValidaciÃ³n

- Suite del módulo: pendiente por cambios de TASK-005 (origen: implementación actual; alcance: revisiones de catálogo; evidencia específica: `mvn test -Dtest=CharacterReviewServiceTest,CharacterReviewControllerTest,CatalogCharacterReviewConcurrencyTest` correcta, 7 pruebas)
- IntegraciÃ³n: pendiente
- ValidaciÃ³n manual: pendiente
