# Ejemplos: agent-director - server

## DirectorAgent (SpringAiDirectorAgent)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiDirectorAgent.java`
**Para qué sirve:** Pide al director un guion completo con escaleta, revisa la escaleta con un hint global y valida la edición directa de un beat. Cada resultado se valida antes de devolverse y una respuesta incompleta lanza `AppException`.

```java
// Generación: un beat por página interior, con los cinco campos obligatorios.
var script = director.generateScript(new DirectorScriptRequest(
        "Amistad", "Un paseo por el bosque", 6,
        "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa",
        7L)); // 7L: personaje de catálogo con retrato aprobado (opcional con el constructor de 6 argumentos)
script.beats().forEach(beat -> System.out.println(beat.event() + " / " + beat.tone()));

// Revisión global: devuelve la escaleta completa con el mismo número de beats.
var review = director.reviewScript(new DirectorReviewRequest(
        script.title(), script.synopsis(), script.cast(), script.beats(), "Que acabe en la playa"));
if (!review.hintAccepted()) {
    System.out.println("Rechazado: " + review.rejectionReason());
}

// Edición directa de un beat: veredicto de edad con motivo si no es apto.
var verdict = director.validateBeat(script.beats().get(0));
if (!verdict.accepted()) {
    System.out.println(verdict.reason());
}
```

## StoryOutlineRepository (StoryOutlineRepositoryAdapter)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryOutlineRepositoryAdapter.java`
**Para qué sirve:** Guarda y lee la escaleta de un cuento (tablas `story_outline`, `story_beat` y `episodic_cast_member`, migración `020_create_story_outline.xml`). El beat de la posición N se guarda como página N y se lee ordenado por página con sus cinco campos. El reparto ligero (nombre y función) cuelga de la escaleta y no toca el catálogo ni `character`. `save` reemplaza la escaleta previa y marca `story.script_version = 1`. Un cuento antiguo tiene `script_version` nulo y ninguna escaleta: `find` y `scriptVersion` devuelven vacío y no se altera.

```java
outlines.save(storyId, outline);               // StoryOutline de dominio; beat i -> página i + 1

Optional<StoryOutline> saved = outlines.find(storyId); // vacío = cuento antiguo
Optional<Integer> version = outlines.scriptVersion(storyId);   // vacío = cuento antiguo
```

## Modelo de escaleta (StoryOutline, StoryBeat, EpisodicCastMember, StoryScriptVersion)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/domain/model/StoryOutline.java` (junto a `StoryBeat.java`, `EpisodicCastMember.java` y `StoryScriptVersion.java`)
**Para qué sirve:** Tipos de dominio de la escaleta. `StoryBeat` lleva los cinco campos obligatorios y `isComplete()` detecta un beat incompleto antes de persistir; `EpisodicCastMember` solo tiene nombre y función (sin identificador de catálogo); `StoryOutline` agrupa reparto y beats (el beat i es la página i + 1) y su `isComplete()` es lo que valida `StoryOutlineRepository.save`, que lanza `IllegalArgumentException` si falla. `Story` expone `getScriptVersion()` (null en cuentos antiguos), `hasOutline()` y `getStatus()`; `StoryMapper` los copia en ambos sentidos sin tocar las listas antiguas de actores.

```java
var outline = new StoryOutline(
        List.of(new EpisodicCastMember("Bruno", "amigo del bosque")),
        List.of(new StoryBeat("Luna encuentra una piedra", "planteamiento",
                List.of("Luna"), "curioso", "claro del bosque")));

if (outline.isComplete()) {
    outlines.save(storyId, outline);           // marca story.scriptVersion = StoryScriptVersion.OUTLINE
}
boolean newFormat = story.hasOutline();        // false en cuentos antiguos
```

## director.modelfile

**Ruta:** `api/src/main/resources/prompts/director.modelfile`
**Para qué sirve:** Definición del modelo `storyteller-director` mantenida por el backend, con los modos GENERAR_GUION, REVISAR_GUION y VALIDAR_EDICION_BEAT. Sustituye a `agents/director-story-agent/Modelfile`; lo registra en Ollama `DirectorModelProvisioner`.

```text
MODO: GENERAR_GUION
Tema: Amistad - Un paseo por el bosque
Número de páginas interiores (beats): 6
Protagonista: nombre, descripción narrativa y descripción visual
```

