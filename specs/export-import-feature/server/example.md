# Ejemplos de uso: export-import-feature (server)

## CoverPlan

Planteamiento de la portada (lugar y tono propios), separado del beat de la página 1. Vive en `StoryOutline.cover()`; es `null` en los cuentos anteriores a la feature.

Ruta: `api/src/main/java/es/vargontoc/storyteller/domain/model/CoverPlan.java`

```java
var beats = List.of(
        new StoryBeat("Luna sale", "inicio", List.of("Luna"), "alegre", "bosque", "cocina de la abuela"));
var cover = new CoverPlan("habitación al anochecer", "calma");

outlineRepository.save(storyId, new StoryOutline(List.of(), beats, cover));

StoryOutline outline = outlineRepository.find(storyId).orElseThrow();
outline.cover();                    // CoverPlan("habitación al anochecer", "calma"), o null en cuentos antiguos
outline.beats().get(0).setting();   // "cocina de la abuela", o null en cuentos antiguos
```

Los constructores antiguos `new StoryBeat(5 campos)` y `new StoryOutline(cast, beats)` siguen disponibles y dejan `setting` y `cover` a `null`.

## Resultado del director con lugar por beat y planteamiento de portada

`DirectorBeat` incorpora `setting`; `DirectorScriptResult` y `DirectorReviewResult` incorporan `cover` (`CoverPlan`). Sirve tanto al crear el guion como en la revisión global con hint aceptado. El adaptador entrega los valores tal cual: si el director omite alguno llegan a `null` (la exigencia de no vacío es de `StoryService`, TASK-004).

Rutas: `api/src/main/java/es/vargontoc/storyteller/domain/response/DirectorBeat.java`, `DirectorScriptResult.java`, `DirectorReviewResult.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiDirectorAgent.java`

```java
DirectorScriptResult script = director.generateScript(request);
script.beats().get(0).setting();   // "cocina de la abuela", o null si el director no lo devolvió
script.cover();                    // CoverPlan("habitación al anochecer", "calma"), o null

DirectorReviewResult review = director.reviewScript(reviewRequest);
if (review.hintAccepted()) {
    review.cover();                // planteamiento de portada de la escaleta candidata
}
```

Los constructores antiguos (`DirectorBeat` de 5 campos, `DirectorScriptResult` de 4, `DirectorReviewResult` de 6) siguen disponibles y dejan los campos nuevos a `null`.

## DirectorModelProvisioner: Modelfile del director con lugar y portada

`DirectorModelProvisioner` se ejecuta al arrancar y registra siempre el modelo del director con `prompts/director.modelfile`; si ya existía en Ollama, `/api/create` lo reemplaza, de modo que el modelo antiguo recibe las reglas nuevas sin intervención manual. El Modelfile exige un `setting` no vacío en cada beat y un `cover` con `setting` y `tone` propios, distintos de los del primer beat, tanto al generar como al revisar el guion. Ya no consulta la lista de modelos.

Rutas: `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisioner.java` y `api/src/main/resources/prompts/director.modelfile`

```java
// Automático (ApplicationRunner); en una prueba, con el RestClient a Ollama sustituido:
new DirectorModelProvisioner(restClient, "storyteller-director",
        new ClassPathResource(DirectorModelProvisioner.MODELFILE_PATH)).run(args);
// POST /api/create { model, from, system, parameters, stream:false }
```

Se desactiva con `app.agents.director-provisioning.enabled=false`.

## Escritor: escena de portada desde su lugar y su tono

`WriterContext` incorpora `coverPlan` (`CoverPlan`). `WriterContext.cover(title, synopsis, protagonist, coverPlan)` lo rellena y `SpringAiWriterAgent` añade al mensaje "Lugar de la portada" y "Tono de la portada", tanto en `writeCover` como en `rewrite` con hint. El hint aceptado devuelve una escena completa; el rechazado, el motivo sin escena. Con `coverPlan` a `null` (cuentos antiguos) no se añade nada; el rechazo por falta de planteamiento es de `StoryPageService` (TASK-008).

Rutas: `api/src/main/java/es/vargontoc/storyteller/domain/request/WriterContext.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java`

```java
var context = WriterContext.cover("El bosque de Luna", "Luna ayuda a un amigo", "Luna, zorra naranja",
        new CoverPlan("habitación al anochecer", "calma"));

WriterResult cover = writer.writeCover(context);   // cover.scene() con la descripción completa

WriterHintResult hint = writer.rewrite(context, cover, "que se vea la luna llena");
if (hint.hintAccepted()) {
    hint.scene();                  // nueva escena completa
} else {
    hint.rejectionReason();        // motivo del rechazo
}
```

