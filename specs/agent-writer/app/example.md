# Ejemplos: agent-writer - app

## storyPages (cliente REST de portada y páginas)

**Ruta:** `app/src/api/storyPages.ts`
**Para qué sirve:** Cliente de las operaciones de portada y páginas de un cuento con escaleta (base `/api/v1/storyteller/{storyId}`): lectura del estado, generación, hint, regeneración, versión vigente, aceptación, descarte de propuestas e imagen aceptada. Desenvuelve el envoltorio `{success, data}`; los rechazos del servidor (409, 400, 404) llegan como `ApiError` con su motivo. La generación (portada, página, hint, regenerar) es asíncrona: responde 202 y el resultado llega por WebSocket. `pageId` es el id de la fila de página (`PageDetail.id`), salvo en `generatePage`, que recibe el número de página. La portada es la página 0. La imagen solo existe para contenido aceptado (`accepted.hasImage`); las versiones y propuestas nunca la llevan. El cabecero del fichero documenta la equivalencia con las rutas propuestas del sprint. Solo cubre cuentos con escaleta; los antiguos siguen con `getStoryPages` de `storyteller.ts`.

```ts
import { ApiError } from './api/httpClient'
import {
  acceptPage, acceptProposal, generatePage, getAcceptedImage, getStoryPagesView, sendHint,
} from './api/storyPages'

const view = await getStoryPagesView(storyId) // { generating, nextPage, pages: PageDetail[] }

try {
  if (view.nextPage !== null && !view.generating) {
    await generatePage(storyId, view.nextPage) // 202; llegará el evento por WebSocket
  }
  await sendHint(storyId, page.id, 'más corto') // el hint vacío se valida antes en el panel
  await acceptPage(storyId, page.id)            // página PENDING
} catch (error) {
  if (error instanceof ApiError) toast(error.message) // p. ej. "Hay una generación en curso"
}

// Reapertura: primero sin confirmar; si hay páginas posteriores no cambia nada y avisa.
let result = await acceptProposal(storyId, page.id)
if (result.requiresConfirmation) {
  // preguntar al usuario: se descartarán result.affectedPages páginas posteriores
  result = await acceptProposal(storyId, page.id, true)
}

// Imagen solo del contenido aceptado
if (page.accepted?.hasImage) {
  const url = URL.createObjectURL(await getAcceptedImage(storyId, page.id))
}
```

## websocket-store: eventos de portada y página

**Ruta:** `app/src/stores/websocket.ts`
**Para qué sirve:** Además de notificaciones y retratos, el store recibe por `/storyteller` los eventos de generación de portada y página y deja el último en `lastStoryPageEvent`. Forma: `{ storyId, pageNumber (0 = portada), type: 'STARTED' | 'VERSION_PRESENTED' | 'HINT_REJECTED' | 'FAILED', reason?, id }`. `reason` solo viene en rechazo de hint y fallo. `id` es local y cambia con cada evento, así que un watch reacciona aunque lleguen dos iguales seguidos. Los eventos de página no muestran toast; el consumidor decide qué enseñar. Los de retratos y las notificaciones genéricas siguen igual. `connectionId` sube en cada apertura del socket: sirve para recargar el estado tras reconectar.

```ts
import { watch } from 'vue'
import { useWebsocketStore } from './stores/websocket'

const ws = useWebsocketStore()

watch(() => ws.lastStoryPageEvent, (event) => {
  if (!event || event.storyId !== openStoryId) return // evento de otro cuento
  if (event.type === 'VERSION_PRESENTED') reloadPage(event.pageNumber)
  if (event.type === 'HINT_REJECTED' || event.type === 'FAILED') showMessage(event.reason)
})

watch(() => ws.connectionId, () => reloadWholeState()) // reconexión
```

## StoryPagesPanel (panel de páginas: vista y acciones básicas)

