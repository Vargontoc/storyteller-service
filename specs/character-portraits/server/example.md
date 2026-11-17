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
List<byte[]> portraits = portraitGenerator.generatePortraits(
    "zorra naranja con cola esponjosa"
);
```

**Notas:** El puerto devuelve exactamente cuatro imágenes o propaga el fallo del proveedor.

## PortraitComfyUiAdapter

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/PortraitComfyUiAdapter.java`
**Para qué sirve:** Implementa el puerto con un único workflow de ComfyUI que genera cuatro variantes cartoon de la misma descripción visual.

```java
List<byte[]> portraits = adapter.generatePortraits("zorra naranja con cola esponjosa");
```

**Notas:** Espera el historial de ComfyUI sin aplicar un límite temporal de generación y rechaza un lote que no tenga cuatro imágenes.

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
