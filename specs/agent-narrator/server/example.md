# Ejemplos: agent-narrator - server

## VoiceLanguageCatalog (VoiceLanguage)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLanguageCatalog.java` (modelo en `domain/model/VoiceLanguage.java`)
**Para qué sirve:** Catálogo fijo de idiomas que se pueden asignar a una voz, limitado a los que admite Chatterbox Multilingual V3. El español (`es`) es el valor por defecto. `isSupported` es el predicado de admisibilidad: un código ausente, vacío o que no está en el catálogo no es admisible (no distingue mayúsculas). Es un bean sin estado ni dependencias.

```java
VoiceLanguageCatalog catalog = new VoiceLanguageCatalog();

VoiceLanguage byDefault = catalog.defaultLanguage();   // ("es", "Español")
List<VoiceLanguage> options = catalog.all();           // lista para mostrar en el formulario de subida

catalog.isSupported("en");   // true
catalog.isSupported("xx");   // false
catalog.isSupported("");     // false
catalog.isSupported(null);   // false

if (!catalog.isSupported(requestedLanguage)) {
    throw new AppException("Idioma obligatorio o no admitido", HttpStatus.BAD_REQUEST);
}
```

## VoiceAudioInspector

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/VoiceAudioInspector.java`
**Para qué sirve:** Comprueba el contenido real de un audio de voz subido, sin fiarse de la extensión ni del tipo MIME: debe ser WAV o MP3 y durar entre 5 y 30 segundos, ambos incluidos. Devuelve un `Inspection` con `rejection` nulo si es válido, o la causa diferenciada `INVALID_FORMAT` / `INVALID_DURATION`, y la duración en milisegundos. Usa JAVE (ffmpeg), ya presente en el proyecto. Es un bean sin estado.

```java
VoiceAudioInspector inspector = new VoiceAudioInspector();

byte[] uploaded = multipartFile.getBytes();
Inspection result = inspector.inspect(uploaded);

if (result.rejection() == Rejection.INVALID_FORMAT) {
    throw new AppException("Formato no admitido: solo WAV o MP3", HttpStatus.BAD_REQUEST);
}
if (result.rejection() == Rejection.INVALID_DURATION) {
    throw new AppException("La duración debe estar entre 5 y 30 segundos", HttpStatus.BAD_REQUEST);
}
// result.isValid() == true: se puede guardar la voz
```

## VoiceRepository (Voice)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/VoiceRepository.java` (adaptador `infrastructure/adapters/out/persistence/VoiceRepositoryAdapter.java`, modelo `domain/model/Voice.java`, migración `026_create_voice.xml`)
**Para qué sirve:** Puerto de persistencia de las voces subidas. Guarda el nombre (único), el idioma y la declaración de permiso; permite listar, buscar por nombre y borrar. No enlaza narraciones: borrar una voz no toca ningún cuento. Un segundo `save` con un nombre ya usado lanza `DataIntegrityViolationException` y la voz existente no cambia. Se inyecta como cualquier puerto de salida.

```java
Voice saved = voiceRepository.save(new Voice("Abuela", "es", true));   // devuelve la voz con su id

List<Voice> all = voiceRepository.findAll();                           // de la más antigua a la más reciente

Optional<Voice> found = voiceRepository.findByName("Abuela");          // vacío si no existe

try {
    voiceRepository.save(new Voice("Abuela", "en", true));
} catch (DataIntegrityViolationException e) {
    // nombre duplicado: la primera "Abuela" sigue igual
}

voiceRepository.delete(saved.getId());
```

## VoiceSampleStore

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/VoiceSampleStore.java` (adaptador `infrastructure/storage/VoiceSampleStoreAdapter.java`, sobre `ResourceStorage`)
**Para qué sirve:** Puerto que guarda, recupera y elimina el audio de referencia de una voz subida, identificado por el id de la voz (`Voice.getId()`). Los bytes se recuperan idénticos a los guardados (sin conversión). Si la muestra no existe, porque nunca se guardó o ya se eliminó, `find` devuelve `Optional.empty()`. Los ficheros viven en `stories/voices/<id>/sample`.

```java
byte[] sample = multipartFile.getBytes();
Voice voice = voiceRepository.save(new Voice("Abuela", "es", true));

