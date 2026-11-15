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
