# Ejemplos: agent-director - app

## TopicSelectionModal

**Ruta:** `app/src/components/TopicSelectionModal.vue`
**Para qué sirve:** Crea un cuento a partir de un tema, un tamaño y un protagonista obligatorio: uno concreto del catálogo con retrato aprobado o la opción visible "Elección aleatoria".

```vue
<topic-selection-modal :open="showCreate" @close="showCreate = false" @created="openStory" />
```

```ts
// Elección concreta
await generateStory(topicId, 'M', 12)
// Elección aleatoria: el servidor elige entre los protagonistas aptos
await generateStory(topicId, 'M', null)
```

**Notas:** Solo se listan personajes con retrato aprobado (`getEligibleProtagonists`). Si no hay ninguno se muestra un aviso y no se envía ninguna solicitud. La lista se recarga al abrir y ante los eventos o reconexiones de retratos. Los errores definitivos del servidor se muestran tal cual.

## StoryScriptPanel

**Ruta:** `app/src/components/StoryScriptPanel.vue`
**Para qué sirve:** Muestra el guion de un cuento con escaleta (título, sinopsis, reparto con nombre y función, y beats por página con sus cinco campos) y permite aprobarlo, enviar un hint global, aceptar o descartar la propuesta candidata (con los beats cambiados resaltados o la indicación "Sin cambios") y editar un beat. El servidor decide los estados; tras cada mutación el panel recarga el guion y la revisión desde REST.

```vue
<story-script-panel :story-id="story.id" />
```

```ts
// Clientes usados (app/src/api/storyScript.ts y reviews.ts)
const script = await getStoryScript(storyId)           // scriptVersion nulo = cuento antiguo
await approveStoryScript(storyId)                      // SCRIPT_PENDING -> SCRIPT_ACCEPTED
const review = await reviewStory(storyId, 'Que acabe en la playa')
review.changedBeats                                    // páginas (desde 1) cambiadas; [] = sin cambios
await confirmStoryReview(storyId, 'CONFIRMED')         // o 'DISCARDED'

// Edición de beat: doble petición si el servidor avisa de páginas invalidadas
const first = await editStoryBeat(storyId, 2, beat)
if (first.status === 'CONFIRMATION_REQUIRED') {
  await editStoryBeat(storyId, 2, beat, true)          // confirmar: borra páginas y aplica
  // await editStoryBeat(storyId, 2, beat, false)      // cancelar: sin efectos
}
```

**Notas:** `REJECTED` muestra el motivo y no cambia la vista vigente. El aviso de invalidación usa `ConfirmModal`. Para un cuento con `scriptVersion` nulo muestra "Este cuento no tiene escaleta" (la elección de panel por cuento queda para TASK-012). Se añadió `httpClient.put`.


## StoryModal (orquestación por estado del guion)

**Ruta:** `app/src/components/StoryModal.vue` (con `app/src/components/content/StoryContent.vue`)
**Para qué sirve:** Decide la pestaña Guión solo por `scriptVersion`: con versión monta `StoryScriptPanel` (más `StoryContent` sin sinopsis ni revisión genérica, como resumen histórico con las acciones de página y borrado); sin versión conserva exactamente la UI previa. Con `status` `SCRIPT_PENDING` oculta las pestañas Portada y Páginas y deshabilita "Generar página"; el servidor sigue validando.

```vue
<story-script-panel v-if="hasOutline" :story-id="storyId" @changed="onScriptChanged" />
<story-content :story-id="storyId" :outlined="hasOutline" :generation-blocked="scriptPending" />
```

```ts
const script = await getStoryScript(storyId)           // el modal lo consulta junto al cuento y las páginas
const hasOutline = script.scriptVersion !== null       // nunca por campos opcionales
const scriptPending = hasOutline && script.status === 'SCRIPT_PENDING'
```

**Notas:** `StoryScriptPanel` emite `changed` tras aprobar, confirmar una revisión o aplicar una edición; el modal recarga cuento, guion, páginas y pestañas. `storyteller.ts` no se amplía: el modal usa `getStoryScript`.