voiceSampleStore.save(voice.getId(), sample);

byte[] reference = voiceSampleStore.find(voice.getId())
        .orElseThrow(() -> new AppException("Voz no disponible", HttpStatus.NOT_FOUND));

voiceSampleStore.delete(voice.getId());
voiceSampleStore.find(voice.getId());   // Optional.empty()
```

## NarrationRepository (StoryNarration)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/NarrationRepository.java` (adaptador `infrastructure/adapters/out/persistence/NarrationRepositoryAdapter.java`, modelo `domain/model/StoryNarration.java`, migración `027_create_story_narration.xml`)
**Para qué sirve:** Puerto de persistencia de la narración de un cuento: estado (`IN_PROGRESS`, `COMPLETED`, `ERROR`), copia del nombre de la voz (sin clave a la voz subida, así que sobrevive a su borrado) y audio de cada página interior. `publish` registra de una vez los audios (clave: nº de página, mínimo 1) y la voz: la portada (página 0) se rechaza con `IllegalArgumentException` y, si algo falla, no queda audio ni voz. `markInProgress` y `markError` dejan el cuento sin audio ni voz; `delete` elimina la narración entera. Los audios se guardan como ficheros (`stories/<storyId>/narration/<página>.wav`), como el audio de las páginas antiguas, y se borran tras confirmarse la transacción.

```java
narrationRepository.markInProgress(storyId);             // IN_PROGRESS, sin audio ni voz

Map<Integer, byte[]> audios = new HashMap<>();           // páginas 1..8, nunca la 0
for (int page = 1; page <= 8; page++) audios.put(page, wavBytes(page));
narrationRepository.publish(storyId, "Abuela", audios); // COMPLETED + voz + 8 audios, todo o nada

StoryNarration narration = narrationRepository.find(storyId).orElseThrow();
narration.getVoiceName();    // "Abuela" (copia del nombre)
narration.getAudioPages();   // [1, 2, ..., 8]

byte[] page2 = narrationRepository.findAudio(storyId, 2).orElseThrow();
narrationRepository.findAudio(storyId, 0);               // Optional.empty(): la portada no tiene audio

narrationRepository.markError(storyId);                  // ERROR, sin audio ni voz
narrationRepository.delete(storyId);                     // borra audios, voz y estado
```

## NarrationNotifier (NarrationEvent)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/NarrationNotifier.java` (evento `domain/events/NarrationEvent.java`, tipos en `domain/enums/NarrationEventType.java`, puerto `application/ports/out/NarrationEventPublisher.java`, adaptador `infrastructure/adapters/out/WebsocketNarrationEventPublisher.java`)
**Para qué sirve:** Publica los eventos de narración de un cuento: `STARTED`, `COMPLETED` (con el nombre de la voz usada) y `FAILED`. Todos llevan el `storyId` (obligatorio; si es nulo lanza `NullPointerException`). `voiceName` solo se informa en `COMPLETED` y se omite del JSON en los demás. Sigue el patrón de `StoryPageNotifier`: el notificador construye el evento y lo entrega al puerto `NarrationEventPublisher`, que el adaptador difunde por `WebsocketUseCase.sendNarrationNotification` a `/storyteller`.

```java
// inyectado como bean: NarrationNotifier notifier

notifier.started(storyId);                 // {"type":"STARTED","storyId":7}

// ... narración y publicación atómica terminadas
notifier.completed(storyId, "Abuela");     // {"type":"COMPLETED","storyId":7,"voiceName":"Abuela"}

// ... ante cualquier fallo, tras limpiar el audio parcial
notifier.failed(storyId);                  // {"type":"FAILED","storyId":7}
```

