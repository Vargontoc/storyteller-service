# Ejemplos: agent-narrator - app

## storyPages: indicadores de audio y narración en curso

**Ruta:** `app/src/api/storyPages.ts`
**Para qué sirve:** `StoryPagesView` y `PageDetail` incorporan los indicadores autoritativos del servidor: `view.hasAudio` (el cuento tiene audio publicado), `view.narrating` (se está narrando) y `hasAudio` en cada página (la portada, page 0, siempre es `false`). El cliente no infiere el audio por la voz ni la narración por eventos: solo lee estos campos. Mientras el cuento se narra, las mutaciones de páginas (hint, regenerar, versión vigente, aceptar propuesta...) responden 409; llega como `ApiError` con el mensaje del servidor ("El cuento se está narrando: no se pueden reabrir, cambiar ni eliminar sus páginas") y `status` 409, listo para mostrarlo en el panel.

```ts
import { ApiError } from './api/httpClient'
import { acceptProposal, getStoryPagesView } from './api/storyPages'

const view = await getStoryPagesView(storyId)
const blocked = view.narrating // deshabilitar reabrir/cambiar mientras se narra

// Solo las páginas interiores con audio ofrecen reproductor
const withAudio = view.pages.filter((p) => p.page > 0 && p.hasAudio)

try {
  await acceptProposal(storyId, page.id, true)
} catch (error) {
  // Carrera: la narración empezó tras cargar la vista
  if (error instanceof ApiError && error.status === 409) showMessage(error.message)
}
```

## voices: cliente de la biblioteca de voces

**Ruta:** `app/src/api/voices.ts`
**Para qué sirve:** aísla las rutas (`/api/v1/voices`) y los envelopes del servidor. `listVoices` devuelve la biblioteca (la voz del sistema va primera, con `system: true` y sin `id`) junto con `systemVoiceAvailable`, `systemVoiceUnavailableReason` y `systemVoiceNameCollision`. `getLanguages` devuelve el catálogo con `defaultLanguage` (español). `uploadVoice` envía multipart (`file`, `name`, `language`, `permission`) sin fijar `Content-Type`. `deleteVoice` acepta un id o `'system'`. `listEligibleVoices` devuelve las voces elegibles para narrar. Los rechazos llegan como `ApiError`: `message` con el texto y `code` con el motivo estable del servidor (`errors[0]`: `NAME_REQUIRED`, `LANGUAGE_REQUIRED`, `PERMISSION_REQUIRED`, `INVALID_FORMAT`, `INVALID_DURATION`, `NAME_DUPLICATED`, `SYSTEM_VOICE_NOT_DELETABLE`, `VOICE_NOT_FOUND`).

```ts
import { ApiError } from './api/httpClient'
import { deleteVoice, getLanguages, listVoices, uploadVoice } from './api/voices'

const library = await listVoices()
if (!library.systemVoiceAvailable) showMessage(library.systemVoiceUnavailableReason)

const { defaultLanguage } = await getLanguages() // preseleccionar el idioma del formulario

try {
  await uploadVoice({ name: 'Mamá', language: defaultLanguage.code, permission: true, file })
} catch (error) {
  if (error instanceof ApiError && error.code === 'NAME_DUPLICATED') showMessage(error.message)
}

await deleteVoice(3) // solo voces subidas; deleteVoice('system') responde 409 SYSTEM_VOICE_NOT_DELETABLE
```

## storyNarration: cliente de narración del cuento

**Ruta:** `app/src/api/storyNarration.ts`
**Para qué sirve:** aísla las rutas (`/api/v1/storyteller/{storyId}`) y los envelopes de la narración. `startNarration(storyId, voiceId)` inicia o renarra con una voz elegible (id numérico o `'system'`; se envía como string en `{ voiceId }`); es asíncrono (202): devuelve el `NarrationState` ya en curso y no modifica estado local, el resultado llega por WebSocket. `getNarrationState` devuelve el estado autoritativo (`hasOutline`, `canNarrate`, `disabledReason`, `narrating`, `hasAudio`, `voiceName`, `lastResult`); nunca incluye el guion de audio. `getPageAudio(storyId, pageNumber)` devuelve el `Blob` WAV de una página interior; la portada (`pageNumber < 1`) se rechaza en el cliente sin llamar al servidor. Los rechazos llegan como `ApiError`: `message` con el texto y `code` con el motivo estable (`NO_OUTLINE`, `PAGES_NOT_ACCEPTED`, `IN_PROGRESS`, `VOICE_NOT_AVAILABLE`, `NO_VOICES_AVAILABLE`, `GENERATION_IN_PROGRESS`, `AUDIO_NOT_FOUND`).