## DirectorModelProvisioner

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisioner.java`
**Para qué sirve:** `ApplicationRunner` que al arrancar consulta Ollama y, si `storyteller-director` no existe, lo crea enviando el contenido de `prompts/director.modelfile` (classpath) como definición. Si ya existe no hace nada; si la consulta o la creación fallan, lanza `IllegalStateException` y aborta el arranque. Se desactiva con `app.agents.director-provisioning.enabled=false` (variable `APP_AGENT_DIRECTOR_PROVISIONING_ENABLED`). El nombre sale de `app.agents.director`.

```java
// Es un componente de Spring: no se invoca a mano. En pruebas, con el constructor de paquete:
var provisioner = new DirectorModelProvisioner(
        ollamaApi, restClient, "storyteller-director", new ClassPathResource("prompts/director.modelfile"));
provisioner.run(args); // crea el modelo solo si listModels() no lo contiene
```

## StoryReviewRepository (escaleta candidata)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/StoryReviewRepository.java`
**Para qué sirve:** La revisión de guion (`StoryReview`) guarda ahora la escaleta candidata completa (`candidateOutline`), el indicador de aceptación del hint, el motivo de rechazo y `changedBeats`: los números de página (desde 1) cuyo beat es realmente distinto del vigente. `createOutlineReview` calcula ese diff contra la escaleta vigente (vacío si son iguales; todas las páginas si aún no hay vigente); un hint rechazado se guarda `DISCARDED` con su motivo y sin escaleta. Mantiene el ciclo PENDING/CONFIRMED/DISCARDED: `changeStatus(storyId, CONFIRMED)` publica la candidata como escaleta vigente y `DISCARDED` no toca la vigente. Migración `021_add_outline_candidate_to_story_review.xml`.

```java
StoryReview review = reviews.createOutlineReview(storyId, "mas aventura", true, null, title, synopsis, candidate);
review.getChangedBeats();      // p. ej. [2, 5]
review.getCandidateOutline();  // escaleta completa propuesta

reviews.changeStatus(storyId, RevisionStatus.CONFIRMED); // la candidata pasa a ser la vigente
reviews.getPendingReview(storyId);                       // null: ya no hay pendiente
```

## BeatEditorService (edición directa de un beat)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java`
**Para qué sirve:** Detrás del puerto `BeatEditing`. Edita el beat de una página interior (página 1 = primer beat). El director solo da el veredicto de edad: si lo rechaza, devuelve `REJECTED` con el motivo y no cambia nada. Si es válido y el cuento tiene páginas generadas, devuelve `CONFIRMATION_REQUIRED` con el contador y no aplica nada hasta que llega `confirmInvalidation=true`; entonces, en una sola transacción, `StoryPageRepository.deleteByStory` elimina las páginas (con sus revisiones y assets, misma política que la revisión de páginas), se guarda el beat y el guion queda en `SCRIPT_PENDING`. Con `confirmInvalidation=false` devuelve `CANCELLED` sin efectos ni consulta al director. Sin páginas aplica directamente. El reparto ligero se conserva tal cual y nunca toca el catálogo. Endpoint: `PUT /api/v1/generations/story/{id}/beat/{page}` con `{event, narrativeFunction, participants, tone, visualNote, confirmInvalidation?}`.

```java
var beat = new StoryBeat("Luna regala la piedra", "nudo", List.of("Luna"), "alegre", "claro del bosque");

BeatEditResult first = editor.edit(new BeatEditCommand(storyId, 2, beat, null));
switch (first.status()) {
    case REJECTED -> System.out.println(first.reason());               // el beat no cambia
    case CONFIRMATION_REQUIRED -> {                                    // hay first.invalidatedPages() páginas
        editor.edit(new BeatEditCommand(storyId, 2, beat, true));      // confirmar: borra páginas y aplica
        // editor.edit(new BeatEditCommand(storyId, 2, beat, false));  // cancelar: sin efectos
    }
    case APPLIED -> System.out.println(first.story().getStatus());     // SCRIPT_PENDING
    default -> { }
}
```

## StoryScriptService (consulta del guion vigente)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryScriptService.java`
**Para qué sirve:** Detrás del puerto `StoryScriptReading`. Devuelve el guion vigente de un cuento (`StoryScript`: `id`, `title`, `synopsis`, `status`, `scriptVersion`, `cast` con `{name, role}` y `beats` con `{page, event, narrativeFunction, participants, tone, visualNote}`), para que la SPA muestre el guion pendiente o aprobado al reabrir un cuento. Es de solo lectura. Los beats van ordenados y con `page` explícito desde 1 (la misma numeración de `changedBeats` y de `PUT .../beat/{page}`). No incluye páginas. En un cuento antiguo (sin escaleta) `scriptVersion` es nulo y `cast` y `beats` van vacíos, sin error; un cuento inexistente lanza `ResourceNotFoundException` (404). Endpoint: `GET /api/v1/generations/story/{id}/script` (`ApiResponse<StoryScript>`).

