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

## Cambio con hint sobre la portada o una página pendiente (PageGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`startHint`; puerto en `application/ports/in/generator/PageGeneration.java`, cuerpo en `infrastructure/adapters/in/rest/dto/PageHintRequestDto.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/hint` con `{"hint": "..."}`; `{id}` es el id de la página (el de `accept`), no su número. Sobre la portada o una página pendiente o aceptada de un cuento con escaleta (una aceptada se reabre, ver la sección siguiente). Un hint vacío o solo con espacios, o una página que no es del cuento, se rechazan antes de pedir turno (400 "El hint es obligatorio" / 404). Después pide el turno `WRITER` y responde `202` con `GenerationStarted(storyId, número de página)`; con la puerta ocupada, 409 "Hay una generación en curso". La versión vigente del histórico es la base que recibe `WriterAgent.rewrite` junto con el hint y el mismo contexto que la generación inicial (la portada usa el contexto de portada). Si el escritor acepta, la página pasa por el bucle de longitud (hasta 3 generaciones) y las versiones resultantes se añaden al histórico de la página real con la última como vigente; la portada guarda una sola versión sin aviso. Los hints sucesivos usan la vigente de cada momento como base y el histórico no tiene límite (10 hints sobre una versión dan 11). Si el escritor rechaza el hint se publica `HINT_REJECTED` con `reason` y no se crea ninguna versión ni se regenera; si falla (también en un intento intermedio del bucle) se publica `FAILED` y el histórico y la vigente no cambian. El turno se libera siempre antes de anunciar el resultado.

```java
GenerationStarted started = pageGeneration.startHint(storyId, pageId, "que el conejo esté contento");

// Por WebSocket (/storyteller):
// {"type":"STARTED","storyId":7,"pageNumber":3}
// {"type":"VERSION_PRESENTED","storyId":7,"pageNumber":3}   // una versión más (vigente) en el histórico
// {"type":"HINT_REJECTED","storyId":7,"pageNumber":3,"reason":"El lobo no aparece en este beat"}
// {"type":"FAILED","storyId":7,"pageNumber":3}
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/40/hint \
  -H "Content-Type: application/json" -d '{"hint":"que el conejo esté contento"}'
# 202 {"success":true,"data":{"storyId":7,"pageNumber":3}}
# Hint vacío -> 400 "El hint es obligatorio"; puerta ocupada -> 409 "Hay una generación en curso"
```

## Regenerar sin hint una página bloqueada por número de frases (PageGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`startRegenerate`; puerto en `application/ports/in/generator/PageGeneration.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/regenerate`, sin cuerpo; `{id}` es el id de la página (el de `accept` y `hint`). Solo se habilita si la versión VIGENTE de una página pendiente tiene aviso de número de frases (`SENTENCE_COUNT` o `BOTH`); si no (sin aviso, solo exceso de palabras aunque otra versión del histórico avise de frases, portada, página aceptada) responde 409 sin pedir turno ni llamar al escritor, y 404 si la página no es del cuento. Con la puerta ocupada, 409 "Hay una generación en curso". Si procede, responde `202` con `GenerationStarted(storyId, número de página)` y en segundo plano pide la página al escritor con `writePage` y el mismo contexto de beat que la generación inicial, aplica el bucle de longitud (hasta 3 generaciones) y añade las versiones a la página real: se suman al histórico existente, igual que con hint, y la última queda vigente. Un fallo en cualquier generación no cambia nada y publica `FAILED`.

```java
GenerationStarted started = pageGeneration.startRegenerate(storyId, pageId);

// Por WebSocket (/storyteller):
// {"type":"STARTED","storyId":7,"pageNumber":3}
// {"type":"VERSION_PRESENTED","storyId":7,"pageNumber":3}   // versión(es) nueva(s) en el histórico, la última vigente
// {"type":"FAILED","storyId":7,"pageNumber":3}
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/40/regenerate
# 202 {"success":true,"data":{"storyId":7,"pageNumber":3}}
# Vigente sin aviso de frases -> 409; puerta ocupada -> 409 "Hay una generación en curso"
```