El constructor de 6 campos y `WriterContext.cover(title, synopsis, protagonist)` siguen disponibles y dejan `coverPlan` a `null`.

## WriterModelProvisioner: Modelfile del escritor con las reglas de la portada

`WriterModelProvisioner` se ejecuta al arrancar y registra siempre el modelo del escritor con `prompts/writer.modelfile`; si ya existía en Ollama, `/api/create` lo reemplaza, de modo que el modelo antiguo recibe las reglas nuevas (igual que el director). Ya no consulta la lista de modelos. El Modelfile indica que la escena de portada ocurre en el "Lugar de la portada" y con el "Tono de la portada", sin copiar los de la página 1, y que con un hint se devuelve la descripción de escena completa.

Rutas: `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/WriterModelProvisioner.java` y `api/src/main/resources/prompts/writer.modelfile`

```java
// Automático (ApplicationRunner); en una prueba, con el RestClient a Ollama sustituido:
new WriterModelProvisioner(restClient, "storyteller-writer",
        new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH)).run(args);
// POST /api/create { model, from, system, parameters, stream:false }
```

Se desactiva con `app.agents.writer-provisioning.enabled=false`.

## StoryService: validación del guion ampliado

`StoryService` exige, al crear el guion (`generate`) y al aceptar una revisión global (`review`), que cada beat tenga `setting` no vacío y que la portada exista con `setting` y `tone` no vacíos (un valor de solo espacios cuenta como vacío). Si falta algo, la salida del director se trata como la de campos obligatorios ausentes: se reintenta hasta 3 veces y, si sigue incompleta, responde 400 sin persistir el guion ni la revisión. Un guion válido se persiste con los lugares y la portada. Las escaletas antiguas ya guardadas no se validan de nuevo.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java`

```java
// Director simulado: guion completo
when(director.generateScript(any())).thenReturn(new DirectorScriptResult("T", "S", cast,
        List.of(new DirectorBeat("Pasa algo", "inicio", List.of("Luna"), "alegre", "bosque", "claro del bosque")),
        new CoverPlan("camino al bosque", "misterioso")));
service.generate(command);      // persiste la escaleta con setting por beat y cover

// Un beat sin lugar, o una portada sin tono, no se persiste
// -> AppException 400 "El agente no devolvió una respuesta válida tras 3 intentos"
```

## BeatEditorService: lugar de escena en la edición directa

`BeatEditorService.edit` trata `setting` como un campo más del beat: es obligatorio y no puede estar en blanco (si falta, 400 sin consultar al director ni guardar). El lugar se envía también al director para la validación de edad y se guarda con el beat. El planteamiento de portada de la escaleta (`cover`) se conserva tal cual. En la API, `BeatEditRequestDto` incluye el campo `setting`.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java`

```java
StoryBeat edited = new StoryBeat("Luna cocina", "nudo", List.of("Luna"), "alegre", "cocina", "cocina de la abuela");
service.edit(new BeatEditCommand(storyId, 2, edited, null));
// beat 2 guardado con setting "cocina de la abuela"; cover intacto

service.edit(new BeatEditCommand(storyId, 2,
        new StoryBeat("Luna cocina", "nudo", List.of("Luna"), "alegre", "cocina", "  "), null));
// -> AppException 400 "El beat debe tener el lugar de la escena"; el beat conserva el lugar anterior
```

## ImageContentInspector

Decide si unos bytes son una imagen por su contenido (firma del formato), sin mirar nombre, tipo declarado, dimensiones ni peso, y sin imponer límites. Reconoce los formatos de `ImageIO` (PNG, JPEG, BMP, GIF...) y WebP.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/ImageContentInspector.java`

```java
byte[] content = file.getBytes();   // p. ej. un MultipartFile

if (!imageContentInspector.isImage(content)) {
    throw new IllegalArgumentException("no es una imagen");   // se conserva la imagen anterior
}
// ...sustituir el asset de la portada o página aceptada
```

Un texto con extensión `.png` devuelve `false`; un PNG llamado `.txt` devuelve `true`.

`identify` deduce además el formato real y su extensión habitual solo de los bytes (sin mirar nombre ni tipo declarado y sin modificarlos); devuelve vacío si no es una imagen. Todo formato aceptado tiene extensión: PNG `.png`, JPEG `.jpg`, BMP `.bmp`, GIF `.gif`, WebP `.webp`...

```java
byte[] original = storage.getResource(page.getImportedImage()).getContentAsByteArray();