## SystemVoiceProvider

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/SystemVoiceProvider.java` (propiedades tipadas en `infrastructure/config/SystemVoiceProperties.java`, prefijo `app.system-voice`)
**Para qué sirve:** Expone la voz del sistema de la instalación: nombre, si está disponible y, si no, el motivo. No hay valor por defecto en el código: nombre y audio de referencia vienen solo de la configuración. Al arrancar valida el audio con `VoiceAudioInspector` (WAV/MP3, 5 a 30 s) y comprueba si el nombre coincide con una voz subida (`collidesWithUploadedVoice`, para el aviso de la biblioteca). Nunca impide el arranque: sin configurar, con audio inexistente o inválido, queda no disponible. Un cambio de configuración se aplica al reiniciar.

Configuración (en `application.yaml` las claves están vacías; se informan por entorno o en la configuración de la instalación):

```yaml
app:
  system-voice:
    name: Narradora            # APP_SYSTEM_VOICE_NAME
    sample-file: /data/voices/narradora.wav   # APP_SYSTEM_VOICE_SAMPLE_FILE (WAV o MP3, 5 a 30 s)
```

```java
// inyectado como bean: SystemVoiceProvider systemVoice

if (systemVoice.isAvailable()) {
    String name = systemVoice.name();                       // "Narradora"
    byte[] reference = systemVoice.sample().orElseThrow();   // audio de referencia para la síntesis
} else {
    String why = systemVoice.unavailableReason();            // p. ej. "No hay nombre de voz del sistema configurado"
}

if (systemVoice.collidesWithUploadedVoice()) {
    // mostrar el aviso en la biblioteca: el nombre coincide con una voz subida
}
```

## VoiceLibraryService (VoiceLibraryUseCase)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLibraryService.java` (puerto `application/ports/in/generator/VoiceLibraryUseCase.java`, comando `domain/request/UploadVoiceCommand.java`, respuestas `domain/response/VoiceLibrary.java` y `VoiceEntry.java`, causas `domain/enums/VoiceRejectionReason.java`, error `shared/exceptions/VoiceRejectedException.java`)
**Para qué sirve:** Biblioteca de voces. `list` devuelve la voz del sistema (primera y marcada `system`, solo si está disponible; si no, `systemVoiceAvailable=false` con su `systemVoiceUnavailableReason`), el aviso `systemVoiceNameCollision` y las voces subidas con nombre e idioma. `upload` valida TODO antes de persistir, en este orden: nombre obligatorio, idioma del catálogo, permiso declarado, nombre no repetido (frente a subidas y a la voz del sistema) y audio WAV/MP3 de 5 a 30 s. Un rechazo lanza `VoiceRejectedException` con un `VoiceRejectionReason` estable (`NAME_REQUIRED`, `LANGUAGE_REQUIRED`, `PERMISSION_REQUIRED`, `NAME_DUPLICATED` con 409, `INVALID_FORMAT`, `INVALID_DURATION`) y no deja registro ni muestra; si falla guardar la muestra tras el registro, se borra el registro. El nombre se recorta y la unicidad distingue mayúsculas, como la base de datos.

```java
// inyectado como bean: VoiceLibraryUseCase library

VoiceLibrary all = library.list();
if (!all.systemVoiceAvailable()) {
    String why = all.systemVoiceUnavailableReason();        // aviso en la biblioteca
}
if (all.systemVoiceNameCollision()) {
    // el nombre de la voz del sistema coincide con el de una voz subida
}
all.voices().forEach(v -> show(v.name(), v.language(), v.system()));

try {
    VoiceEntry created = library.upload(
            new UploadVoiceCommand("Abuela", "es", true, multipartFile.getBytes()));
} catch (VoiceRejectedException e) {
    VoiceRejectionReason reason = e.getReason();            // el controller lo traduce a su respuesta
}
```

### Borrado y voces elegibles para narrar (TASK-011)

**Para qué sirve:** `delete(id)` borra una voz subida (registro y muestra de referencia) sin tocar ninguna narración guardada (esta guarda solo una copia del nombre). `id == null` identifica la voz del sistema (su `VoiceEntry.id` es null): se rechaza con `SYSTEM_VOICE_NOT_DELETABLE` (409) y sigue listada; un id inexistente se rechaza con `VOICE_NOT_FOUND` (404). `eligible()` devuelve la voz del sistema solo si está disponible y las voces subidas en español. `eligibleSample(id)` devuelve el audio de referencia de una voz elegible (subida, o del sistema con `id == null`); vacío si no es elegible (borrada, en otro idioma, sistema no disponible). Para añadir `findById` al puerto `VoiceRepository` se amplió también su adaptador.