## Elegir una versión vigente (PageAcceptance)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`selectVersion`; puerto en `application/ports/in/generator/PageAcceptance.java`, respuesta en `domain/response/VersionSelected.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/versions/{versionId}/select`, sin cuerpo; `{id}` es el id de la página y `{versionId}` el id real de la versión (`PageVersion.getId()`). Marca como vigente una versión del histórico de la portada o de una página pendiente y conserva todo el histórico. Es síncrono (no llama al modelo) y no pide turno: responde `200` con `VersionSelected(storyId, pageId, versionId, warning)`, donde `warning` es el aviso de la versión elegida (`NONE`, `TOO_MANY_WORDS`, `SENTENCE_COUNT` o `BOTH`). La página pasa a mostrar su texto, escena y aviso; si no tiene aviso de número de frases, se puede aceptar después, y la aceptación usa la versión elegida. Se rechaza con 409 si hay una generación en curso del mismo cuento (`gate.isGeneratingStory`; la de otro cuento o de retratos no bloquea) o si la página no está pendiente (una aceptada ya no es elegible), y con 404 si la página no es del cuento o la versión no pertenece a esa página. Si se rechaza, la vigente no cambia. En una página aceptada reabierta solo se eligen sus propuestas: la versión aceptada no está en el histórico, así que pedirla (su id de página u otro id ajeno) responde 404.

```java
VersionSelected selected = pageAcceptance.selectVersion(storyId, pageId, versionId);
selected.warning();   // aviso de la versión elegida, el que verá el usuario

// Si no avisa de número de frases, ya se puede aceptar con ella
pageAcceptance.acceptPage(storyId, pageId);
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/40/versions/101/select
# 200 {"success":true,"data":{"storyId":7,"pageId":40,"versionId":101,"warning":"TOO_MANY_WORDS"}}
# Generación del cuento en curso -> 409; versión de otra página o aceptada (no está en el histórico) -> 404
```

## Reabrir con hint la portada o una página aceptada (PageGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`startHint` y `selectVersion`; mismo endpoint `POST /api/v1/storyteller/{storyId}/pages/{id}/hint`)
**Para qué sirve:** `startHint` también admite la portada y las páginas `ACCEPTED`. El contenido aceptado (texto, escena e imagen importada) vive en la propia `StoryPage` y no se modifica ni entra en el histórico; las propuestas se guardan en el histórico de versiones de esa página real. Una página aceptada con propuestas es simplemente `ACCEPTED` con versiones en el histórico. Base del primer hint: el contenido aceptado (se construye en memoria, no se guarda); con propuestas existentes, la propuesta vigente. Las propuestas pasan por el bucle de longitud (hasta 3, con sus avisos), salvo la portada. Las páginas posteriores quedan intactas (el descarte en cascada es de la aceptación de la propuesta). Se mantienen el hint obligatorio (400), la puerta ocupada (409), `HINT_REJECTED` y `FAILED` sin cambios. `selectVersion` permite elegir entre propuestas de una aceptada; `acceptPage` sigue solo para pendientes (409 con una aceptada).

```java
// Páginas 1 a 4 aceptadas; hint sobre la 2 (id real de la fila)
pageGeneration.startHint(storyId, page2Id, "que el conejo esté contento");
// Propuesta vigente en el histórico de la 2; page2.getText() sigue siendo el texto aceptado;
// las páginas 3 y 4 siguen aceptadas

// Segundo hint: el escritor recibe la propuesta vigente como base
pageGeneration.startHint(storyId, page2Id, "más corto");

// Elegir entre propuestas (la versión aceptada no es elegible: 404)
pageAcceptance.selectVersion(storyId, page2Id, proposalId);
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/41/hint \
  -H "Content-Type: application/json" -d '{"hint":"que el conejo esté contento"}'
# 202; WebSocket: STARTED y VERSION_PRESENTED con pageNumber 2
# Consulta SQL: contenido aceptado en story_page y propuestas en story_page_version
```