ImageContentInspector.ImageFormat format = imageContentInspector.identify(original).orElseThrow();
format.name();        // "PNG" (aunque se subiera como "foto.jpg")
format.extension();   // ".png"

String entryName = "pages/001/image" + format.extension();   // nombre en el zip, bytes sin convertir
```

## StorySlugger

Deriva un slug determinista desde un texto: minúsculas, tildes, diéresis y `ñ` a ASCII, cada secuencia no alfanumérica en un guion, sin guiones en los extremos y recortado a 40 caracteres sin guion final. Sirve para el `storyId` (desde el título) y el `id` del protagonista (desde el nombre de la instantánea). No inventa identificadores: si el resultado tiene menos de 3 caracteres, `isValid` devuelve `false`.

Ruta: `api/src/main/java/es/vargontoc/storyteller/domain/service/StorySlugger.java`

```java
var slugger = new StorySlugger();

String storyId = slugger.slugify("La luna dormida");              // "la-luna-dormida"
String heroId  = slugger.slugify("Niña Ñoño, ¡el pingüino!");     // "nina-nono-el-pinguino"

if (!slugger.isValid(slugger.slugify("Yo"))) {                     // "yo" < 3 caracteres
    // informar de la falta; no se inventa otro identificador
}
```

## StoryPageService: portada con su planteamiento

La generación de la portada (`startCover`) y el hint sobre la portada pendiente o aceptada (`startHint` con la página 0) mandan al escritor el lugar y el tono propios de la portada (`CoverPlan` de la escaleta aprobada), no los del beat de la página 1. Si el guion no tiene lugar y tono de portada (cuento anterior a la feature), la petición se rechaza con 409 antes de pedir turno y sin llamar al escritor. El turno, las versiones y los eventos (STARTED, VERSION_PRESENTED, HINT_REJECTED, FAILED) son los de las páginas.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`

```java
GenerationStarted started = storyPageService.startCover(storyId);   // pageNumber 0; la escena llega por evento

storyPageService.startHint(storyId, coverPageId, "que se vea la luna llena");
// -> el escritor recibe WriterContext.cover(..., coverPlan("habitación al anochecer", "calma"))

// Cuento legacy sin planteamiento de portada:
// -> AppException 409 "Al guion le faltan el lugar y el tono de la portada"; sin turno ni escritor
```

## StoryPageService: página 1 bloqueada hasta aceptar la portada

En un cuento con escaleta, la página 1 solo se admite cuando la portada (página 0) tiene contenido aceptado. Con la portada pendiente, o sin portada, `startPage` (y la operación síncrona `generate`) responden 409 "Primero hay que aceptar la portada" sin pedir turno ni llamar al escritor. Aceptar la portada no encadena la página 1: sigue generándose bajo demanda. Una portada aceptada y reabierta con hint conserva su estado `ACCEPTED`, así que la página 1 no vuelve a bloquearse.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`

```java
storyPageService.startPage(storyId, 1);
// portada pendiente o inexistente -> AppException 409 "Primero hay que aceptar la portada"

storyPageService.acceptPage(storyId, coverPageId);   // solo acepta; no genera la página 1
storyPageService.startPage(storyId, 1);              // ahora sí se admite

storyPageService.startHint(storyId, coverPageId, "que se vea la luna llena");   // reabre la portada
storyPageService.startPage(storyId, 1);              // sigue admitida (la portada continúa aceptada)
```

## StoryPageService: subida de imagen en la portada y en las páginas aceptadas

`PageAcceptance.uploadImage` (implementado por `StoryPageService`) asocia una imagen al contenido aceptado de la portada (página 0) o de una página interior y sustituye la anterior. Solo admite portadas o páginas aceptadas (409 si están pendientes) y consulta a `ImageContentInspector` por el contenido real (415 "El fichero no es una imagen" si no lo es). Las validaciones van antes de tocar el almacén, de modo que un rechazo conserva la imagen anterior. No hay límite de tamaño propio y el fichero se guarda tal cual (sin convertir ni redimensionar, sin extensión: el formato real se deduce del contenido al exportar). `ResourceStorage.saveImportedImage` escribe la nueva y retira la anterior; la ruta se persiste en `imported_image` de la página, así que se sigue sirviendo tras reiniciar (`GET .../pages/{id}/image`). El límite multipart de Spring sube a 100 MB por fichero (`spring.servlet.multipart`).

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/out/ResourceStorage.java`, `api/src/main/java/es/vargontoc/storyteller/infrastructure/storage/ResourceStorageAdapter.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/web/StorytellerController.java`

