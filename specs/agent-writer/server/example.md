# Ejemplos: agent-writer - server

## GenerationGate (GenerationGatePort)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/GenerationGate.java` (puerto en `application/ports/out/GenerationGatePort.java`)
**Para qué sirve:** Puerta única en memoria: solo una generación (escritor, director, agente de personaje o retratos) a la vez en toda la aplicación. Al arrancar está libre. `tryAcquire` no espera: devuelve `false` si hay otra en curso y el servicio debe rechazar la solicitud. `release` se llama siempre en `finally`. `isGeneratingStory` dice si la generación en curso es de un cuento concreto (los turnos sin cuento, como retratos, devuelven `false`).

```java
if (!gate.tryAcquire(GenerationGatePort.GenerationType.WRITER, storyId)) {
    throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);
}
try {
    // llamada al modelo
} finally {
    gate.release();
}

// Operaciones que no deben coincidir con una generación del mismo cuento (aceptar, elegir versión...)
if (gate.isGeneratingStory(storyId)) {
    throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);
}
```

## PageLengthValidator (PageLengthWarning)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/domain/service/PageLengthValidator.java` (aviso en `domain/enums/PageLengthWarning.java`)
**Para qué sirve:** Clasifica el texto narrativo de una página: válido si tiene de 1 a 4 frases y como máximo 66 palabras. Devuelve `NONE`, `TOO_MANY_WORDS`, `SENTENCE_COUNT` o `BOTH`. Regla de frases: un grupo de signos de cierre (`...`, `?!`, `!!!`) cuenta como una sola frase; si tras el cierre sigue una minúscula (incisos de diálogo como `—¡Hola! —dijo el conejo.`) la frase continúa y no se cuenta; el texto sin signo de cierre o vacío tiene 0 frases. Es una clase sin estado ni dependencias: se instancia con `new` o se declara como bean.

```java
PageLengthValidator validator = new PageLengthValidator();

PageLengthWarning warning = validator.validate("—¡Hola! —dijo el conejo. Luna sonrió.");
// NONE (2 frases, 6 palabras)

validator.validate("El conejo salta sin parar");   // SENTENCE_COUNT (0 frases)

if (warning.hasSentenceWarning()) {
    // no se puede aceptar la versión: hay que pedir otra
}
```

## StoryPageVersionRepository (PageVersion)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryPageVersionRepositoryAdapter.java` (puerto en `application/ports/out/persistence/StoryPageVersionRepository.java`, modelo en `domain/model/PageVersion.java`, migración `024_create_story_page_version.xml`)
**Para qué sirve:** Histórico de versiones de la portada o de una página (`StoryPage`): texto narrativo (`null` en la portada), escena y aviso de longitud, sin imagen. Cada página tiene como mucho una versión vigente (lo garantiza también un índice único en base de datos). `add` guarda al final del histórico y, si la nueva es vigente, la anterior deja de serlo. `setCurrent` cambia la vigente y falla si la versión no es de esa página. `deleteByPage` borra el lote entero de una página sin tocar las demás. No hay límite de versiones.

```java
SceneComposition scene = new SceneComposition("Luna en el bosque", List.of(), "bosque");

// Versión de página con su aviso de longitud; la última se marca vigente
versions.add(pageId, new PageVersion("Luna sale.", scene, PageLengthWarning.NONE, false));
versions.add(pageId, new PageVersion("Texto largo...", scene, PageLengthWarning.TOO_MANY_WORDS, true));

// Portada: sin texto narrativo
versions.add(coverId, new PageVersion(null, scene, PageLengthWarning.NONE, true));

List<PageVersion> history = versions.findByPage(pageId);   // de la más antigua a la más reciente
versions.setCurrent(pageId, history.get(0).getId());         // la primera pasa a ser la vigente
versions.deleteByPage(pageId);                               // al aceptar o invalidar la página
```