## Descartar las propuestas de una reapertura (PageAcceptance)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`discardProposals`; puerto en `application/ports/in/generator/PageAcceptance.java`, respuesta en `domain/response/ProposalsDiscarded.java`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/proposals/discard`, sin cuerpo (POST, como `accept` y `select`, que también actúan sobre una página). `{id}` es el id real de la portada o de la página aceptada reabierta. Borra solo su histórico de propuestas (`deleteByPage` de ESA página): el contenido aceptado, su imagen importada, las páginas posteriores y los históricos de otras páginas no se tocan. Es síncrono, no llama al modelo y no pide turno; responde `200` con `ProposalsDiscarded(storyId, pageId, status)` (`status` es `ACCEPTED`). Se rechaza con 409 si hay una generación en curso del mismo cuento, si la página no está aceptada o si no tiene propuestas, y con 404 si la página no es del cuento. Una propuesta con aviso de número de frases también se puede descartar.

```java
// Página 2 aceptada con dos propuestas (hints previos); id real de la fila
ProposalsDiscarded done = pageAcceptance.discardProposals(storyId, page2Id);
done.status();   // ACCEPTED: la página vuelve a mostrar solo su versión aceptada, con su imagen
```

```
curl -X POST http://localhost:8080/api/v1/storyteller/7/pages/41/proposals/discard
# 200 {"success":true,"data":{"storyId":7,"pageId":41,"status":"ACCEPTED"}}
# Generación del cuento en curso, página no aceptada o sin propuestas -> 409; página de otro cuento -> 404
```

## Aceptar una propuesta de reapertura con descarte en cascada (PageAcceptance)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`acceptProposal`; puerto en `application/ports/in/generator/PageAcceptance.java`, respuesta en `domain/response/ProposalAccepted.java`, borrado en `StoryPageRepository.deleteAfter`, endpoint en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** `POST /api/v1/storyteller/{storyId}/pages/{id}/accept-proposal?confirm=true|false` (por defecto `false`), sin cuerpo. Es un endpoint aparte de `accept` porque `accept` es solo de páginas pendientes y este añade confirmación y contador. `{id}` es el id real de la portada o de la página aceptada reabierta K. El servicio es transaccional y calcula todo antes de mutar: comprueba página del cuento (404), estado aceptada, generación en curso, que haya propuestas y que la vigente no avise del número de frases (409), y cuenta las páginas con número mayor que K (la portada nunca tiene cascada). Sin confirmar y con posteriores no cambia nada y responde `200` con `requiresConfirmation=true` y `affectedPages`. Con `confirm=true`, o si no hay posteriores, K pasa a la propuesta vigente sin imagen importada, se borra su histórico y se eliminan las posteriores (con sus versiones por cascada de FK, revisiones y assets), sin iniciar ninguna generación; responde `200` con `requiresConfirmation=false` y el número de páginas eliminadas.

```java
// Páginas 1 a 4 aceptadas y una propuesta vigente para la 2 (id real de la fila)
ProposalAccepted ask = pageAcceptance.acceptProposal(storyId, page2Id, false);
ask.requiresConfirmation();   // true: no ha cambiado nada
ask.affectedPages();          // 2 (las páginas 3 y 4)

ProposalAccepted done = pageAcceptance.acceptProposal(storyId, page2Id, true);
done.requiresConfirmation();  // false: la 2 es la propuesta, sin imagen ni histórico; 3 y 4 ya no existen
// La siguiente página permitida es la 3 y se genera solo con generate
```