```java
// Caso de uso
StoryPage page = pageAcceptance.uploadImage(storyId, pageId, file.getBytes());
page.getImportedImage();   // ruta de la imagen original guardada

// Rechazos (la imagen anterior no cambia)
pageAcceptance.uploadImage(storyId, pendingPageId, png);   // AppException 409
pageAcceptance.uploadImage(storyId, pageId, textBytes);    // AppException 415 "El fichero no es una imagen"
```

```
POST /api/v1/storyteller/{storyId}/pages/{id}/image   (multipart/form-data, parte "file")
200 -> { storyId, pageId, page, status }   404 página ajena   409 no aceptada   415 no es imagen
```

## StoryPageService: portada fuera de la cascada y retirada de imágenes al reaprobar

`acceptProposal` sobre la portada (página 0) no aplica cascada ni pide confirmación: la portada pasa a la propuesta vigente, pierde solo su imagen importada (referencia y fichero) y las páginas interiores aceptadas conservan texto, escena, imagen y audio. Sobre una página interior, la página pierde su imagen y, con confirmación, las posteriores se eliminan con sus assets. Los cambios interiores no tocan la imagen de la portada. `ResourceStorage.deleteImportedImage` borra el fichero de la imagen importada de una página; `deletePageAssets` ahora borra de verdad los directorios de la página y de las descendientes aunque no estén vacíos (antes `deleteIfExists` los dejaba huérfanos). La edición heredada de una página (`StoryPageRepository.update`) también retira la referencia `imported_image`, porque su directorio de assets se elimina.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/out/ResourceStorage.java`, `api/src/main/java/es/vargontoc/storyteller/infrastructure/storage/ResourceStorageAdapter.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryPageRepositoryAdapter.java`

```java
// Portada: sin cascada, sin confirmación; solo cambia su contenido y se retira su imagen
ProposalAccepted cover = pageAcceptance.acceptProposal(storyId, coverPageId, false);
cover.affectedPages();   // 0; las páginas 1..N siguen aceptadas, con su imagen y audio

// Página interior 2 con posteriores: pide confirmar y, al confirmar, retira su imagen y elimina las posteriores
pageAcceptance.acceptProposal(storyId, page2Id, false);   // requiresConfirmation = true, affectedPages = 2
pageAcceptance.acceptProposal(storyId, page2Id, true);    // la portada conserva su imagen

// Almacén
storage.deleteImportedImage(storyId, pageId);             // borra el fichero de la imagen importada
```

## StoryProtagonistSnapshotReader: instantánea del protagonista del cuento

Puerto de lectura del protagonista tal como quedó en el cuento al crearlo: nombre, descripción visual y ruta del retrato (`StoryProtagonistSnapshot`). Lee el personaje principal del propio cuento, nunca el catálogo vivo: si el protagonista del catálogo se edita después, la lectura sigue devolviendo la instantánea. `portraitPath` es `null` si la instantánea no tiene retrato; devuelve vacío si el cuento no tiene protagonista y lanza `ResourceNotFoundException` si el cuento no existe.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/StoryProtagonistSnapshotReader.java`, `api/src/main/java/es/vargontoc/storyteller/domain/model/StoryProtagonistSnapshot.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryProtagonistSnapshotAdapter.java`

```java
StoryProtagonistSnapshot hero = snapshotReader.find(storyId).orElseThrow();

hero.name();               // "Luna" (el de la instantánea, aunque el catálogo ahora diga otro)
hero.visualDescription();  // descripción visual de la instantánea
Resource portrait = storage.getResource(hero.portraitPath());   // retrato de la instantánea
```

## StoryPageQueryService: descripción de escena completa en la vista

`StoryPageQueryService.findPages` y `findPage` devuelven en `accepted.scene` la descripción de escena aceptada de cada página, completa y sin recortar. La portada (página 0) es independiente de la página 1: su vista trae su propia descripción, no la del primer beat. Una página pendiente no tiene contenido aceptado (`accepted` es `null`).

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageQueryService.java` y `api/src/main/java/es/vargontoc/storyteller/domain/response/StoryPagesView.java`

```java
StoryPagesView view = pageQuery.findPages(storyId).orElseThrow();