```java
try {
    library.delete(voiceEntry.id());                 // subida: desaparece del listado y de las elegibles
} catch (VoiceRejectedException e) {
    // e.getReason(): SYSTEM_VOICE_NOT_DELETABLE o VOICE_NOT_FOUND
}

List<VoiceEntry> options = library.eligible();       // [sistema (si disponible), subidas "es"]

VoiceEntry chosen = options.get(0);
byte[] reference = library.eligibleSample(chosen.id())   // id null = voz del sistema
        .orElseThrow(() -> new AppException("Voz no disponible", HttpStatus.CONFLICT));
```

## NarratorModelProvisioner

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/NarratorModelProvisioner.java` (Modelfile `api/src/main/resources/prompts/narrator.modelfile`)
**Para qué sirve:** Registra en Ollama al arrancar, solo si falta, el modelo del narrador de audio (`storyteller-audio-narrator`, base `qwen2.5:14b-instruct`). Es independiente del modelo antiguo `storyteller-narrator` (`app.agents.narrator`). El Modelfile ordena dividir el texto aceptado de cada página en pocos fragmentos de frases completas y anotar la expresividad de cada uno, sin añadir, quitar, reordenar ni sustituir palabras y sin atender al número de palabras. Es un `ApplicationRunner`: no se invoca a mano. Se desactiva con `app.agents.audio-narrator-provisioning.enabled=false`.

Configuración:

```yaml
app:
  agents:
    audio-narrator: storyteller-audio-narrator   # APP_AGENT_AUDIO_NARRATOR
    audio-narrator-provisioning:
      enabled: true                              # APP_AGENT_AUDIO_NARRATOR_PROVISIONING_ENABLED
```

Formato de salida JSON que devuelve el modelo (lo deserializa el agente narrador, TASK-014). Una entrada por página interior recibida, en orden, desde la página 1:

```json
{
  "pages": [
    {
      "page": 1,
      "fragments": [
        { "text": "El perro dijo: ¡Guau!", "exaggeration": 0.9, "cfgWeight": 0.5, "temperature": 0.8 },
        { "text": "Y corrió hasta casa.", "exaggeration": 0.6, "cfgWeight": 0.4, "temperature": 0.8 }
      ]
    }
  ]
}
```

Rangos pedidos al modelo: `exaggeration` 0.25 a 2.0, `cfgWeight` 0.0 a 1.0, `temperature` 0.05 a 5.0 (recomendado hasta 1.2). Los `text` de cada página, unidos, deben conservar las palabras del texto aceptado; lo comprueba `AudioScriptValidator`.

## StoryNarrationService (NarrationState)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java` (puerto `application/ports/in/generator/StoryNarrationUseCase.java`, respuesta `domain/response/NarrationState.java`, motivos en `domain/enums/NarrationDisabledReason.java`)
**Para qué sirve:** Estado de narración de un cuento y las consultas internas que usa `StoryPageService`. `getState` se calcula siempre desde el estado actual de las páginas interiores (1..nº de beats de la escaleta; una portada pendiente no bloquea). `canNarrate` es falso con `disabledReason`: `NO_OUTLINE` (cuento antiguo), `IN_PROGRESS` (se está narrando) o `PAGES_NOT_ACCEPTED` (falta alguna interior por aceptar o generar); si no, `disabledReason` es nulo. Con audio, `voiceName` es la voz usada; `lastResult` es el resultado del último intento (`IN_PROGRESS`, `COMPLETED`, `ERROR`) o nulo si nunca se narró. `deleteAudio` borra a la vez audio y voz (y el estado de narración). Las TASK-020/021/022 ampliarán este servicio (iniciar, ejecutar, descargar).

```java
// inyectado como bean: StoryNarrationUseCase narration

NarrationState state = narration.getState(storyId);
if (!state.hasOutline()) {
    // cuento antiguo: no mostrar el panel de narración
} else if (!state.canNarrate()) {
    NarrationDisabledReason why = state.disabledReason();   // PAGES_NOT_ACCEPTED, IN_PROGRESS...
} else if (state.hasAudio()) {
    String voice = state.voiceName();                       // "narrado con Abuela" (renarrar permitido)
}

// Desde StoryPageService, antes de mutar páginas interiores:
if (narration.isNarrating(storyId)) {
    throw new AppException("Se está narrando el cuento", HttpStatus.CONFLICT);
}
boolean willLoseAudio = narration.hasAudio(storyId);        // para el aviso de borrado

// ... y, tras mutar con éxito y en la misma transacción:
if (willLoseAudio) {
    narration.deleteAudio(storyId);                         // sin audio ni voz; narrar sigue la regla de páginas
}
```

