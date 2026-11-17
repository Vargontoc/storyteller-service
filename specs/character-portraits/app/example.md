# Ejemplos: character-portraits - app

## PortraitCandidateGallery

**Ruta:** `app/src/components/PortraitCandidateGallery.vue`
**Para qué sirve:** Muestra el lote vigente de cuatro retratos de un personaje y permite elegir uno o solicitar una regeneración.

```vue
<portrait-candidate-gallery
  :character-id="character.id"
  :name="character.name"
  :candidates="candidates"
  :locked="portraitRunning"
  :busy="portraitBusy"
  @generate="generate"
  @select="selectCandidate"
/>
```

**Notas:** Mientras `locked` sea verdadero, la galería permanece consultable y sus acciones quedan deshabilitadas.

## CharacterCatalogView

**Ruta:** `app/src/views/CharacterCatalogView.vue`
**Para qué sirve:** Coordina el estado REST de generación, la galería y las señales WebSocket del personaje seleccionado.

```ts
await generateCatalogCharacterPortraits(characterId)
await refreshPortrait(characterId)
```

**Notas:** El WebSocket solo provoca una recarga REST; no libera controles ni decide el estado del trabajo localmente.

## CharacterReviewPanel

**Ruta:** `app/src/components/CharacterReviewPanel.vue`
**Para qué sirve:** Presenta una propuesta de revisión visual y solicita confirmación antes de crear un personaje derivado cuando el original ya se usa en cuentos.

```vue
<character-review-panel
  :character-id="character.id"
  :locked="portraitRunning"
  @changed="refreshAfterReview"
/>
```

**Notas:** Cancelar la confirmación del derivado solo cierra el diálogo; no envía una resolución ni modifica al personaje original.

## TopicSelectionModal

**Ruta:** `app/src/components/TopicSelectionModal.vue`
**Para qué sirve:** Permite asociar opcionalmente un personaje del catálogo al nuevo cuento y evita enviar uno con retratos en generación.

```ts
const story = await generateStory(topicId, size, selectedCatalogCharacterId ?? undefined)
```

**Notas:** La disponibilidad se recarga desde REST al abrir el modal y cuando llega un evento de retrato; el servidor mantiene la validación definitiva del bloqueo.