view.pages().get(0).accepted().scene();   // escena completa de la portada
view.pages().get(1).accepted().scene();   // escena completa de la página 1 (distinta)

pageQuery.findPage(storyId, pageId).orElseThrow().accepted().scene();   // la de esa página
```

## CharacterBackgroundRemover: fondo del retrato con ComfyUI

Puerto que quita el fondo de un retrato y devuelve el PNG transparente. `ComfyUiBackgroundRemover` sube los bytes con `ComfyUIClient.upload`, lanza el workflow `comfyui/workflow-remove-background.json` (LoadImage, "Image Remove Background (rembg)" con `app.comfy.character-background-model`, SaveImage), sondea hasta `app.comfy.timeout-seconds` y descarga el PNG sin reescalarlo. No usa `generation-gate` ni reintenta: cualquier fallo (ComfyUI caído, sin resultado, timeout) devuelve `Optional.empty()`, que es el resultado identificable de "imagen no obtenida". Requiere el nodo rembg (WAS Node Suite) instalado en ComfyUI.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterBackgroundRemover.java`, `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/ComfyUiBackgroundRemover.java` y `api/src/main/resources/comfyui/workflow-remove-background.json`

```java
StoryProtagonistSnapshot hero = snapshotReader.find(storyId).orElseThrow();
byte[] portrait = storage.getResource(hero.portraitPath()).getContentAsByteArray();

Optional<byte[]> cutout = backgroundRemover.removeBackground(portrait);
if (cutout.isEmpty()) {
    // no se pudo obtener la imagen: informar de la falta, sin reintentar
} else {
    byte[] png = cutout.get();   // mismo tamaño que el retrato, fondo transparente
}
```

## VisualContractValidator: validador del contrato v0.1 del agente visual

Valida en una sola pasada un `story.json` y la imagen del personaje contra la sección 5 de `docs/contracts/agent-visual/input.md` y devuelve todas las reglas incumplidas, sin detenerse en la primera. Recibe los bytes del `story.json` y los ficheros del zip por ruta relativa a su raíz (que hace de `input/`). Comprueba UTF-8 sin BOM, JSON sin claves duplicadas, campos fijos (`schemaVersion` "0.1", `language` "es", `ageRange` "3-4"), paleta base sin `colors`, `id` slug y únicos, protagonista único, escenas `cover` y `scene`, referencias de personaje existentes y la imagen (PNG real por cabecera, alfa con esquinas transparentes, al menos 1024 × 1024 px y 1:1). Cada error trae `code` estable del contrato, `path` JSONPath y `message`; además de los códigos de la sección 7 emite `STORY_JSON_INVALID`, `CHARACTER_ID_INVALID`, `CHARACTER_NAME_MISSING`, `CHARACTER_TRAITS_INVALID`, `SCENE_ID_INVALID` y `SCENE_SETTING_MISSING`. Usa `JsonMapperFactory` del framework para leer el JSON; no usa `Validator`/`ValidationResult` porque su enum de códigos es cerrado.

Ruta: `api/src/main/java/es/vargontoc/storyteller/application/service/VisualContractValidator.java`

```java
byte[] storyJson = ...;                                  // story.json compuesto
Map<String, byte[]> files = Map.of("characters/nina/character.png", pngBytes);

VisualContractValidator.Result result = contractValidator.validate(storyJson, files);

if (!result.isValid()) {
    for (VisualContractValidator.Error e : result.errors()) {
        // e.code() "CHARACTER_IMAGE_TOO_SMALL", e.path() "$.characters[0].image", e.message()
    }
}
```

## NubiGameExporter: zip para Nubi Game con rutas explícitas e imágenes originales