```java
StoryScript script = scripts.getScript(storyId);
script.status();                       // SCRIPT_PENDING | SCRIPT_ACCEPTED
if (script.scriptVersion() == null) {
    // cuento antiguo: cast y beats vacíos, usar el panel anterior
} else {
    script.beats().forEach(b -> System.out.println(b.page() + ": " + b.event() + " (" + b.tone() + ")"));
    script.cast().forEach(c -> System.out.println(c.name() + " - " + c.role()));
}
```

## StoryService (guion con protagonista obligatorio y escaleta)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java`
**Para qué sirve:** Casos de uso del guion detrás de `StoryGeneration`. `generate` exige un protagonista de catálogo con retrato aprobado: el de `catalogCharacterId` o, si es `null`, uno al azar entre los disponibles (con retrato aprobado y sin generación de retratos en curso); si no hay ninguno lanza `AppException` y no crea el cuento. Envía al director la ficha del protagonista y reintenta (hasta 3 veces) una escaleta inválida sin persistir nada; solo con escaleta completa guarda el cuento (con la instantánea del protagonista como único personaje), la escaleta y el reparto ligero, que nunca pasa por el catálogo. `approve` pasa `SCRIPT_PENDING` a `SCRIPT_ACCEPTED`. `review` envía el hint al director: aceptado crea una revisión `PENDING` con la candidata; rechazado crea una revisión con el motivo y la escaleta vigente queda intacta. `confirmReview` con `CONFIRMED` publica la candidata, aplica título y sinopsis y deja el guion en `SCRIPT_PENDING`. Los cuentos antiguos (sin escaleta) no admiten aprobar ni revisar el guion. Endpoints: `POST /api/v1/generations/story` (`{topicId, size, catalogCharacterId?}`), `POST /api/v1/generations/story/{id}/approve`, `POST /api/v1/reviews/story`, `GET /api/v1/reviews/story/{id}` y `POST /api/v1/reviews/story/confirm`.

```java
// Protagonista concreto o al azar (catalogCharacterId = null)
Story story = stories.generate(new StoryGenerateCommand(topicId, StorySize.S, 7L));
Story random = stories.generate(new StoryGenerateCommand(topicId, StorySize.S));

stories.approve(story.getId());                                   // SCRIPT_ACCEPTED

StoryReview review = stories.review(new StoryReviewCommand(story.getId(), "Que acabe en la playa"));
if (review.isHintAccepted()) {
    review.getChangedBeats();                                      // beats cambiados de la candidata
    stories.confirmReview(new ConfirmReviewRequestDto(story.getId(), RevisionStatus.CONFIRMED));
} else {
    System.out.println(review.getRejectedReason());                // la escaleta vigente no cambia
}
```

## StoryPageService (páginas con guion aprobado y beat)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`
**Para qué sirve:** Antes de generar portada o página interior exige el guion en `SCRIPT_ACCEPTED`; si está pendiente lanza `AppException` (409) indicando que debe aprobarse. La portada sigue usando sinopsis y reparto y no consume beat. Para una página interior N resuelve el beat N de la escaleta (`StoryOutlineRepository.find`) y lo pasa íntegro al escritor en el parámetro `beat` de `prompts/new_page.st` (qué ocurre, función narrativa, quiénes aparecen, emoción o tono y nota visual). Los cuentos antiguos sin escaleta conservan su flujo anterior: no exigen aprobación ni reciben beat. `deleteInvalidatedPages` retira todas las páginas del cuento (delega en `StoryPageRepository.deleteByStory`) y devuelve cuántas se retiraron.

```java
// Con el guion en SCRIPT_PENDING lanza AppException("Debe aprobarse el guion ...")
stories.approve(storyId);                                       // SCRIPT_ACCEPTED
StoryPage cover = pageService.generate(new StoryPageGenerateCommand(storyId)); // portada, sin beat
StoryPage page1 = pageService.generate(new StoryPageGenerateCommand(storyId)); // página 1 -> beat 1

int removed = pageService.deleteInvalidatedPages(storyId);      // número de páginas retiradas
```
