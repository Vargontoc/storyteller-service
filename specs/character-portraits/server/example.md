# Ejemplos: character-portraits - server

## CatalogCharacterPortraitJpaRepository

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/persistence/CatalogCharacterPortraitJpaRepository.java`
**Para qué sirve:** Persiste y recupera el único retrato aprobado asociado a un personaje del catálogo.

```java
var portrait = new CatalogCharacterPortraitJpaEntity();
portrait.setCatalogCharacter(character);
portrait.setAssetPath("portraits/luna-approved.png");
portraits.save(portrait);

var approved = portraits.findByCatalogCharacterId(character.getId());
```

## PortraitGenerationJobJpaRepository

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/persistence/PortraitGenerationJobJpaRepository.java`
**Para qué sirve:** Guarda el estado persistente de un trabajo de retratos para que las capas posteriores deriven el bloqueo.

```java
var job = new PortraitGenerationJobJpaEntity();
job.setCatalogCharacter(character);
job.setStatus(PortraitGenerationStatus.RUNNING);
jobs.save(job);
```

## PortraitCandidateJpaRepository

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/persistence/PortraitCandidateJpaRepository.java`
**Para qué sirve:** Almacena los candidatos de un personaje y permite eliminarlos al reemplazarlos o invalidarlos.

```java
var candidate = new PortraitCandidateJpaEntity();
candidate.setCatalogCharacter(character);
candidate.setGenerationJob(job);
candidate.setAssetPath("portraits/luna-1.png");
candidates.save(candidate);

candidates.deleteByCatalogCharacterId(character.getId());
```

## CatalogCharacterForkJpaRepository

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/persistence/CatalogCharacterForkJpaRepository.java`
**Para qué sirve:** Registra qué personaje derivado procede de un original, sin modificar la ficha original.

```java
var relation = new CatalogCharacterForkJpaEntity();
relation.setOriginalCatalogCharacter(original);
relation.setForkCatalogCharacter(fork);
forks.save(relation);
```

## PortraitGeneratorPort

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/PortraitGeneratorPort.java`
**Para qué sirve:** Solicita un lote de cuatro bytes de retrato a un generador externo desde la capa de aplicación.

```java
var prompt = new CharacterPortraitPrompt(
    "zorra naranja con cola esponjosa",
    "small orange fox with a fluffy white-tipped tail",
    List.of("small orange fox", "fluffy white-tipped tail")
);
List<byte[]> portraits = portraitGenerator.generatePortraits(prompt);
```

**Notas:** El puerto devuelve exactamente cuatro imágenes o propaga el fallo del proveedor. El prompt combina la descripción inglesa con los atributos visuales y la plantilla de cuerpo completo.

## PortraitComfyUiAdapter

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/PortraitComfyUiAdapter.java`
**Para qué sirve:** Implementa el puerto con un único workflow de ComfyUI que genera cuatro variantes cartoon de la misma descripción visual.

```java
var prompt = new CharacterPortraitPrompt(
    "zorra naranja con cola esponjosa",
    "small orange fox with a fluffy white-tipped tail",
    List.of("small orange fox", "fluffy white-tipped tail")
);
List<byte[]> portraits = adapter.generatePortraits(prompt);
```

**Notas:** Espera el historial de ComfyUI sin aplicar un límite temporal de generación, exige un lote de cuatro imágenes y, si `character-transparent-background` está activo, rechaza cualquier PNG sin canal alfa. Requiere el nodo `Image Remove Background (rembg)` en ComfyUI.

## PortraitWebsocketNotifier

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitWebsocketNotifier.java`
**Para qué sirve:** Publica el ciclo de generación de retratos en el canal público `/storyteller` mediante el difusor WebSocket existente.

```java
notifier.started(catalogCharacterId);
notifier.progress(catalogCharacterId);
notifier.success(catalogCharacterId);
notifier.error(catalogCharacterId, "ComfyUI no está disponible. Inténtalo de nuevo.");
```

**Notas:** Cada evento contiene el identificador del catálogo, el tipo de evento, el estado de generación y un motivo comunicable.

## PortraitGenerationJobStore

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/PortraitGenerationJobStore.java`
**Para qué sirve:** Persiste el ciclo de vida de un trabajo de retratos y permite derivar más adelante el bloqueo desde los trabajos `RUNNING`.