```ts
import { ApiError } from './api/httpClient'
import { getNarrationState, getPageAudio, startNarration } from './api/storyNarration'

const state = await getNarrationState(storyId)
if (!state.canNarrate) showMessage(state.disabledReason) // NO_OUTLINE | IN_PROGRESS | PAGES_NOT_ACCEPTED

try {
  await startNarration(storyId, 'system') // o el id de una voz elegible, p. ej. 12
} catch (error) {
  if (error instanceof ApiError && error.status === 409) showMessage(error.message, error.code)
}

// Solo páginas interiores con audio (page >= 1)
const url = URL.createObjectURL(await getPageAudio(storyId, 2))
// ... al cambiar de página o desmontar: URL.revokeObjectURL(url)
```

## websocket-store: eventos de narración

**Ruta:** `app/src/stores/websocket.ts`
**Para qué sirve:** `lastNarrationEvent` guarda el último evento de narración de `/storyteller`: `{ storyId, type: 'STARTED' | 'COMPLETED' | 'FAILED', voiceName?, id }`. `voiceName` solo llega en `COMPLETED`; `FAILED` no lleva motivo. Cada mensaje recibe un `id` local, de modo que dos eventos iguales seguidos también se observan. El discriminador frente a los eventos de página (que también usan `STARTED` y `FAILED`) es `pageNumber`: con `pageNumber` numérico va a `lastStoryPageEvent`; sin él, a `lastNarrationEvent`. Los eventos son solo señales de recarga: el estado se vuelve a pedir por REST (`getNarrationState`). Tras una reconexión `connectionId` cambia y también debe provocar esa recarga.

```ts
import { watch } from 'vue'
import { useWebsocketStore } from './stores/websocket'
import { getNarrationState } from './api/storyNarration'

const ws = useWebsocketStore()

const reload = async () => { state.value = await getNarrationState(storyId) }

// Recargar ante un evento del cuento abierto
watch(() => ws.lastNarrationEvent?.id, () => {
  if (ws.lastNarrationEvent?.storyId === storyId) reload()
})
// ... y tras reconectar (se pudo perder algún evento)
watch(() => ws.connectionId, reload)
```

## story-narration-panel: panel de narración del cuento

**Ruta:** `app/src/components/StoryNarrationPanel.vue`
**Para qué sirve:** controla la narración de un cuento con escaleta. Prop: `storyId`. Carga el estado con `getNarrationState` y muestra «Narrar» (o «Renarrar» si `hasAudio`), habilitado según `canNarrate`; si no lo está, muestra el motivo autoritativo (`disabledReason`: faltan páginas por aceptar, narración en curso, sin escaleta). Al pulsar la acción pide `listEligibleVoices` y muestra solo esas voces, con la del sistema preseleccionada si está disponible (si no, sin preselección; si no hay ninguna, avisa y remite a la sección Voces). Al renarrar, antes de iniciar, pide confirmación con `ConfirmModal` avisando de que se sustituye el audio actual; si se cancela no se llama al servidor. Iniciar es asíncrono: solo muestra el estado en curso devuelto (`narrating`: indicador y botón deshabilitado). Recarga el estado únicamente ante eventos de narración con su `storyId` (`lastNarrationEvent`) o tras una reconexión (`connectionId`); entonces muestra «Narrado con <voz>» (`voiceName`) o, si `lastResult` es `ERROR`, el aviso de error con narrar habilitado. Los rechazos 409 se traducen por `ApiError.code` (`GENERATION_IN_PROGRESS`, `NO_VOICES_AVAILABLE`, `VOICE_NOT_AVAILABLE`, `PAGES_NOT_ACCEPTED`, `IN_PROGRESS`, `NO_OUTLINE`). Nunca solicita ni muestra el guion de audio. `StoryModal` lo monta (TASK-032) en la pestaña «Páginas», encima del panel de páginas, solo cuando el cuento tiene escaleta (`hasOutline`, derivado de `scriptVersion`); los cuentos antiguos no lo muestran y conservan su vista. La clave `narration-${storyId}` evita reutilizar el estado de otro cuento. Nota: en cuentos con escaleta, `StoryContent` (pestaña «Guion») ya no muestra el botón antiguo «Generar narración» (flujo FUN-004, `generateStoryAudio`); lo oculta con su prop `outlined`. Los cuentos antiguos lo conservan.

```vue
<script setup lang="ts">
import StoryNarrationPanel from './components/StoryNarrationPanel.vue'
</script>

<template>
  <story-narration-panel :story-id="story.id" />
</template>
```

## story-pages-panel: reproductor del audio de la página

**Ruta:** `app/src/components/StoryPagesPanel.vue`
**Para qué sirve:** en cada página interior con `hasAudio` verdadero (la portada y los cuentos sin audio no lo muestran) ofrece el botón «Reproducir audio». Al pulsarlo pide con `getPageAudio` solo el audio de esa página, crea una URL temporal (`URL.createObjectURL`) y lo reproduce; al terminar (`ended`), al reproducir otra página, al recargar la vista o al desmontar detiene el audio y libera la URL (`revokeObjectURL`). Nunca encadena la página siguiente. Si la carga falla (por ejemplo `AUDIO_NOT_FOUND`) muestra un aviso bajo esa página sin afectar al resto del panel. Tras un evento `COMPLETED` del cuento abierto (`lastNarrationEvent`) o un cambio de `connectionId` recarga la vista REST, de modo que el reproductor aparece (o desaparece si se borró la narración) sin intervención manual; no se crea audio de forma optimista. El audio sigue reproduciéndose aunque se haya borrado la voz subida: el cliente no depende de la voz.