Compone el zip de Nubi Game a partir de un `NubiGameStory` (título, ruta de la portada y páginas con número, texto aceptado, ruta de imagen y ruta de audio, todas del almacén). Devuelve los bytes del zip, armado en memoria: no escribe ficheros ni lo persiste. Contenido: `story.json` (`title`, `cover` y `pages` en orden con `page`, `text`, `image`, `audio`; rutas relativas a la raíz y sin campo de formato), `cover.<ext>` (sin audio) y, por página, `pages/001/image.<ext>` y `pages/001/audio.wav`. Cada imagen se copia byte a byte (sin convertir, redimensionar ni reorientar) y su extensión sale de `ImageContentInspector.identify`, no del nombre con que se subió. El audio interior se copia tal cual. Si falta un fichero o una imagen no lo es, lanza `IllegalStateException`: la comprobación de faltas previa es de la exportación (TASK-025). Sustituye a `ResourceStorage.downloadStory` (convención `page_N`), ya retirado junto con la operación pública anterior (TASK-025).

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/NubiGameExporter.java` y `api/src/main/java/es/vargontoc/storyteller/domain/model/NubiGameStory.java`

```java
var story = new NubiGameStory("La luna dormida", coverPath, List.of(
        new NubiGameStory.Page(1, "Luna salió al bosque.", imagePath1, audioPath1),
        new NubiGameStory.Page(2, "Encontró un amigo.", imagePath2, audioPath2)));

byte[] zip = nubiGameExporter.export(story);
// story.json, cover.png, pages/001/image.jpg, pages/001/audio.wav, pages/002/...
```

```json
{
  "title": "La luna dormida",
  "cover": "cover.png",
  "pages": [
    { "page": 1, "text": "Luna salió al bosque.", "image": "pages/001/image.jpg", "audio": "pages/001/audio.wav" }
  ]
}
```

## StoryService: aprobar el guion lanza la portada

`StoryService.approve` aprueba primero el guion (`SCRIPT_ACCEPTED`) y después pide la portada con `CoverGeneration.startCover`, la operación existente de `StoryPageService`: no bloquea (la escena llega por WebSocket) y no genera la página 1. Devuelve `StoryApproval(story, coverStarted)`. Si la portada no puede empezar (turno ocupado, planteamiento de portada ausente, fallo al lanzarla), la aprobación se completa igualmente con `coverStarted = false` y no hay reintento: la portada se pide a mano con la operación de siempre. `approve` no usa transacción propia para que la aprobación quede confirmada antes de pedir la portada y un rechazo de esta no la revierta. `StoryService` depende del puerto `CoverGeneration`, no de `StoryPageService`, y `StoryPageService` no depende de `StoryService`, así que no hay dependencia circular.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/in/generator/StoryGeneration.java`, `api/src/main/java/es/vargontoc/storyteller/domain/response/StoryApproval.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/web/GenerationsController.java`

```java
StoryApproval approval = storyGeneration.approve(storyId);

approval.story().getStatus();   // SCRIPT_ACCEPTED, también si la portada no empezó
if (approval.coverStarted()) {
    // la descripción de la portada llega por WebSocket (VERSION_PRESENTED / FAILED)
} else {
    // ofrecer pedir la portada a mano: POST de generación de portada
}
```

```
POST /api/v1/generations/story/{id}/approve
200 -> { data: { story: { ...cuento aprobado }, coverStarted: true|false } }
```

## StoryService: rehacer el guion descarta la portada y su imagen

"Rehacer el guion" es el mecanismo vigente: confirmar una revisión global por hint (`StoryService.confirmReview` con `CONFIRMED`) que publica la escaleta nueva y deja el guion en `SCRIPT_PENDING`. Justo después, `StoryService` pide `CoverGeneration.discardCover`, que `StoryPageService` implementa: borra las versiones y la página de la portada (su contenido aceptado) y su imagen importada con `ResourceStorage.deleteImportedImage`. Sin portada no hace nada; con una generación del cuento en curso responde 409 y la confirmación se revierte entera (misma transacción). Las páginas interiores no se tocan. Al aprobar de nuevo el guion, `approve` pide la portada como siempre y se genera una descripción nueva pendiente de revisión. Descartar la revisión, rechazar el hint o editar un beat directamente no pasan por aquí. `StoryPageRepositoryAdapter.delete` retira ahora también las revisiones antiguas de la página (sin cascada en BD).

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java`, `api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/in/generator/CoverGeneration.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryPageRepositoryAdapter.java`

```java
// Confirmar la escaleta candidata rehace el guion y descarta la portada
storyService.confirmReview(new ConfirmReviewRequestDto(storyId, RevisionStatus.CONFIRMED));
// -> portada sin versiones, sin contenido aceptado y sin imagen importada