### Inicio de la narración y la renarración (TASK-020)

**Rutas:** `StoryNarrationService.start` (puerto `StoryNarrationUseCase`), punto de extensión `application/ports/out/NarrationExecutor.java` (implementado por `BackgroundNarrationExecutor`, TASK-021), error `shared/exceptions/NarrationRejectedException.java`, motivos `domain/enums/NarrationRejectionReason.java`; `GenerationGatePort.GenerationType` gana `NARRATION`.
**Para qué sirve:** `start(storyId, voiceId)` inicia narrar o renarrar (es la misma operación); `voiceId == null` es la voz del sistema. Primero valida, sin pedir el turno: escaleta (`NO_OUTLINE`), páginas interiores aceptadas (`PAGES_NOT_ACCEPTED`), no narrándose (`IN_PROGRESS`), que haya voces elegibles (`NO_VOICES_AVAILABLE`) y que la elegida lo sea (`VOICE_NOT_AVAILABLE`, borrada o no española). Luego pide el turno a `GenerationGate` (ocupado: `GENERATION_IN_PROGRESS`, "Hay una generación en curso"). Todos son `NarrationRejectedException` con motivo estable y HTTP 409, y ninguno toca el audio previo. Con el turno: toma la muestra, borra audio y voz anteriores (`markInProgress`), notifica `STARTED`, entrega el trabajo a `NarrationExecutor` y devuelve el `NarrationState` (ya `narrating`). Si algo falla tras conseguir el turno, lo libera. `NarrationExecutor` es el punto de extensión que implementa `BackgroundNarrationExecutor` (TASK-021): ejecuta en segundo plano y libera el turno al terminar, con éxito o con error.

```java
// inyectado como bean: StoryNarrationUseCase narration

try {
    NarrationState state = narration.start(storyId, null);       // voz del sistema; con id de voz subida, esa voz
    // state.narrating() == true; el turno de generación queda retenido hasta que el ejecutor lo libere
} catch (NarrationRejectedException e) {
    NarrationRejectionReason why = e.getReason();                // el controller lo traduce; e.getStatus() == 409
}
```

### Descarga del audio de una página interior (TASK-022)

**Rutas:** `StoryNarrationService.getPageAudio` (puerto `StoryNarrationUseCase`).
**Para qué sirve:** `getPageAudio(storyId, page)` devuelve los bytes del audio narrado publicado de una página interior (`page >= 1`). La portada (0), una página sin audio, un cuento sin audio y una narración en curso o con error (no hay audio publicado; una renarración descarta el previo al empezar) responden `AppException` con HTTP 404 y mensaje `AUDIO_NOT_FOUND`. El audio no depende de la voz subida: sigue disponible aunque se borre. La respuesta binaria REST es de TASK-028.

```java
// inyectado como bean: StoryNarrationUseCase narration

try {
    byte[] audio = narration.getPageAudio(storyId, 2);   // solo el audio de la página 2
} catch (AppException e) {
    // e.getStatus() == 404: portada, sin audio o narración no publicada
}
```

### Ejecución de la narración en segundo plano (TASK-021)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/BackgroundNarrationExecutor.java` (implementa `application/ports/out/NarrationExecutor.java`; sustituye y borra `PendingNarrationExecutor`)
**Para qué sirve:** Es la implementación real de `NarrationExecutor`. `StoryNarrationService.start` la invoca con el turno concedido y el cuento ya en curso; no se llama a mano. `execute` lanza el trabajo en el `TaskExecutor` del proyecto, tras el commit de la transacción de `start` (si esa transacción se deshace, libera el turno). El trabajo: lee los textos aceptados de las páginas interiores 1..beats (sin portada), pide el guion al narrador, lo valida con `AudioScriptValidator`, llama una vez a `OllamaPort.stopAllServices()`, sintetiza cada fragmento en español con la muestra recibida y la expresividad del guion, une los clips de cada página con `AudioAssembler.concat` (en memoria) y, solo con todas las páginas listas, publica audios y nombre de la voz con `NarrationRepository.publish` y notifica `COMPLETED`. El guion nunca se persiste. Ante cualquier fallo (narrador, guion alterado, síntesis en cualquier página, publicación) llama a `markError` (sin audio ni voz parciales), notifica `FAILED` y, siempre, libera el turno. No hay reintento. La muestra y el nombre llegan por parámetro, así que borrar la voz durante la narración no la afecta.