**Ruta:** `app/src/components/StoryPagesPanel.vue`
**Para qué sirve:** Muestra la portada y las páginas de un cuento con escaleta. Carga el estado por REST (`getStoryPagesView`) al montar y tras cada acción confirmada; el servidor decide `nextPage` y las acciones de cada página, y el panel no recalcula reglas de secuencia. Portada con título y escena; página con texto y escena; la imagen solo se pide para contenido aceptado con `hasImage` (blob URL que se libera al desmontar). Ofrece "Generar portada" o "Generar página N" solo si `nextPage` lo indica y no hay generación en curso; aceptar no genera la siguiente. Acciones básicas: Aceptar (`ACCEPT`) y Enviar hint (`HINT`); el hint se recorta y, si queda vacío, no se envía y se avisa. Los rechazos del servidor se muestran con su motivo. No hay campos editables de texto ni de escena. Histórico y regeneración: ver la sección siguiente; la reapertura llega en TASK-033. La generación es asíncrona (202): hasta que exista la suscripción WebSocket (TASK-034), el botón "Recargar" actualiza el estado. Se monta desde la pestaña "Páginas" de `StoryModal.vue` para cuentos con escaleta (TASK-035).

```vue
<script setup lang="ts">
import StoryPagesPanel from './components/StoryPagesPanel.vue'
</script>

<template>
  <!-- Solo para cuentos con escaleta (scriptVersion no nulo) -->
  <StoryPagesPanel :story-id="storyId" />
</template>
```

## StoryPagesPanel: histórico, avisos, vigente y regeneración sin hint

**Ruta:** `app/src/components/StoryPagesPanel.vue`
**Para qué sirve:** Para una portada o página pendiente, pinta todas las versiones recibidas en `PageDetail.versions` (también 11 o más), sin imagen. Solo la vigente (`current`) lleva la etiqueta "Vigente" y borde destacado; el resto ofrece "Marcar como vigente" (si el servidor incluye `SELECT_VERSION` en `actions`), que llama a `selectVersion` y recarga el estado. Cada versión muestra su aviso junto al contenido (`TOO_MANY_WORDS`, `SENTENCE_COUNT`, `BOTH`; `NONE` no muestra nada). Los botones salen de `actions` del servidor, sin recalcular longitud: con aviso de frases en la vigente el servidor omite `ACCEPT` y envía `REGENERATE`, y el panel muestra "Enviar hint" y "Regenerar sin hint" (`regenerate`, 202 asíncrono, recarga tras la petición); con aviso solo de palabras siguen Aceptar y hint; si el aviso está en una versión no vigente no hay `REGENERATE`. La recarga por evento WebSocket llega en TASK-034.

```ts
// Lo que el panel recibe del servidor para una página pendiente con aviso de frases en la vigente
const page: PageDetail = {
  id: 10, page: 1, status: 'PENDING', beatId: 1, title: null, accepted: null, proposals: [],
  versions: [
    { id: 100, text: 'Texto largo...', scene: 'Un bosque', warning: 'SENTENCE_COUNT', current: true },
    { id: 101, text: 'Otro texto', scene: 'Un río', warning: 'NONE', current: false },
  ],
  actions: ['HINT', 'REGENERATE', 'SELECT_VERSION'], // sin ACCEPT
}
// Resultado: dos versiones, la 100 destacada con su aviso; botones "Marcar como vigente" (en la 101),
// "Enviar hint" y "Regenerar sin hint"; no hay "Aceptar".
```

## StoryPagesPanel: recarga por eventos de generación

**Ruta:** `app/src/components/StoryPagesPanel.vue` (consume `lastStoryPageEvent` y `connectionId` de `app/src/stores/websocket.ts`, sin cambios en el store)
**Para qué sirve:** El panel se suscribe con `watch` al último evento de páginas y a `connectionId`, así que el resultado de una generación aparece sin pulsar "Recargar". Ignora los eventos de otros cuentos. `STARTED` muestra "Generando la portada / la página N..." (indicador local, aparte de `view.generating` del servidor). `VERSION_PRESENTED` quita el indicador y recarga el estado completo por REST (`getStoryPagesView`). `HINT_REJECTED` y `FAILED` muestran el `reason` y recargan, de modo que se conserva el contenido y, cuando el servidor confirma que ya no genera, se libera el botón para reintentar. El error se limpia con el siguiente `STARTED`. Un cambio de `connectionId` (reconexión) recarga el panel completo. El estado siempre se relee de REST; el evento solo es la señal. Montado en `StoryModal.vue` para cuentos con escaleta (TASK-035).