storyService.approve(storyId);   // genera una portada nueva, pendiente de revisión
```

## VisualAgentExporter: story.json y zip para el agente visual

Compone el zip de entrada del agente visual (contrato v0.1) a partir de un `VisualAgentStory`: título, protagonista de la instantánea (`StoryProtagonistSnapshot`), escena de la portada (`Cover`: descripción, lugar y tono propios) y una `Page` por página interior (texto, descripción, lugar y tono del beat). Genera `story.json` con campos fijos (`schemaVersion` "0.1", `language` "es", `ageRange` "3-4", paleta base sin `colors`), `title` y `storyId` (slug del título), un único personaje `protagonist` con `id` (slug del nombre) y de 1 a 5 `immutableTraits` literales de su descripción visual (fragmentos separados por coma, punto y coma, punto o salto de línea), la escena `cover-01` (texto = título, `emotion` = tono de la portada) y `scene-01`, `scene-02`... en orden, todas referenciando solo al protagonista; los textos se recortan de espacios sobrantes. Carga el retrato de la instantánea con `ResourceStorage.getResource`, le quita el fondo con `CharacterBackgroundRemover` y lo incluye como `characters/<id>/character.png`. Antes de generar el zip pasa el resultado a `VisualContractValidator`: si hay errores devuelve `Result` sin zip y con todos ellos; si el retrato no existe o no se pudo obtener añade `CHARACTER_IMAGE_MISSING` o `CHARACTER_IMAGE_NOT_OBTAINED` a las demás faltas. Todo en memoria: no persiste nada. La operación pública y la evaluación de condiciones previas son de TASK-030.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/VisualAgentExporter.java` y `api/src/main/java/es/vargontoc/storyteller/domain/model/VisualAgentStory.java`

```java
var hero = snapshotReader.find(storyId).orElseThrow();
var story = new VisualAgentStory("La luna dormida", hero,
        new VisualAgentStory.Cover("Nina mira la luna desde la ventana", "habitación al anochecer", "calma"),
        List.of(new VisualAgentStory.Page("Nina saluda a la luna.", "Nina en el jardín bajo una luna grande",
                "jardín de noche", "curiosidad")));

VisualAgentExporter.Result result = visualAgentExporter.export(story);
if (result.isValid()) {
    byte[] zip = result.zip();   // story.json y characters/nina/character.png
} else {
    for (VisualContractValidator.Error e : result.errors()) {
        // e.code() "CHARACTER_IMAGE_TOO_SMALL" o "CHARACTER_IMAGE_NOT_OBTAINED", e.path(), e.message()
    }
}
```

## StoryExportService: exportación para Nubi Game con evaluación completa de faltas

`StoryExport.exportForNubiGame` (implementado por `StoryExportService`) sustituye a la descarga heredada (`GET /{storyId}/download`, `StorytellerUseCase.downloadStory` y `ResourceStorage.downloadStory` con la convención `page_N` y la conversión a mp3, ya retirados). Comprueba en una sola pasada la portada (aceptada y con lugar y tono de portada en el guion; si no, `COVER_NOT_ACCEPTED` "portada sin aceptar", también en los cuentos anteriores a la feature), cada página interior aceptada con texto (`ACCEPTED_TEXT`, también las no generadas aún), la imagen de la portada y la imagen y el audio de cada página (`IMAGE`, `AUDIO`; el audio sale de `NarrationRepository.findAudioPath`; la portada no lleva audio). Acumula todas las faltas, cada una con su portada (`page` 0) o página; si hay alguna devuelve el rechazo sin zip y, si no, el zip de `NubiGameExporter`. Solo rechaza por disponibilidad: el formato de las imágenes (PNG, JPEG, BMP...) no es motivo de rechazo. Lanza `ResourceNotFoundException` si el cuento no existe. `ExportResult` y `ExportFault` son genéricos y sirven también a la exportación visual (TASK-030).

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryExportService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/in/export/StoryExport.java`, `api/src/main/java/es/vargontoc/storyteller/domain/response/ExportResult.java`, `api/src/main/java/es/vargontoc/storyteller/domain/response/ExportFault.java`, `api/src/main/java/es/vargontoc/storyteller/domain/enums/ExportMissing.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/web/StoryExportController.java`

```java
ExportResult result = storyExport.exportForNubiGame(storyId);

