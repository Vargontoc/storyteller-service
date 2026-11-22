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
