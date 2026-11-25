# Ejemplos de uso: app (export-import-feature)

## story-script-api (`app/src/api/storyScript.ts`)

Cliente del guion con el lugar de cada beat, el planteamiento de portada y el resultado de aprobación con `coverStarted`. En cuentos anteriores `setting` y `cover` llegan `null` o ausentes; no se derivan del beat 1.

```ts
import { approveStoryScript, editStoryBeat, getStoryScript } from '../api/storyScript'

const script = await getStoryScript(7)
script.beats[0].setting // lugar del beat (null/undefined en cuentos anteriores)
script.cover?.setting // lugar de la portada
script.cover?.tone // tono de la portada

// El servidor exige `setting` no vacío al editar un beat (si no, 400).
await editStoryBeat(7, 2, { ...script.beats[1], setting: 'Playa' })

const { story, coverStarted } = await approveStoryScript(7)
if (!coverStarted) {
  // mostrar la acción manual de generar la portada
}
```

`CandidateOutline` (`app/src/api/reviews.ts`) también lleva `cover?: { setting, tone } | null` y `setting` en sus beats.

## story-pages-api (`app/src/api/storyPages.ts`)

Cliente de portada y páginas. Se añade la subida de imagen (`uploadPageImage`, multipart con la parte `file`; el navegador fija el `Content-Type`). El hint de la portada reutiliza `sendHint` con el `id` de la página 0 (no hay ruta ni estado propios). Los rechazos llegan como `ApiError` con el mensaje y el estado del servidor.

```ts
import { sendHint, uploadPageImage } from '../api/storyPages'
import { ApiError } from '../api/httpClient'

// Subir o sustituir la imagen de la portada o de una página aceptada (pageId = id de la fila).
try {
  await uploadPageImage(7, pageId, fichero) // fichero: File del <input type="file">
} catch (e) {
  if (e instanceof ApiError) {
    e.status // 404 otro cuento, 409 sin aceptar, 415 "El fichero no es una imagen"
    e.message
  }
}

// Hint sobre la portada: pageId de la página con page === 0.
const cover = view.pages.find((p) => p.page === 0)!
await sendHint(7, cover.id, 'que se vea la luna llena')
// 409 "Al guion le faltan el lugar y el tono de la portada" si el cuento no tiene planteamiento de portada
```

## story-export-api (`app/src/api/storyExports.ts`)

Cliente de las dos exportaciones síncronas (`GET /api/v1/storyteller/{id}/export/nubi-game` y `.../export/visual-agent`). Devuelve una unión discriminada: `download` (zip y nombre de fichero del servidor) o `rejected` (409, con la lista completa de faltas tal como llega). `page` 0 es la portada, N una página interior y -1 una falta de todo el cuento; `code` es el código del contrato o `null` (siempre `null` en Nubi Game). Otros errores (p. ej. 404 si el cuento no existe) se propagan como `ApiError`. La SPA no prevalida ni deduce la disponibilidad.

```ts
import { exportNubiGame, exportVisualAgent } from '../api/storyExports'

const result = await exportNubiGame(7) // o exportVisualAgent(7)
if (result.kind === 'download') {
  const url = URL.createObjectURL(result.blob)
  // <a :href="url" :download="result.filename"> ... luego URL.revokeObjectURL(url)
} else {
  for (const f of result.faults) {
    console.log(f.page, f.missing, f.message, f.code) // 0 portada, N página, -1 cuento
  }
}
```

`ApiError` ahora conserva además la carga útil `data` de la respuesta de error (`e.data`).

## story-export-actions (`app/src/views/StorytellerView.vue`)

Lista de cuentos. Las opciones de cada fila ofrecen dos acciones, `Exportar para Nubi Game` y `Exportar para agente visual`, que sustituyen a la descarga heredada (ya no existe `downloadStory`). Se muestran siempre: la disponibilidad la decide el servidor, la vista no prevalida. En éxito se descarga el zip con el nombre recibido (la URL del blob se crea y se revoca solo entonces). Ante un rechazo no se descarga nada y un bloque `.export-rejection` lista todas las faltas recibidas con su contexto (`Portada`, `Página N` o `Cuento`) y su mensaje. Otros errores (p. ej. 404) salen como aviso.