```java
// Lo invoca StoryNarrationService.start al conceder el turno; equivale a:
narrationExecutor.execute(storyId, "Abuela", sampleBytes);   // vuelve de inmediato

// En segundo plano, con 8 páginas interiores:
//   narrator.narrate(textos) -> validator -> ollama.stopAllServices() -> synthesizer.synthesize(...) por fragmento
//   -> narrations.publish(storyId, "Abuela", {1..8 -> wav}) -> notifier.completed(storyId, "Abuela") -> gate.release()
// Si la síntesis falla en la página 4: narrations.markError(storyId), notifier.failed(storyId), gate.release();
// el cuento queda sin audio ni voz y se puede volver a narrar.
```

## NarrationReconciliationService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/NarrationReconciliationService.java` (disparo en `infrastructure/config/NarrationReconciliationRunner.java`; consulta `NarrationRepository.findInProgress()`)
**Para qué sirve:** Al arrancar cierra como fallidas las narraciones que quedaron en curso por un reinicio: las pasa a `ERROR` con `markError` (sin audio parcial ni voz), notifica `FAILED` por cada una con `NarrationNotifier.failed` y no toca las terminadas. Un fallo en un cuento se registra y no impide reconciliar los demás. La puerta `GenerationGate` no se persiste: es memoria y arranca libre, así que no hay nada que liberar. Sigue el patrón de la reconciliación de retratos: un `ApplicationRunner` (`NarrationReconciliationRunner`) la invoca; se desactiva con `app.narration.reconciliation.enabled=false`.

```java
// Lo invoca el runner al arrancar; no se llama a mano. Equivale a:
narrationReconciliationService.reconcile();

// Antes (arranque tras reinicio):  cuento 7 -> IN_PROGRESS, cuento 8 -> COMPLETED con voz "Abuela"
// Después:                         cuento 7 -> ERROR, sin audio ni voz, evento {"type":"FAILED","storyId":7}
//                                  cuento 8 -> sin cambios, sin evento
// Un cuento en ERROR vuelve a poder narrarse y generationGate.tryAcquire(...) concede el turno.
```

```yaml
app:
  narration:
    reconciliation:
      enabled: true   # por defecto activo; false lo desactiva (pruebas de contexto)
```

## AudioScriptValidator

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/domain/service/AudioScriptValidator.java`
**Para qué sirve:** Comprueba de forma determinista, página a página, que los fragmentos del guion de audio unidos reproducen exactamente la secuencia de palabras del texto aceptado. Solo normaliza mayúsculas y puntuación (los acentos cuentan): una palabra añadida, quitada, reordenada o sustituida, o una onomatopeya omitida, invalida la página. Devuelve un `Result` con `valid` y, si falla, `failedPage` con el número (desde 1, solo páginas interiores) de la primera página que no conserva las palabras; un número distinto de páginas falla en la primera que falta. Recibe cadenas, no depende del modelo del guion. Clase sin estado ni dependencias.

```java
AudioScriptValidator validator = new AudioScriptValidator();

List<String> accepted = List.of("El perro dijo: ¡Guau! Y corrió.", "Llegó a casa.");   // texto aceptado, páginas 1..n
List<List<String>> script = List.of(
        List.of("El perro dijo: ¡Guau!", "Y corrió."),                                   // fragmentos de la página 1
        List.of("llegó a CASA"));                                                         // otra puntuación/mayúsculas: válido