```ts
// Dentro del panel (resumen)
watch(() => websocketStore.lastStoryPageEvent?.id, async () => {
  const event = websocketStore.lastStoryPageEvent
  if (!event || event.storyId !== props.storyId) return // otro cuento
  if (event.type === 'STARTED') { operation.value = event.pageNumber; return }
  operation.value = null
  eventError.value = event.type === 'VERSION_PRESENTED' ? null : (event.reason ?? 'La generación no se completó')
  await load()
})
watch(() => websocketStore.connectionId, async () => { operation.value = null; await load() })
```

## StoryPagesPanel: reapertura, confirmación en cascada y descarte de propuestas

**Ruta:** `app/src/components/StoryPagesPanel.vue` (reutiliza `app/src/components/ConfirmModal.vue` sin cambios)
**Para qué sirve:** En una página aceptada reabierta (con `proposals`), el panel mantiene el contenido aceptado arriba, con su imagen si `hasImage`, y debajo una sección "Propuestas" sin imagen con la vigente destacada. "Marcar como vigente" solo aparece en las propuestas, nunca en la aceptada. Los botones salen de `actions` del servidor: "Aceptar propuesta" (`ACCEPT_PROPOSAL`) y "Descartar propuestas" (`DISCARD_PROPOSALS`). Aceptar llama primero a `acceptProposal(..., false)`: si responde `requiresConfirmation`, no ha cambiado nada y se abre `ConfirmModal` con `affectedPages` ("Se eliminarán 2 páginas posteriores"); solo "Aceptar y eliminar" repite la llamada con `confirm=true`, y "Cancelar" cierra sin mutar. Sin posteriores (p. ej. la portada) no hay modal. Tras cualquier respuesta el panel recarga el estado del servidor, sin borrado optimista. Descartar llama a `discardProposals` y recarga: vuelve a verse solo la aceptada. No hay edición manual. Montado en `StoryModal.vue` para cuentos con escaleta (TASK-035).

```ts
// Lo que el panel recibe para la página 2 reabierta, con las páginas 3 y 4 ya aceptadas
const page: PageDetail = {
  id: 20, page: 2, status: 'ACCEPTED', beatId: 2, title: null,
  accepted: { text: 'Texto aceptado', scene: 'Un bosque', hasImage: true },
  versions: [],
  proposals: [{ id: 200, text: 'Propuesta', scene: 'Un río', warning: 'NONE', current: true }],
  actions: ['HINT', 'SELECT_VERSION', 'ACCEPT_PROPOSAL', 'DISCARD_PROPOSALS'],
}
// "Aceptar propuesta" -> acceptProposal(7, 20, false) -> { requiresConfirmation: true, affectedPages: 2 }
// Modal: "Se eliminarán 2 páginas posteriores. ¿Continuar?"
// Confirmar -> acceptProposal(7, 20, true) y recarga; Cancelar -> no hay más llamadas.
// "Descartar propuestas" -> discardProposals(7, 20) y recarga.
```

## StoryModal: pestaña de páginas según escaleta

**Ruta:** `app/src/components/StoryModal.vue`
**Para qué sirve:** El modal decide qué vista de páginas montar solo por `scriptVersion` del guion que ya carga (`hasOutline`; nulo = cuento antiguo), sin mirar si hay páginas ni campos opcionales. Cuento con escaleta y guion aprobado: una única pestaña "Páginas" que monta `StoryPagesPanel` (el panel muestra portada y páginas, por eso no hay pestaña "Portada"); su visibilidad no depende de la lista legacy `getStoryPages`. Con el guion pendiente (`SCRIPT_PENDING`) no hay pestañas de portada ni páginas. Cuento antiguo: sin cambios, pestañas "Portada" y "Páginas" según la lista legacy y `PagesContent` con sus acciones y rutas.

```vue
<!-- Dentro de la pestaña 'paginas' de StoryModal.vue -->
<story-pages-panel v-if="storyId && hasOutline" :key="contentKey" :story-id="storyId" />
<pages-content v-else-if="storyId" :key="contentKey" :story-id="storyId" :is-cover="false" />
```