```vue
<!-- Fila de cada cuento -->
<button data-export="nubi-game" @click.stop="onExport(story, 'nubi-game')">Exportar para Nubi Game</button>
<button data-export="visual-agent" @click.stop="onExport(story, 'visual-agent')">Exportar para agente visual</button>

<!-- Rechazo -->
<div class="export-rejection">
  <li class="export-fault"><strong>Portada:</strong> Falta la imagen</li>
  <li class="export-fault"><strong>Página 2:</strong> Falta el audio</li>
</div>
```

## story-script-panel (`app/src/components/StoryScriptPanel.vue`)

Panel de la escaleta. Cada beat muestra su `Lugar` (o "sin definir" si el cuento es anterior) y un bloque `Portada` aparte con el lugar y el tono de `script.cover`; sin `cover` muestra "Este cuento no tiene planteamiento de portada" sin derivar nada del beat 1. El formulario de edición carga `beat.setting` y lo envía (el servidor lo exige no vacío; sus errores se muestran sin cambiar el guion). La propuesta candidata también muestra su `cover`. Si la aprobación devuelve `coverStarted: false`, muestra un único aviso que remite a pedir la portada a mano desde el panel de páginas (TASK-021).

```vue
<StoryScriptPanel :story-id="7" @changed="reloadStory" />
```

## story-pages-panel (`app/src/components/StoryPagesPanel.vue`)

Panel de portada y páginas. Cada sección (`data-page="0"` para la portada, `1..n` para interiores) muestra completa y sin recortes la `accepted.scene` de su propio detalle, tal y como la entrega el servidor; nunca reutiliza la escena de otra página. La portada y la página 1 pueden mostrar escenas distintas.

```vue
<StoryPagesPanel :story-id="7" />
<!-- Portada:  <section data-page="0"> ... <p class="scene">Escena: {{ portada.accepted.scene }}</p> -->
<!-- Página 2: <section data-page="2"> ... <p class="scene">Escena: {{ pagina2.accepted.scene }}</p> -->
```

### Subida de imagen (TASK-020)

Cada portada o página con contenido aceptado muestra un selector `Subir imagen` (`<input type="file">`); en portada o página pendiente no aparece. Al elegir un fichero se llama a `uploadPageImage(storyId, page.id, file)` sin prevalidar tipo ni tamaño. Tras éxito el panel recarga la vista y vuelve a descargar la imagen aceptada (las URL temporales anteriores se liberan con el ciclo habitual). Si el servidor rechaza (p. ej. 415 "El fichero no es una imagen" o 409 sin aceptar), se muestra su mensaje y se conserva la imagen ya cargada.

```vue
<StoryPagesPanel :story-id="7" />
<!-- <section data-page="3"> ... <label class="upload">Subir imagen <input type="file" @change="upload(page.id, $event)" /></label> -->
```

### Revisión de la portada y bloqueo de la página 1 (TASK-021)

La portada es la página 0 y sigue el ciclo de cualquier página: `Generar portada` (cuando `nextPage` es 0, p. ej. si la aprobación no la inició), `Aceptar`, `Enviar hint` (`sendHint` con el `id` de la página 0) y, ya aceptada, propuestas de cambio con `Aceptar propuesta`. Los eventos WebSocket `VERSION_PRESENTED`, `HINT_REJECTED` y `FAILED` con `pageNumber: 0` solo recargan la vista REST; el motivo del rechazo o del fallo se muestra en `.event-error` y tras un fallo vuelve a ofrecerse `Generar portada`.

`Generar página 1` solo aparece si `nextPage` es 1 y la portada de la vista está aceptada; aceptar la portada la habilita pero no la genera. En cuentos anteriores sin planteamiento de portada, generar la portada o enviar un hint muestra el rechazo 409 del servidor: "Al guion le faltan el lugar y el tono de la portada". Al aceptar una nueva versión de la portada la recarga refleja que solo la portada pierde su imagen y que las páginas interiores aceptadas conservan texto, imagen y audio.

```vue
<StoryPagesPanel :story-id="7" />
<!-- Portada pendiente:  <section data-page="0"> versión + [Aceptar] [Enviar hint] ; sin botón "Generar página 1" -->
<!-- Portada aceptada:   botón "Generar página 1" (nextPage === 1) -->
<!-- Cuento anterior:    <p class="error">Al guion le faltan el lugar y el tono de la portada</p> -->
```