AudioScriptValidator.Result result = validator.validate(accepted, script);
if (!result.valid()) {
    throw new AppException("El guion de audio altera el texto de la página " + result.failedPage(),
            HttpStatus.INTERNAL_SERVER_ERROR);
}
```

## NarratorAgent (SpringAiNarratorAgent)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiNarratorAgent.java` (puerto `application/ports/out/external/NarratorAgent.java`, modelos `domain/model/PageAudioScript.java` y `domain/model/AudioFragment.java`)
**Para qué sirve:** Envía a Ollama el texto aceptado de las páginas interiores, en orden (la primera es la página 1; la portada nunca se incluye), y devuelve el guion de audio completo: un `PageAudioScript` por página con sus `AudioFragment` (texto, `exaggeration`, `cfgWeight`, `temperature`). Usa el modelo `app.agents.audio-narrator` con su propio `ChatClient` (bean `audio-narrator-agent`), independiente del narrador antiguo, y no contacta con Ollama al arrancar. Lanza `AppException` (502) si el modelo no está disponible, falla o responde algo inutilizable: faltan páginas, número de página incorrecto, página sin fragmentos, texto vacío o falta algún parámetro. No hay reintento. No valida que las palabras se conserven (lo hace `AudioScriptValidator`) ni los rangos de los parámetros.

```java
// inyectado como bean: NarratorAgent narrator

List<String> accepted = List.of("El perro dijo: ¡Guau! Y corrió.", "Llegó a casa.");   // páginas interiores 1..n

try {
    List<PageAudioScript> script = narrator.narrate(accepted);       // una entrada por página, en orden
    PageAudioScript page1 = script.get(0);                           // page() == 1
    for (AudioFragment f : page1.fragments()) {
        synthesize(f.text(), f.exaggeration(), f.cfgWeight(), f.temperature());
    }
} catch (AppException e) {
    // el narrador falló o devolvió un guion incompleto: la narración termina con error (ALT-006)
}
```

## SpeechSynthesizer (ChatterboxSpeechSynthesisAdapter)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/ChatterboxSpeechSynthesisAdapter.java` (puerto `application/ports/out/external/SpeechSynthesizer.java`, petición `domain/request/SpeechSynthesisRequest.java`)
**Para qué sirve:** Sintetiza un fragmento en Chatterbox Multilingual V3 y devuelve el audio WAV. Cada petición lleva el texto, el idioma (`es`), el audio de referencia de la voz (bytes) y la expresividad; se envía como multipart a `POST /audio/speech/upload` del contenedor (campos `input`, `language_id`, `voice_file`, `exaggeration`, `cfg_weight`, `temperature`, `response_format=wav`). Antes de enviar recorta la expresividad al rango del narrador: `exaggeration` 0.25 a 2.0, `cfgWeight` 0.0 a 1.0, `temperature` 0.05 a 5.0. Un error del motor o una respuesta vacía lanzan `AppException` 502, y un tiempo agotado o contenedor caído, 504 (`app.chatterbox.timeout-seconds`). No hay reintento. Es independiente de `AudioGeneratorAdapter` (cuentos antiguos) y no libera la VRAM de Ollama: quien orquesta (TASK-021) debe llamar una vez a `OllamaPort.stopAllServices()` entre el narrador y la primera síntesis. `SpeechSynthesisRequest` rechaza con `IllegalArgumentException` texto vacío, idioma vacío o muestra vacía.

```java
// inyectado como bean: SpeechSynthesizer synthesizer

byte[] reference = library.eligibleSample(chosen.id()).orElseThrow();   // audio de referencia de la voz elegida

try {
    byte[] wav = synthesizer.synthesize(new SpeechSynthesisRequest(
            fragment.text(), "es", reference,
            fragment.exaggeration(), fragment.cfgWeight(), fragment.temperature()));
    pageClips.add(wav);                                                  // se unen en un audio por página
} catch (AppException e) {
    // motor caído, error, timeout o audio vacío: la narración termina con error (ALT-006)
}
```

Prueba manual (contenedor de Chatterbox en marcha): `mvn -f api/pom.xml test -Dtest=ChatterboxSpeechSynthesisManualHarness -Dspeech.harness=true -Dspeech.sample=<ruta a un WAV/MP3 de 5 a 30 s> -Dspeech.out=<salida.wav>`.