```java
var job = portraitGenerationJobStore.start(catalogCharacterId);

portraitGenerationJobStore.complete(job.id());
var runningJobs = portraitGenerationJobStore.findRunning();
```

**Notas:** Solo puede existir un trabajo `RUNNING` por personaje. Las consultas no cambian el estado ni aplican caducidad; un fallo se registra con `fail(jobId, publicReason)`.

## PortraitCandidateStore

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/PortraitCandidateStore.java`
**Para qué sirve:** Mantiene el lote temporal de cuatro retratos de un personaje hasta que una tarea posterior permita seleccionar uno.

```java
var candidates = portraitCandidateStore.replace(
    catalogCharacterId,
    generationJobId,
    List.of("characters/42/one.png", "characters/42/two.png",
        "characters/42/three.png", "characters/42/four.png")
);

portraitCandidateStore.delete(catalogCharacterId, candidates.getFirst().id());
```

**Notas:** El lote debe contener exactamente cuatro rutas y el trabajo debe pertenecer al personaje. Al reemplazar o descartar, se eliminan solo los assets de candidatos; el retrato aprobado no se toca.

## PortraitConditionAdapter

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/PortraitConditionAdapter.java`
**Para qué sirve:** Responde si un personaje tiene retrato aprobado o imágenes generadas, para que el catálogo y las revisiones calculen su estado visual.

```java
boolean approved = conditions.hasApprovedPortrait(catalogCharacterId);
boolean hasImages = conditions.hasGeneratedImages(catalogCharacterId);
```

**Notas:** Son consultas de solo lectura. Una imagen generada puede ser un candidato pendiente o el retrato ya aprobado.

## PortraitForkService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitForkService.java`
**Para qué sirve:** Crea un nuevo personaje de catálogo a partir de uno ya usado, preservando por completo el original y sus cuentos.

```java
CatalogCharacter fork = portraitForkService.create(
    originalCatalogCharacterId,
    "zorra plateada con cola esponjosa",
    new ActorMetadata("silver fox", List.of("silver", "fluffy tail"))
);

portraitForkService.rename(fork.id(), "Luna plateada");
```

**Notas:** Solo se permite si una instantánea referencia explícitamente el catálogo original. El fork comienza sin retrato ni candidatos; las filas históricas con FK nula no se asocian por nombre.

## PortraitLockService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitLockService.java`
**Para qué sirve:** Determina si un personaje está bloqueado a partir de un trabajo persistido en estado `RUNNING`.

```java
if (portraitLockService.isLocked(catalogCharacterId)) {
    // La ficha sigue consultable, pero no se permiten modificaciones.
}

portraitLockService.requireUnlocked(catalogCharacterId);
portraitLockService.assertCanUseAsStoryProtagonist(catalogCharacterId);
```

**Notas:** No almacena un lock ni aplica caducidad. Al finalizar el job como `COMPLETED` o `FAILED`, la consulta deja de encontrar un `RUNNING` y el personaje queda libre.

## PortraitGenerationReconciliationService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationReconciliationService.java`
**Para qué sirve:** Al arrancar, falla únicamente los jobs `RUNNING` que no constan como tareas vivas y limpia solo sus candidatos.

```java
taskRegistry.register(jobId); // TASK-007 lo registrará al iniciar el trabajo.
reconciliationService.reconcile();
taskRegistry.unregister(jobId);
```

**Notas:** El runner está activo por defecto fuera de pruebas. No aplica TTL; preserva jobs vivos y estados finales, y no elimina retratos aprobados ni candidatos de otro job.

## PortraitGenerationService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationService.java`
**Para qué sirve:** Inicia de forma asíncrona un ciclo de generación de cuatro candidatos para un personaje de catálogo.