```
curl -X POST "http://localhost:8080/api/v1/storyteller/7/pages/41/accept-proposal"
# 200 {"success":true,"data":{"storyId":7,"pageId":41,"pageNumber":2,"status":"ACCEPTED","requiresConfirmation":true,"affectedPages":2}}
curl -X POST "http://localhost:8080/api/v1/storyteller/7/pages/41/accept-proposal?confirm=true"
# 200 {"success":true,"data":{"storyId":7,"pageId":41,"pageNumber":2,"status":"ACCEPTED","requiresConfirmation":false,"affectedPages":2}}
# Generación en curso, página no aceptada, sin propuestas o vigente con aviso de frases -> 409; página de otro cuento -> 404
```

## Cuento completo y listo para audio (PageAcceptance, PageGeneration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java` (`acceptPage`, `acceptProposal`, `startCover`, `startPage`; estado en `domain/enums/StoryStatus.java`)
**Para qué sirve:** al aceptar con `acceptPage` la página cuyo número coincide con el total de beats de la escaleta (la portada no cuenta), el cuento pasa a `StoryStatus.PAGES_ACCEPTED` (listo para la etapa de audio). Aceptar cualquier otra página o la portada no cambia el estado. Con el cuento en `PAGES_ACCEPTED`, `startCover` y `startPage` (cualquier número, también mayor que la escaleta) responden `409 "El cuento ya tiene todas las páginas aceptadas"` sin pedir turno ni llamar al escritor. Si se reabre una página K y se acepta su propuesta con cascada (`acceptProposal` con `confirm=true` que borra posteriores), el cuento vuelve a `SCRIPT_ACCEPTED` y se pueden pedir de nuevo las páginas K+1 en adelante; si K es la última (no se borra nada) sigue completo. El estado se guarda como texto (`story.status` es `VARCHAR(20)`, sin CHECK), sin migración. Para el cliente de audio (TASK-038): el literal es `PAGES_ACCEPTED`.

```java
// Guion de 8 páginas con las 1 a 7 aceptadas; page8Id es la fila real de la página 8 pendiente
pageAcceptance.acceptPage(storyId, page8Id);   // el cuento pasa a PAGES_ACCEPTED
pageGeneration.startPage(storyId, 9);          // AppException 409: ya tiene todas las páginas aceptadas
```

```
curl -X POST "http://localhost:8080/api/v1/storyteller/7/pages/48/accept"
# 200 {"success":true,"data":{"storyId":7,"pageId":48,"pageNumber":8,"status":"ACCEPTED"}}
curl -X POST "http://localhost:8080/api/v1/storyteller/7/pages/9/generate"
# 409 "El cuento ya tiene todas las páginas aceptadas"
# Consulta SQL: SELECT status FROM story WHERE id = 7;  -> PAGES_ACCEPTED
```

## Consulta de portada y páginas con acciones e imagen aceptada (PageQuery)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageQueryService.java` (puerto en `application/ports/in/persistence/PageQuery.java`, respuestas en `domain/response/StoryPagesView.java`, `PageDetail.java`, `AcceptedContent.java` y `PageVersionView.java`, acciones en `domain/enums/PageAction.java`, endpoints en `infrastructure/adapters/in/rest/web/StorytellerController.java`)
**Para qué sirve:** solo lectura. `GET /api/v1/storyteller/{storyId}/pages` y `GET .../pages/{id}` devuelven, para un cuento con escaleta, el estado del cuento, si hay generación en curso del cuento (`gate.isGeneratingStory`), `nextPage` (lo que se puede generar ahora: 0 la portada, N la página N; `null` si hay algo pendiente, el cuento está completo `PAGES_ACCEPTED`, el guion no está aprobado o hay generación en curso) y, por página (la portada es la 0, con `title` y sin texto), su estado, `beatId`, el contenido aceptado (`accepted`: texto, escena y `hasImage`; `null` si está pendiente), las `versions` si está pendiente o las `proposals` si es una aceptada reabierta (versión con texto, escena, aviso y `current`; nunca imagen) y las `actions` que calcula el servidor: pendiente sin aviso de frases `ACCEPT` y `HINT`; con aviso de frases `HINT` y `REGENERATE`, sin `ACCEPT` (la regeneración depende solo de la vigente); `SELECT_VERSION` si hay más de una versión o propuesta; aceptada sin propuestas solo `HINT` (así se reabre); con propuestas además `ACCEPT_PROPOSAL` (si la vigente no avisa de frases) y `DISCARD_PROPOSALS`. Con generación en curso del cuento todas las acciones van vacías. Los cuentos antiguos conservan exactamente su respuesta de `PageSummary`. La imagen del contenido aceptado se descarga con `GET .../pages/{id}/image` (bytes; 404 si la página no es del cuento, no está aceptada o no tiene imagen). No existe ninguna operación para escribir texto o escena a mano. Es un servicio aparte de `StoryPageService` para no ampliar su constructor.

