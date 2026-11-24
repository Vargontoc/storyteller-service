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