```java
portraitGenerationService.generate(catalogCharacterId);
```

**Notas:** Crea primero un job `RUNNING` y registra la tarea viva antes de encolarla. El trabajo asíncrono exige exactamente cuatro imágenes, guarda sus assets y rutas como candidatos, marca `COMPLETED` y publica el evento de éxito. Un fallo de ComfyUI, almacenamiento, lote incompleto o encolado marca `FAILED`, limpia solo los candidatos de ese job y libera el registro; no hay deadline ni TTL. Una generación posterior vuelve a reemplazar únicamente el lote de candidatos y no toca el retrato aprobado.

## PortraitSelectionService

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/service/PortraitSelectionService.java`
**Para qué sirve:** Convierte un candidato vigente en el único retrato aprobado del personaje.

```java
portraitSelectionService.select(catalogCharacterId, candidateId);
```

**Notas:** La selección se serializa bloqueando el personaje. Rechaza candidatos inexistentes, de otro personaje, obsoletos o mientras exista un job `RUNNING`. Conserva el fichero del candidato elegido como retrato aprobado y elimina únicamente los otros tres ficheros y filas de la galería.

## CharacterCatalogUseCase (retratos)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterCatalogUseCase.java`
**Para qué sirve:** Expone desde el catálogo la generación, la galería y la selección de retratos.

```java
catalog.generatePortraits(catalogCharacterId);
boolean generating = catalog.generatingPortraits(catalogCharacterId);
var candidates = catalog.candidates(catalogCharacterId);
catalog.selectPortrait(catalogCharacterId, candidates.getFirst().id());
byte[] image = catalog.candidateAsset(catalogCharacterId, candidates.getFirst().id());
```

**Notas:** El asset solo se obtiene tras validar que el candidato pertenece al personaje indicado; no recibe una ruta de fichero del cliente. Las rutas REST equivalentes quedan bajo `/api/v1/catalog-characters/{id}/portraits`.

## Retrato aprobado del catálogo

**Ruta:** `GET /api/v1/catalog-characters/{id}/portraits/approved/asset`
**Para qué sirve:** Devuelve los bytes del retrato aprobado del personaje indicado.

```http
GET /api/v1/catalog-characters/7/portraits/approved/asset
```

**Notas:** El servidor resuelve el asset desde el identificador de catálogo; no acepta rutas de archivo del cliente. Responde 404 si el personaje no existe o no tiene retrato aprobado.

## StoryGenerateCommand (personaje de catálogo opcional)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/domain/command/StoryGenerateCommand.java`
**Para qué sirve:** Permite asociar explícitamente un personaje maestro al nuevo cuento sin romper solicitudes existentes.

```java
var legacy = new StoryGenerateCommand("Aventuras", "Un viaje en globo", StorySize.S);
var withCatalogCharacter = new StoryGenerateCommand("Aventuras", "Un viaje en globo", StorySize.S, catalogCharacterId);
```

**Notas:** Con `catalogCharacterId`, se rechaza el personaje bloqueado antes de llamar al agente y el protagonista del cuento guarda la FK explícita al catálogo. La ficha y el retrato se copian a un asset propio del cuento, por lo que cambios posteriores del catálogo no alteran el snapshot.

## CharacterReviewUseCase (confirmación de fork)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterReviewUseCase.java`
**Para qué sirve:** Resuelve una revisión visual; si el personaje ya tiene instantáneas de cuento, primero devuelve una confirmación de fork.

```java
var warning = reviews.resolve(new CatalogCharacterReviewResolutionCommand(
    catalogCharacterId, reviewId, RevisionStatus.CONFIRMED));
if (warning.forkConfirmationRequired()) {
    reviews.resolve(new CatalogCharacterReviewResolutionCommand(
        catalogCharacterId, reviewId, RevisionStatus.CONFIRMED, true));
}
```

**Notas:** Un personaje bloqueado rechaza la solicitud y la resolución. Una revisión visual sin cuentos invalida retrato y candidatos; con cuentos, el original no se modifica hasta confirmar el fork.