```java
Optional<StoryPagesView> view = pageQuery.findPages(storyId);   // vacío en un cuento antiguo
PageDetail page = view.orElseThrow().pages().get(1);
page.status();      // PENDING
page.actions();     // [ACCEPT, HINT, SELECT_VERSION]
byte[] image = pageQuery.getAcceptedImage(storyId, acceptedPageId);   // 404 si no tiene
```

```
curl http://localhost:8080/api/v1/storyteller/7/pages
# {"success":true,"data":{"storyId":7,"status":"SCRIPT_ACCEPTED","generating":false,"nextPage":null,"pages":[
#   {"id":40,"page":0,"status":"ACCEPTED","beatId":null,"title":"El conejo blanco",
#    "accepted":{"text":null,"scene":"Luna en el claro","hasImage":true},
#    "versions":[],"proposals":[],"actions":["HINT"]},
#   {"id":41,"page":1,"status":"PENDING","beatId":12,"title":null,"accepted":null,
#    "versions":[{"id":101,"text":"Luna sale.","scene":"Luna en el claro","warning":"NONE","current":true}],
#    "proposals":[],"actions":["ACCEPT","HINT"]}]}}
curl http://localhost:8080/api/v1/storyteller/7/pages/40
# {"success":true,"data":{"id":40,"page":0,...}}   (un solo PageDetail)
curl -o portada.png http://localhost:8080/api/v1/storyteller/7/pages/40/image
# 200 con la imagen; sin imagen o página de otro cuento -> 404
```

## Invalidación de páginas por edición de beat (BeatEditorService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java` (borrado en `StoryPageService.deleteInvalidatedPages` y `StoryPageRepositoryAdapter.deleteByStory`)
**Para qué sirve:** el contrato de la edición de beat no cambia. Al confirmar la edición, `BeatEditorService` pide a `StoryPageService.deleteInvalidatedPages` retirar todas las páginas del cuento (portada incluida) en la misma transacción que guarda el beat y devuelve el cuento a `SCRIPT_PENDING` (también desde `PAGES_ACCEPTED`). El adaptador borra las revisiones antiguas, las páginas (sus `story_page_version` caen por `ON DELETE CASCADE`, incluidas las propuestas pendientes, y el contenido aceptado y la imagen importada van en la propia fila), vacía el resumen acumulado y retira los assets. `story_page.beat_id` usa `ON DELETE SET NULL` si se reemplaza la escaleta. Sin páginas generadas no se borra nada.

```java
BeatEditResult r = beatEditing.edit(new BeatEditCommand(storyId, 3, beat, true));   // confirmInvalidation = true
r.status();            // APPLIED
r.invalidatedPages();  // páginas retiradas (con sus versiones)
r.story().getStatus(); // SCRIPT_PENDING
```

```
# Sin confirmación: avisa con el contador y no cambia nada
curl -X PUT http://localhost:8080/api/v1/generations/story/7/beat/3 -H "Content-Type: application/json" -d '{"event":"...","narrativeFunction":"nudo","participants":["Luna"],"tone":"curioso","visualNote":"claro"}'
# Repetido con "confirmInvalidation": true -> páginas y versiones eliminadas
# SQL: SELECT count(*) FROM story_page WHERE story_id = 7;  -> 0
```

