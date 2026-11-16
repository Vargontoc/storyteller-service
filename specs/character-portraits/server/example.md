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