## StoryPageRepository: estado, contenido aceptado e imagen importada (StoryPage)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryPageRepositoryAdapter.java` (puerto en `application/ports/out/persistence/StoryPageRepository.java`, modelo `domain/model/StoryPage.java`, estado en `domain/enums/PageStatus.java`, migración `025_add_outline_fields_to_story_page.xml`)
**Para qué sirve:** Amplía la página existente para los cuentos con escaleta. `createPending` crea la portada (`page` 0, sin beat) o la página N vinculada a su beat (`beatId`), en estado `PENDING`, sin contenido aceptado (`text` y `composition` a `null`; el texto de trabajo vive en las `PageVersion`) y sin imagen. `accept` la deja `ACCEPTED` con su contenido (`text` es `null` en la portada, cuyo título es `getCoverText()`) y sin imagen. `removeImportedImage` quita solo la asociación con la imagen importada y no toca el contenido aceptado. La imagen importada (`getImportedImage()`, una referencia) no la asigna esta feature: la importación está fuera de alcance y las pruebas la preparan a mano. En las páginas de cuentos antiguos `getPageStatus()`, `getBeatId()` e `getImportedImage()` son `null` y el resto se lee como siempre. El modelo usa `getPageStatus()` porque `StoryPageReview` ya tiene `getStatus()`.

```java
SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");

StoryPage cover = pages.createPending(storyId, 0, null);      // portada pendiente
StoryPage page1 = pages.createPending(storyId, 1, beat1Id);   // página 1 pendiente, vinculada al beat 1

pages.accept(cover.getId(), null, scene);                     // portada aceptada: título y escena, sin texto
pages.accept(page1.getId(), "Luna sale.", scene);             // página aceptada con su contenido

StoryPage read = pages.getPage(page1.getId());
read.getPageStatus();      // ACCEPTED
read.getImportedImage();   // referencia a la imagen importada o null

pages.removeImportedImage(page1.getId());                     // sin imagen; mismo contenido aceptado
```

## StoryPageNotifier (StoryPageGenerationEvent)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageNotifier.java` (evento en `domain/events/StoryPageGenerationEvent.java`, tipos en `domain/enums/StoryPageEventType.java`, puerto de salida en `application/ports/out/StoryPageEventPublisher.java`)
**Para qué sirve:** Publica los cuatro eventos de una generación de portada o página: `STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED` (con el motivo del escritor en `reason`) y `FAILED`. Cada evento lleva `storyId` y `pageNumber` (0 es la portada); `reason` es `null` salvo en el rechazo del hint. Entrega el evento al puerto `StoryPageEventPublisher`, que difunde `WebsocketStoryPageEventPublisher` (ver más abajo).

```java
notifier.started(storyId, 3);                          // inicio de la página 3
notifier.versionPresented(storyId, 0);                 // versión presentada de la portada
notifier.hintRejected(storyId, 2, "El hint contradice el guion");
notifier.failed(storyId, 3);                           // fallo de la página 3
```

## WriterModelProvisioner (writer.modelfile)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/WriterModelProvisioner.java` (Modelfile en `api/src/main/resources/prompts/writer.modelfile`)
**Para qué sirve:** Al arrancar, registra en Ollama el modelo del escritor (nombre del bean `scriptwriter-model`, por defecto `storyteller-writer`) a partir del Modelfile del backend. Si Ollama ya lo tiene, no hace nada; si lo crea, traduce `FROM`, `PARAMETER` y `SYSTEM` al cuerpo de `/api/create`. Un error de Ollama (con su estado HTTP y cuerpo) o un Modelfile sin `FROM` impiden el arranque. Se desactiva con `app.agents.writer-provisioning.enabled=false` (variable `APP_AGENT_WRITER_PROVISIONING_ENABLED`); las pruebas de contexto lo desactivan para no depender de Ollama. El Modelfile fija las reglas del escritor: fidelidad al beat, 1 a 4 frases y 60 palabras, sin subordinadas encadenadas y vocabulario de 3 a 4 años sin imitar el habla infantil. Es un `ApplicationRunner`: no se invoca a mano.