## Turno de generación en la validación de beat (BeatEditorService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java`
**Para qué sirve:** la edición de un beat pide turno a `GenerationGatePort` (tipo `DIRECTOR`, con el `storyId` del cuento) justo antes de llamar al director y lo libera en `finally`, tanto en éxito como en rechazo, aviso de confirmación o fallo. El turno cubre la validación y la aplicación (borrado de páginas y guardado del beat), de modo que una generación de página del mismo cuento no puede colarse a mitad. Si la puerta está ocupada, responde 409 "Hay una generación en curso" sin llamar al director ni modificar nada. La cancelación (`confirmInvalidation: false`) y las entradas inválidas no usan la puerta. El contrato no cambia salvo ese 409.

```java
try {
    BeatEditResult r = beatEditing.edit(new BeatEditCommand(storyId, 3, beat, null));
} catch (AppException e) {
    // e.getStatus() == HttpStatus.CONFLICT, mensaje "Hay una generación en curso"
}
// Durante la validación cualquier otra generación se rechaza:
// gate.tryAcquire(GenerationType.WRITER, storyId) -> false
```

```
# Con una página generándose (puerta ocupada):
curl -X PUT http://localhost:8080/api/v1/generations/story/7/beat/3 -H "Content-Type: application/json" -d '{"event":"...","narrativeFunction":"nudo","participants":["Luna"],"tone":"curioso","visualNote":"claro"}'
# 409 "Hay una generación en curso"
```

## Turno de generación en el guion y su revisión global (StoryService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java`
**Para qué sirve:** `generate` (crear el cuento y su escaleta) y `review` (revisión global por hint) son las únicas operaciones de `StoryService` que llaman al director, así que son las únicas que piden turno a `GenerationGatePort` (tipo `DIRECTOR`) y lo liberan en `finally`, en éxito y en error. En `generate` el turno va sin `storyId` (el cuento aún no existe) y cubre la llamada al director y la persistencia; en `review` va con el `storyId` y se pide antes de descartar la revisión pendiente, de modo que un rechazo no cambia nada. Con la puerta ocupada responden 409 "Hay una generación en curso" sin llamar al director. `approve` y `confirmReview` no llaman al director y no usan la puerta. El turno no se retiene tras responder. El contrato no cambia salvo ese 409.

```java
try {
    Story story = storyGeneration.generate(new StoryGenerateCommand("Amistad", "Un paseo", StorySize.S, 7L));
} catch (AppException e) {
    // e.getStatus() == HttpStatus.CONFLICT, mensaje "Hay una generación en curso"
}
// Durante la generación cualquier otra se rechaza:
// gate.tryAcquire(GenerationType.CHARACTER_AGENT, null) -> false
```

```
# Con una página generándose (puerta ocupada), crear un cuento:
curl -X POST http://localhost:8080/api/v1/generations/story -H "Content-Type: application/json" -d '{"topic":"Amistad","description":"Un paseo","size":"S"}'
# 409 "Hay una generación en curso"
# Revisión global por hint con la puerta ocupada:
curl -X POST http://localhost:8080/api/v1/reviews/story -H "Content-Type: application/json" -d '{"storyId":7,"hint":"Final en la playa"}'
# 409 "Hay una generación en curso"
```

## Turno de generación en el alta de personajes y la solicitud de retratos (CharacterCatalogService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java`
**Para qué sirve:** `create` (alta con el agente de personaje, automática o por frase) pide turno `CHARACTER_AGENT` a `GenerationGatePort` y lo libera en `finally`, en éxito, rechazo del agente y error. `generatePortraits` pide turno `PORTRAITS` y lo **entrega** a `PortraitGenerationService`: el catálogo no lo libera cuando el trabajo se acepta; lo libera `PortraitGenerationService` al terminar el trabajo (éxito o error) o si no consigue encolarlo. Solo si `generate` lanza una excepción antes de aceptar el trabajo (por ejemplo, ya hay un trabajo de ese personaje) el catálogo libera el turno. Con la puerta ocupada ambas operaciones responden 409 "Hay una generación en curso" sin llamar al agente, sin crear nada y sin crear ningún trabajo de retratos. El bloqueo por personaje (`PortraitLockService`) no cambia. El contrato no cambia salvo ese 409.