if (result.isDelivered()) {
    byte[] zip = result.zip();
} else {
    for (ExportFault fault : result.faults()) {
        // fault.page() 2, fault.missing() IMAGE, fault.message() "imagen"
        // fault.page() 4, fault.missing() AUDIO, fault.message() "audio"
    }
}
```

```
GET /api/v1/storyteller/{storyId}/export/nubi-game
200 -> zip (application/octet-stream, attachment "story-{storyId}.zip")
409 -> { success: false, message: "El cuento no se puede exportar",
         data: { faults: [ { page: 2, missing: "IMAGE", message: "imagen" },
                           { page: 4, missing: "AUDIO", message: "audio" } ] } }
404 -> el cuento no existe
```

`missing`: `COVER_NOT_ACCEPTED`, `ACCEPTED_TEXT`, `IMAGE`, `AUDIO`; `page` 0 es la portada.

## StoryExportService: exportación para el agente visual con evaluación completa de condiciones

`StoryExport.exportForVisualAgent` (implementado por `StoryExportService`, en el mismo servicio y controlador que Nubi Game) no exige imágenes ni audio de las páginas. Comprueba en una sola pasada el guion aprobado (`SCRIPT_ACCEPTED` o `PAGES_ACCEPTED`; si no, `SCRIPT_NOT_APPROVED`), la portada aceptada (`COVER_NOT_ACCEPTED`) con lugar y tono propios en el guion (`COVER_SETTING_TONE`), cada página interior aceptada (`ACCEPTED_TEXT`), el lugar de escena de cada beat (`BEAT_SETTING`; en un cuento anterior a la feature faltan los lugares y la portada, y se indica cada condición) y la instantánea del protagonista (`PROTAGONIST_MISSING`). Acumula todas las faltas. Solo si no hay ninguna compone el `VisualAgentStory` (descripción de cada escena = escena aceptada de la página; lugar y tono de cada página = los de su beat) y se lo pasa a `VisualAgentExporter`: si éste devuelve errores, el rechazo trae todas las reglas del contrato incumplidas (`CONTRACT_RULE`, con su código estable en `code`) o el fallo del protagonista (`PROTAGONIST_IMAGE`: sin retrato o no se pudo quitar el fondo; la instantánea no cambia). Disponibilidad y contrato comparten la misma respuesta de rechazo (`ExportResult`/`ExportFault`); `page` es 0 para la portada, N para una página y -1 (`ExportFault.STORY`) para faltas del cuento entero. Lanza `ResourceNotFoundException` si el cuento no existe. No persiste nada.

Rutas: `api/src/main/java/es/vargontoc/storyteller/application/service/StoryExportService.java`, `api/src/main/java/es/vargontoc/storyteller/application/ports/in/export/StoryExport.java`, `api/src/main/java/es/vargontoc/storyteller/domain/response/ExportFault.java`, `api/src/main/java/es/vargontoc/storyteller/domain/enums/ExportMissing.java` y `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/web/StoryExportController.java`

```java
ExportResult result = storyExport.exportForVisualAgent(storyId);

if (result.isDelivered()) {
    byte[] zip = result.zip();   // story.json y characters/<id>/character.png
} else {
    for (ExportFault fault : result.faults()) {
        // fault.page() 3,  fault.missing() ACCEPTED_TEXT,   fault.message() "texto aceptado"
        // fault.page() 2,  fault.missing() BEAT_SETTING,    fault.message() "lugar de escena"
        // fault.page() -1, fault.missing() CONTRACT_RULE,   fault.code() "CHARACTER_IMAGE_TOO_SMALL"
    }
}
```

```
GET /api/v1/storyteller/{storyId}/export/visual-agent
200 -> zip (application/octet-stream, attachment "story-{storyId}-visual-agent.zip")
409 -> { success: false, message: "El cuento no se puede exportar",
         data: { faults: [ { page: 3,  missing: "ACCEPTED_TEXT", message: "texto aceptado", code: null },
                           { page: -1, missing: "CONTRACT_RULE", message: "... ($.characters[0].image)",
                             code: "CHARACTER_IMAGE_TOO_SMALL" } ] } }
404 -> el cuento no existe
```

`missing`: `SCRIPT_NOT_APPROVED`, `COVER_NOT_ACCEPTED`, `COVER_SETTING_TONE`, `ACCEPTED_TEXT`, `BEAT_SETTING`, `PROTAGONIST_MISSING`, `PROTAGONIST_IMAGE`, `CONTRACT_RULE`. `code` solo viene en `CONTRACT_RULE` y `PROTAGONIST_IMAGE`; en el resto es `null` (y la exportación de Nubi Game también lo incluye a `null`).