```java
// No se llama desde el código: Spring lo ejecuta al arrancar.
// Con el modelo ausente en Ollama, equivale a:
//   POST /api/create {"model":"storyteller-writer","from":"qwen2.5:14b-instruct",
//                     "system":"Eres el agente escritor...","parameters":{...},"stream":false}
// Para arrancar sin aprovisionar:
//   app.agents.writer-provisioning.enabled=false
```

## WriterAgent (SpringAiWriterAgent)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java` (puerto en `application/ports/out/external/WriterAgent.java`, contexto en `domain/request/WriterContext.java`, resultados en `domain/response/WriterResult.java` y `WriterHintResult.java`)
**Para qué sirve:** Adaptador del escritor en Ollama con tres operaciones. `writeCover` devuelve la escena de portada (texto siempre `null`; solo recibe título, sinopsis e instantánea del protagonista). `writePage` devuelve texto y escena de la página del beat, con el reparto episódico y el texto aceptado de las páginas anteriores. `rewrite` aplica un hint sobre una versión base y devuelve `hintAccepted` con la nueva versión o el rechazo con `rejectionReason`; con un contexto de portada no exige texto. Una respuesta sin texto (salvo portada) o sin escena, un rechazo sin motivo, un modelo no disponible o un error de Ollama lanzan `AppException`. La salida JSON del modelo es `{text, scene}` para portada y página, y `{hintAccepted, rejectionReason, text, scene}` para el hint, con `scene = {scene, actors:[{name, action}], background}` (el Modelfile lo recoge). No aplica la validación de longitud: eso es del servicio.

```java
// Portada: sin beat, reparto ni páginas anteriores
WriterResult cover = writer.writeCover(WriterContext.cover(title, synopsis, protagonistSnapshot));
cover.scene();   // cover.text() es null

// Página N con su contexto
WriterContext context = new WriterContext(title, synopsis, protagonistSnapshot,
        episodicCast, beat3, List.of("Texto página 1.", "Texto página 2."));
WriterResult page = writer.writePage(context);    // page.text() y page.scene()

// Nueva versión con hint sobre la versión base
WriterHintResult version = writer.rewrite(context, page, "que el conejo esté contento");
if (version.hintAccepted()) {
    // version.text() y version.scene() son la nueva versión
} else {
    // version.rejectionReason() es el motivo para el adulto
}
```

**Prueba manual contra Ollama:** `api/src/test/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgentManualHarness.java` llama a `writePage` y a `rewrite` (hint sin sustento en el beat) y muestra el resultado por consola. Solo corre con `-Dwriter.harness=true`; en la suite normal se omite.

```
mvn -f api/pom.xml test -Dtest=SpringAiWriterAgentManualHarness -Dwriter.harness=true
```

## Difusión por /storyteller de eventos de portada y página (WebsocketStoryPageEventPublisher)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/WebsocketStoryPageEventPublisher.java` (difusión en `WebsocketAdapter.sendStoryPageNotification`, declarada en `application/ports/in/WebsocketUseCase.java`)
**Para qué sirve:** Implementa `StoryPageEventPublisher` y envía cada evento de `StoryPageNotifier` a todos los clientes suscritos a `/storyteller`, con el mismo canal que las notificaciones y los retratos (que no cambian). El JSON lleva `type` (`STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED`, `FAILED`), `storyId`, `pageNumber` (0 es la portada) y `reason`, que solo aparece en `HINT_REJECTED`. Sustituye al publicador provisional que solo escribía en el log. Es un bean: no se instancia a mano.

```java
notifier.started(7L, 0);
// {"type":"STARTED","storyId":7,"pageNumber":0}
notifier.hintRejected(7L, 2, "Fuera del beat");
// {"type":"HINT_REJECTED","storyId":7,"pageNumber":2,"reason":"Fuera del beat"}
```

