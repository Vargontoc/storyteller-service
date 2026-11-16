# Ejemplos: character-inventory - server

## CharacterAgent

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterAgent.java`
**Para qué sirve:** Aísla la generación y revisión textual de una ficha de personaje del proveedor de IA. Una respuesta rechazada incluye el motivo y no contiene una ficha aplicable.

```java
CatalogCharacterAgentResult result = characterAgent.generateFromHint(
        "Una zorra curiosa que ayuda a sus amigos");

if (result.accepted()) {
    String name = result.name();
    String narrative = result.narrativeDescription();
    String visual = result.visualDescription();
} else {
    String reason = result.rejectionReason();
}
```

**Notas:** El agente debe estar disponible en Ollama; las solicitudes vacías se rechazan antes de invocarlo.

## SpringAiCharacterAgent

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiCharacterAgent.java`
**Para qué sirve:** Implementa `CharacterAgent` con el `ChatClient` cualificado del modelo de personaje y las reglas configuradas para edad y especies.

```java
CatalogCharacterReviewAgentResult proposal = characterAgent.review(
        new CatalogCharacterReviewAgentRequest(
                "Luna",
                "Una zorra curiosa",
                "Zorra naranja de cola esponjosa",
                "Orange fox with a fluffy tail",
                List.of("orange fur", "fluffy tail"),
                CharacterReviewTarget.BOTH,
                "Hazla más sonriente"));
```

**Notas:** El adaptador rechaza una respuesta aceptada que no incluya nombre, descripción narrativa y descripción visual.

## CharacterImageConditionPort

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterImageConditionPort.java`
**Para qué sirve:** Permite a los servicios consultar el estado visual de un personaje del catálogo sin depender de cómo se guardan o generan los assets.

```java
boolean draft = !imageConditions.hasApprovedPortrait(catalogCharacterId);
boolean hasImages = imageConditions.hasGeneratedImages(catalogCharacterId);
```

**Notas:** Es un puerto de solo lectura; su implementación productiva pertenece al módulo de assets.

## CharacterCatalogUseCase

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterCatalogUseCase.java`
**Para qué sirve:** Crea una ficha del catálogo mediante el agente; una respuesta rechazada no se persiste.

```java
CatalogCharacterAgentResult result = catalog.create(new CatalogCharacterCreateCommand("Una zorra curiosa"));
if (!result.accepted()) {
    String reason = result.rejectionReason();
}
```

**Notas:** Una respuesta rechazada o incompleta no crea ninguna ficha; la persistencia solo ocurre tras validar una respuesta aceptada y completa.

## CharacterCatalogUseCase (consulta)

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterCatalogUseCase.java`
**Para qué sirve:** Consulta fichas y renombra solo el maestro del catálogo.

```java
CatalogCharacterView character = catalog.get(catalogCharacterId);
CatalogCharacterView renamed = catalog.rename(catalogCharacterId, "Luna la zorra");
```

**Notas:** Sin proveedor de retrato, la ficha textual sigue disponible con `draft = null` y `portraitStatus = UNKNOWN`; con proveedor, el estado es `DRAFT` o `APPROVED`.

## CharacterReviewUseCase

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterReviewUseCase.java`
**Para qué sirve:** Solicita una propuesta de revisión y comunica si hay que confirmar el reemplazo de una propuesta pendiente.

```java
CatalogCharacterReviewRequestResult result = reviews.request(
        new CatalogCharacterReviewCommand(characterId, CharacterReviewTarget.VISUAL,
                "Haz su pelaje más claro", false));

if (result.replacementConfirmationRequired()) {
    result = reviews.request(new CatalogCharacterReviewCommand(characterId,
            CharacterReviewTarget.VISUAL, "Haz su pelaje más claro", true));
}
if (result.visualConsistencyWarning()) {
    // Informar de la posible pérdida de consistencia visual.
}
```

**Notas:** Una solicitud narrativa no consulta imágenes. El aviso solo aparece si una propuesta visual cambia realmente la descripción y existen imágenes generadas.

## Resolución de revisión de catálogo

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/application/ports/in/character/CharacterReviewUseCase.java`
**Para qué sirve:** Confirma o descarta una revisión pendiente del personaje indicado.

```java
reviews.resolve(new CatalogCharacterReviewResolutionCommand(
        characterId, reviewId, RevisionStatus.CONFIRMED));
```

**Notas:** Confirmar aplica solo los campos del target; descartar conserva íntegra la ficha. Una revisión ya resuelta o de otro personaje se rechaza.

## CharacterModelProvisioner

**Ruta:** `api/src/main/java/es/vargontoc/storyteller/infrastructure/config/CharacterModelProvisioner.java`
**Para qué sirve:** Al arrancar, crea el modelo configurado de personaje solo si Ollama no lo lista.

**Notas:** Usa `app.agents.character-base-model` y `app.agents.character-system`. Si Ollama o el modelo base no están disponibles, el arranque falla explícitamente para no publicar la API sin el agente requerido.
