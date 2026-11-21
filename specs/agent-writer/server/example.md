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
**Para qué sirve:** Publica los cuatro eventos de una generación de portada o página: `STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED` (con el motivo del escritor en `reason`) y `FAILED`. Cada evento lleva `storyId` y `pageNumber` (0 es la portada); `reason` es `null` salvo en el rechazo del hint. Entrega el evento al puerto `StoryPageEventPublisher`; la difusión por `/storyteller` se conecta en TASK-009.

```java
notifier.started(storyId, 3);                          // inicio de la página 3
notifier.versionPresented(storyId, 0);                 // versión presentada de la portada
notifier.hintRejected(storyId, 2, "El hint contradice el guion");
notifier.failed(storyId, 3);                           // fallo de la página 3
```