## Flujo antiguo a través de la puerta (StoryPageService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`
**Para qué sirve:** `generate` y `review` se bifurcan por `story.hasOutline()`. Los cuentos antiguos (sin escaleta) conservan la deducción de la página siguiente (portada, luego la siguiente por recuento) y `StoryPageReview`, pero ahora piden turno `WRITER` a `GenerationGatePort`: con la puerta ocupada se lanza `AppException` 409 "Hay una generación en curso" sin llamar al modelo ni descartar la revisión pendiente, y el turno se libera en `finally` con éxito o con fallo. Los cuentos con escaleta no usan `StoryPageReview`: `review` los rechaza con 409. La API REST no cambia.

```java
// Cuento antiguo: igual que antes para quien llama; solo cambia que puede rechazarse por la puerta
StoryPage page = storyPageService.generate(new StoryPageGenerateCommand(storyId));
// con retratos u otra generación en curso -> AppException(409, "Hay una generación en curso")

StoryPageReview review = storyPageService.review(new StoryPageReviewCommand(pageId, "más color", target));
// en un cuento con escaleta -> AppException(409, "Los cuentos con escaleta no usan la revisión de páginas heredada")
```

## Generar la escena de portada (CoverGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`startCover`; puerto en `application/ports/in/generator/CoverGeneration.java`, respuesta en `domain/response/GenerationStarted.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** Solo para cuentos con escaleta. `POST /api/v1/storyteller/{storyId}/cover/generate` comprueba que el guion está aprobado (`SCRIPT_ACCEPTED`) y que no hay portada, pide el turno `WRITER` y responde `202` con `GenerationStarted(storyId, 0)` sin esperar al modelo. En segundo plano (`TaskExecutor`) llama a `WriterAgent.writeCover`, crea la portada pendiente (`createPending(storyId, 0, null)`; el título es el del guion) y guarda la escena como primera `PageVersion` vigente, sin texto ni aviso: la portada nunca pasa por la validación de longitud. Después de liberar el turno publica `VERSION_PRESENTED` (o `FAILED` si el escritor o el guardado fallan, sin dejar portada) con `pageNumber` 0; `STARTED` se publica al conceder el turno. Se rechaza con 409 si el guion no está aprobado, si ya hay portada, si el cuento es antiguo o si hay otra generación en curso (sin pedir turno en los tres primeros casos). Con escaleta, `POST /api/v1/generations/page` ya no genera la portada: responde 409 y remite a esta operación.

```java
GenerationStarted started = coverGeneration.startCover(storyId);   // GenerationStarted(storyId, 0)

// Por WebSocket (/storyteller):
// {"type":"STARTED","storyId":7,"pageNumber":0}
// {"type":"VERSION_PRESENTED","storyId":7,"pageNumber":0}   // o {"type":"FAILED",...}
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/cover/generate
```

## Aceptar la portada o una página pendiente (PageAcceptance)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`acceptPage`; puerto en `application/ports/in/generator/PageAcceptance.java`, respuesta en `domain/response/PageAccepted.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/accept` acepta la portada (`pageNumber` 0) o una página pendiente de un cuento con escaleta. Promueve la versión vigente a contenido aceptado (`StoryPageRepository.accept`: estado `ACCEPTED`, sin imagen; el texto es `null` en la portada) y borra todo el histórico de versiones (`deleteByPage`). No pide turno ni inicia la generación de la página siguiente. Solo bloquea (409) si la vigente tiene aviso de número de frases (`SENTENCE_COUNT` o `BOTH`: hay que elegir otra versión válida, pedir un cambio con hint o regenerar sin hint) o si hay una generación en curso del mismo cuento (`gate.isGeneratingStory`); el aviso de exceso de palabras no bloquea y el texto se acepta sin cambios, y una generación de otro cuento o de retratos tampoco. También responde 409 si la página no está pendiente y 404 si no pertenece al cuento. Si se rechaza, no cambia nada.

```java
StoryPage accepted = pageAcceptance.acceptPage(storyId, pageId);
accepted.getPageStatus();   // ACCEPTED; el histórico de versiones ya no existe
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/40/accept
# {"success":true,"data":{"storyId":7,"pageId":40,"pageNumber":0,"status":"ACCEPTED"}}
```