```vue
<script setup lang="ts">
import StoryPagesPanel from './components/StoryPagesPanel.vue'
</script>

<template>
  <!-- El reproductor aparece solo en las páginas interiores con hasAudio -->
  <story-pages-panel :story-id="story.id" />
</template>
```

## story-pages-panel: aviso de borrado de audio y bloqueo durante la narración

**Ruta:** `app/src/components/StoryPagesPanel.vue` (y `StoryModal.vue` para la recarga del panel de narración)
**Para qué sirve:** (1) el aviso de confirmación del descarte en cascada al aceptar una propuesta reabierta añade «Se borrará todo el audio de la narración y la voz» solo si `view.hasAudio`; sin audio el aviso es el de siempre y cancelar no envía ninguna petición. (2) Con `view.narrating` el panel muestra «El cuento se está narrando: no se pueden reabrir, cambiar ni eliminar sus páginas» y deshabilita enviar hint (reabrir), regenerar, marcar versión vigente y aceptar propuesta; aceptar una página y descartar propuestas no se bloquean. Si aun así llega un 409 `IN_PROGRESS` por carrera, se muestra el motivo del servidor y se recarga la vista sin dar la acción por hecha. (3) No hay audio optimista: tras confirmar se recarga la vista REST (el reproductor desaparece) y el panel emite `changed`; `StoryModal` lo usa para volver a montar `StoryNarrationPanel` (clave `narration-<id>-<rev>`), que así relee su estado y muestra la narración deshabilitada hasta reaceptar las páginas, ya que el borrado del audio no genera un evento de narración.

```vue
<template>
  <story-narration-panel :key="`narration-${storyId}-${narrationRev}`" :story-id="storyId" />
  <story-pages-panel :story-id="storyId" @changed="narrationRev++" />
</template>
```

## story-script-panel: aviso de borrado de audio al editar un beat

**Ruta:** `app/src/components/StoryScriptPanel.vue` (tipo `BeatEditResult.audioWillBeDeleted` en `app/src/api/storyScript.ts`; `StoryModal.vue` para el refresco de la narración)
**Para qué sirve:** la edición de un beat que invalida páginas sigue en dos pasos (primera petición, aviso, confirmación). Si el servidor responde `CONFIRMATION_REQUIRED` con `audioWillBeDeleted` verdadero, el aviso añade «Se borrará todo el audio de la narración y la voz»; si es falso o falta, el aviso es el de siempre (nunca se deduce del estado local). Si al confirmar el servidor responde 409 `IN_PROGRESS` (el cuento se está narrando), se muestra su motivo en el formulario del beat sin recargar ni emitir `changed`. Tras confirmar con éxito se recarga el guion y se emite `changed`; `StoryModal` lo usa para volver a montar `StoryNarrationPanel` (clave `narration-<id>-<rev>`), que así muestra el cuento sin audio ni voz.

```vue
<template>
  <story-narration-panel :key="`narration-${storyId}-${narrationRev}`" :story-id="storyId" />
  <story-script-panel :story-id="storyId" @changed="narrationRev++" />
</template>
```

## voice-library-view: vista de la biblioteca de voces

**Ruta:** `app/src/views/VoiceLibraryView.vue`
**Para qué sirve:** tercera sección principal de la app (botón «Voces» de la navegación de `App.vue`). Carga la biblioteca con `voices.ts`, lista cada voz con nombre, idioma (las voces ya subidas pueden tener otro) y la marca «Voz del sistema» (la del sistema no ofrece borrar), avisa si la voz del sistema no está disponible (con su motivo) o si su nombre coincide con una voz subida, y ofrece el formulario de subida (nombre, fichero WAV/MP3 y casilla de permiso; sin selector de idioma: la subida envía siempre `language: 'es'`). Los rechazos de subida se traducen por `ApiError.code` a avisos legibles y no alteran la lista; el borrado pide confirmación con `ConfirmModal` y solo recarga la lista tras el éxito, sin actualización optimista. No recibe props ni emite eventos.

```vue
<script setup lang="ts">
import { ref } from 'vue'
import VoiceLibraryView from './views/VoiceLibraryView.vue'

const activeView = ref<'stories' | 'characters' | 'voices'>('voices')
</script>

<template>
  <button type="button" @click="activeView = 'voices'">Voces</button>
  <voice-library-view v-if="activeView === 'voices'" />
</template>
```