```java
try {
    catalog.create(new CatalogCharacterCreateCommand(null));   // alta automática
} catch (AppException e) {
    // e.getStatus() == HttpStatus.CONFLICT, mensaje "Hay una generación en curso"
}
catalog.generatePortraits(7L);   // la puerta queda ocupada (PORTRAITS) hasta que terminen los retratos
// gate.tryAcquire(GenerationType.WRITER, 7L) -> false mientras tanto
```

```
# Con una página generándose (puerta ocupada), alta automática:
curl -X POST http://localhost:8080/api/v1/catalog-characters
# 409 "Hay una generación en curso"
# Con la puerta ocupada, solicitar retratos:
curl -X POST http://localhost:8080/api/v1/catalog-characters/7/portraits/generate
# 409 "Hay una generación en curso" (no se crea trabajo)
```

## Turno de generación en la solicitud de revisión de ficha (CharacterReviewService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/CharacterReviewService.java`
**Para qué sirve:** `request` pide turno `CHARACTER_AGENT` (sin cuento) a `GenerationGatePort` justo antes de descartar la revisión pendiente y llamar al agente, y lo libera en `finally`, en éxito, rechazo del agente y error. Con la puerta ocupada responde 409 "Hay una generación en curso" sin llamar al agente y sin descartar ni modificar la revisión pendiente. Si hay una pendiente y no se confirma el reemplazo, devuelve la petición de confirmación sin pedir turno (no llama al agente). `resolve` (confirmar o descartar) no llama al agente y no usa la puerta. El contrato no cambia salvo ese 409.

```java
try {
    reviews.request(new CatalogCharacterReviewCommand(7L, CharacterReviewTarget.NARRATIVE, "más valiente", false));
} catch (AppException e) {
    // e.getStatus() == HttpStatus.CONFLICT, mensaje "Hay una generación en curso"
}
```

```
# Con un guion generándose (puerta ocupada), pedir un cambio de ficha:
curl -X POST http://localhost:8080/api/v1/catalog-characters/7/reviews -H "Content-Type: application/json" -d '{"target":"NARRATIVE","hint":"más valiente","replacePending":false}'
# 409 "Hay una generación en curso" (la revisión pendiente, si la hay, sigue intacta)
```

## Turno ocupado durante la generación de retratos (PortraitGenerationService)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationService.java`
**Para qué sirve:** el turno `PORTRAITS` que adquiere `CharacterCatalogService.generatePortraits` se conserva desde que se crea el trabajo hasta que termina: `PortraitGenerationService` lo libera en el `finally` del trabajo (éxito o error) y, si no consigue encolarlo, tras marcarlo fallido. Mientras tanto la portada, la página y la generación antigua responden 409 "Hay una generación en curso". El bloqueo por personaje (`PortraitLockService`) sigue funcionando a la vez: renombrar o pedir una revisión de ese personaje responde 409 mientras haya un trabajo en ejecución. Al terminar, ambos quedan libres. El contrato no cambia salvo ese 409.

```java
catalog.generatePortraits(7L);        // turno PORTRAITS ocupado y trabajo en ejecución
pageService.startPage(5L, 1);         // AppException 409 "Hay una generación en curso"
catalog.rename(7L, "Otra");           // AppException 409 (bloqueo por personaje)
// Al terminar el trabajo (éxito o error) el turno queda libre
pageService.startPage(5L, 1);         // GenerationStarted(5, 1)
```

```
curl -X POST http://localhost:8080/api/v1/catalog-characters/7/portraits/generate
curl -X POST http://localhost:8080/api/v1/storyteller/5/pages/1/generate
# 409 "Hay una generación en curso"
# Al terminar los retratos, la misma petición de página responde 202
```