## Generar la página N desde su beat (PageGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`startPage`; puerto en `application/ports/in/generator/PageGeneration.java`, respuesta en `domain/response/GenerationStarted.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** Solo para cuentos con escaleta. `POST /api/v1/storyteller/{storyId}/pages/{number}/generate` exige guion aprobado, la portada aceptada y todas las páginas anteriores aceptadas, y que `number` sea la siguiente (existentes + 1) y tenga beat en la escaleta; si no, responde 409 con el motivo (por ejemplo "Primero hay que aceptar la página 2") sin pedir turno ni llamar al escritor. Después pide el turno `WRITER` y responde `202` con `GenerationStarted(storyId, number)`. El contexto se cierra en la solicitud: título y sinopsis del cuento, instantánea del protagonista guardada en el cuento (no el catálogo), reparto episódico y beat N de la escaleta y texto aceptado de las páginas 1 a N-1. En segundo plano llama a `WriterAgent.writePage`, crea la página pendiente vinculada al beat N (`createPending(storyId, N, N)`, sin imagen) y guarda el resultado como única `PageVersion` vigente con el aviso de `PageLengthValidator` (el bucle de longitud de TASK-014 puede guardar hasta 3, ver la sección siguiente). Si falla el guardado de la versión se retira solo esa página (`StoryPageRepository.delete`). Tras liberar el turno publica `VERSION_PRESENTED` o `FAILED` con `pageNumber` N; `STARTED` se publica al conceder el turno. Con la puerta ocupada, 409 "Hay una generación en curso".

```java
GenerationStarted started = pageGeneration.startPage(storyId, 1);   // GenerationStarted(storyId, 1)

// Por WebSocket (/storyteller):
// {"type":"STARTED","storyId":7,"pageNumber":1}
// {"type":"VERSION_PRESENTED","storyId":7,"pageNumber":1}   // o {"type":"FAILED",...}
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/1/generate
# Con la página 1 pendiente: 409 "Primero hay que aceptar la página 1" al pedir la 2
```

## Bucle de longitud de la generación de página (StoryPageService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`generateWithinLimit`, privado, y `createPage`)
**Para qué sirve:** Cada solicitud de generación de página N llama al escritor hasta 3 veces dentro del mismo turno de la puerta y valida cada texto con `PageLengthValidator`. Si una generación no tiene aviso, solo esa se guarda (las anteriores fuera del límite se descartan), con una sola llamada si la primera ya cumple. Si las tres tienen aviso se guardan las tres, cada una con su aviso, y la tercera es la vigente; se publica `VERSION_PRESENTED`, no `FAILED`. El bucle trabaja en memoria y la página y sus versiones se crean al final, así que mientras dura el proceso no hay nada consultable y la puerta sigue ocupada. Si falla cualquier generación no se guarda ninguna versión ni se crea la página, se libera el turno y se publica `FAILED`. `generateWithinLimit(Supplier<WriterResult>)` recibe la llamada al escritor, por lo que el cambio con hint, la reapertura y la regeneración sin hint (TASK-015 a TASK-018) lo reutilizan pasando su propia generación. La portada no lo usa.

```java
// Dentro de startPage: la generación es un Supplier y el bucle devuelve las versiones a guardar
createPage(storyId, number, () -> writer.writePage(context));

// Resultado según los textos devueltos por el escritor:
// 70 palabras, luego 55 palabras y 3 frases -> histórico: [segunda]            (vigente, sin aviso)
// 70, 70, 70 palabras                       -> histórico: [1ª, 2ª, 3ª]         (TOO_MANY_WORDS, la 3ª vigente)
// 5 frases, 70 palabras, 70 palabras        -> avisos SENTENCE_COUNT, TOO_MANY_WORDS, TOO_MANY_WORDS
// segunda llamada con error                 -> sin versiones, sin página, FAILED
```
