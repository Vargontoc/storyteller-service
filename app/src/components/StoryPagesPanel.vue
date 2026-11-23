<template>
  <p v-if="loading">Cargando páginas...</p>
  <p v-else-if="loadError" class="error">{{ loadError }}</p>
  <div v-else-if="view">
    <span class="badge">{{ statusLabel }}</span>
    <p v-if="view.generating" class="info">Generación en curso...</p>
    <p v-else-if="operation !== null" class="info operation">
      Generando {{ operation === 0 ? 'la portada' : `la página ${operation}` }}...
    </p>
    <p v-if="view.narrating" class="info narrating">
      El cuento se está narrando: no se pueden reabrir, cambiar ni eliminar sus páginas.
    </p>
    <p v-if="actionError" class="error">{{ actionError }}</p>
    <p v-if="eventError" class="error event-error">{{ eventError }}</p>

    <div class="row">
      <button v-if="view.nextPage !== null" type="button" class="btn" :disabled="busy || view.generating" @click="generate">
        {{ view.nextPage === 0 ? 'Generar portada' : `Generar página ${view.nextPage}` }}
      </button>
      <button type="button" class="btn" :disabled="busy" @click="load">Recargar</button>
    </div>

    <section v-for="page in view.pages" :key="page.id" class="page" :data-page="page.page">
      <h4>{{ page.page === 0 ? 'Portada' : `Página ${page.page}` }}</h4>
      <span class="badge">{{ page.status === 'ACCEPTED' ? 'Aceptada' : 'Pendiente' }}</span>

      <template v-if="page.accepted">
        <img v-if="page.accepted.hasImage && images[page.id]" class="image" :src="images[page.id]" alt="Ilustración aceptada" />
        <h5 v-if="page.title" class="title">{{ page.title }}</h5>
        <p v-if="page.accepted.text" class="text">{{ page.accepted.text }}</p>
        <p class="scene">Escena: {{ page.accepted.scene }}</p>
      </template>
      <p v-else-if="!page.versions.length" class="info">Pendiente de aceptar.</p>

      <!-- Histórico (o propuestas de reapertura): sin imagen; solo la vigente se destaca. -->
      <h5 v-if="page.proposals.length" class="title">Propuestas</h5>
      <ul v-if="shownVersions(page).length" class="versions">
        <li v-for="version in shownVersions(page)" :key="version.id" class="version" :class="{ current: version.current }">
          <span v-if="version.current" class="badge">Vigente</span>
          <p v-if="version.text" class="text">{{ version.text }}</p>
          <p class="scene">Escena: {{ version.scene }}</p>
          <p v-if="version.warning !== 'NONE'" class="warning">{{ warningLabel(version.warning) }}</p>
          <button
            v-if="!version.current && page.actions.includes('SELECT_VERSION')"
            type="button"
            class="btn"
            :disabled="locked"
            @click="select(page.id, version.id)"
          >
            Marcar como vigente
          </button>
        </li>
      </ul>

      <!-- Audio de narración: solo interiores con audio declarado; se pide al pulsar y no se encadena. -->
      <div v-if="page.page > 0 && page.hasAudio" class="audio">
        <button type="button" class="btn" :disabled="playingPage === page.page" @click="playAudio(page.page)">
          {{ playingPage === page.page ? 'Reproduciendo audio...' : 'Reproducir audio' }}
        </button>
        <p v-if="audioError?.page === page.page" class="error audio-error">{{ audioError.message }}</p>
      </div>

      <div v-if="page.actions.includes('ACCEPT_PROPOSAL') || page.actions.includes('DISCARD_PROPOSALS')" class="row">
        <button v-if="page.actions.includes('ACCEPT_PROPOSAL')" type="button" class="btn" :disabled="locked" @click="acceptReopened(page.id)">
          Aceptar propuesta
        </button>
        <button v-if="page.actions.includes('DISCARD_PROPOSALS')" type="button" class="btn" :disabled="busy" @click="discard(page.id)">
          Descartar propuestas
        </button>
      </div>

      <div v-if="page.actions.includes('ACCEPT') || page.actions.includes('REGENERATE')" class="row">
        <button v-if="page.actions.includes('ACCEPT')" type="button" class="btn" :disabled="busy" @click="accept(page.id)">Aceptar</button>
        <button v-if="page.actions.includes('REGENERATE')" type="button" class="btn" :disabled="locked" @click="regeneratePage(page.id)">
          Regenerar sin hint
        </button>
      </div>

      <div v-if="page.actions.includes('HINT')">
        <textarea v-model="hints[page.id]" rows="2" placeholder="Qué quieres cambiar" />
        <button type="button" class="btn" :disabled="locked" @click="hint(page.id)">Enviar hint</button>
        <p v-if="hintErrors[page.id]" class="error">{{ hintErrors[page.id] }}</p>
      </div>
    </section>

    <ConfirmModal
      :open="cascade !== null"
      title="Aceptar propuesta"
      :message="cascadeMessage"
      confirm-label="Aceptar y eliminar"
      danger
      @confirm="confirmCascade"
      @cancel="cascade = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { useWebsocketStore } from '../stores/websocket'
import ConfirmModal from './ConfirmModal.vue'
import { getPageAudio } from '../api/storyNarration'
import {
  acceptPage,
  acceptProposal,
  discardProposals,
  generateCover,
  generatePage,
  getAcceptedImage,
  getStoryPagesView,
  regenerate,
  selectVersion,
  sendHint,
  type PageDetail,
  type StoryPagesView,
  type VersionWarning,
} from '../api/storyPages'

interface Props {
  storyId: number
}
const props = defineProps<Props>()

const loading = ref(false)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busy = ref(false)
const view = ref<StoryPagesView | null>(null)
// Texto del hint y su error por id de página; URL de la imagen aceptada por id de página.
const hints = reactive<Record<number, string>>({})
const hintErrors = reactive<Record<number, string>>({})
const images = reactive<Record<number, string>>({})

// Aceptación pendiente de confirmar: páginas posteriores que se eliminarían (dato del servidor).
const cascade = ref<{ pageId: number; affectedPages: number } | null>(null)
const cascadeMessage = computed(() => {
  const n = cascade.value?.affectedPages ?? 0
  const audioNotice = view.value?.hasAudio ? ' Se borrará todo el audio de la narración y la voz.' : ''
  return `Se eliminará${n === 1 ? '' : 'n'} ${n} página${n === 1 ? '' : 's'} posterior${n === 1 ? '' : 'es'}.${audioNotice} ¿Continuar?`
})

// Mientras se narra, el servidor rechaza reabrir, regenerar, cambiar versión y aceptar propuesta.
const locked = computed(() => busy.value || view.value?.narrating === true)

// Tras una mutación completada el padre puede recargar otros paneles (p. ej. el de narración, que no recibe evento al borrarse el audio).
const emit = defineEmits<{ changed: [] }>()

const statusLabel = computed(() => {
  if (view.value?.status === 'PAGES_ACCEPTED') return 'Cuento completo'
  return 'Páginas en curso'
})

function messageOf(err: unknown) {
  return err instanceof ApiError ? err.message : 'Error inesperado'
}

function releaseImages() {
  for (const id of Object.keys(images)) {
    URL.revokeObjectURL(images[Number(id)])
    delete images[Number(id)]
  }
}

// Solo el contenido aceptado que declara imagen la solicita; si falla, se muestra sin imagen.
async function loadImages(loaded: StoryPagesView) {
  releaseImages()
  for (const page of loaded.pages) {
    if (!page.accepted?.hasImage) continue
    try {
      images[page.id] = URL.createObjectURL(await getAcceptedImage(props.storyId, page.id))
    } catch {
      // sin imagen
    }
  }
}

// Audio de la página en reproducción (una sola a la vez); la URL temporal se libera al terminar, cambiar o desmontar.
const playingPage = ref<number | null>(null)
const audioError = ref<{ page: number; message: string } | null>(null)
let audio: HTMLAudioElement | null = null
let audioUrl: string | null = null

function stopAudio() {
  if (audio) {
    audio.pause()
    audio = null
  }
  if (audioUrl) {
    URL.revokeObjectURL(audioUrl)
    audioUrl = null
  }
  playingPage.value = null
}

async function playAudio(pageNumber: number) {
  stopAudio()
  audioError.value = null
  try {
    const url = URL.createObjectURL(await getPageAudio(props.storyId, pageNumber))
    audioUrl = url
    // 'ended' solo detiene esta página: nunca avanza ni reproduce otra.
    const player = new Audio(url)
    audio = player
    player.addEventListener('ended', stopAudio)
    player.addEventListener('error', stopAudio)
    playingPage.value = pageNumber
    await player.play()
  } catch (err) {
    stopAudio()
    audioError.value = { page: pageNumber, message: err instanceof ApiError ? err.message : 'No se pudo reproducir el audio' }
  }
}

// Fuente de verdad: el estado siempre viene de REST.
async function load() {
  stopAudio()
  try {
    const loaded = await getStoryPagesView(props.storyId)
    view.value = loaded
    loadError.value = null
    await loadImages(loaded)
  } catch (err) {
    loadError.value = messageOf(err)
  }
}

// Ejecuta una mutación y recarga; el rechazo del servidor se muestra con su motivo.
async function mutate(action: () => Promise<unknown>) {
  busy.value = true
  actionError.value = null
  try {
    await action()
    await load()
    emit('changed')
  } catch (err) {
    actionError.value = messageOf(err)
    // Carrera: la narración empezó tras cargar la vista; se muestra el motivo y se relee el estado real.
    if (err instanceof ApiError && err.status === 409 && err.code === 'IN_PROGRESS') await load()
  } finally {
    busy.value = false
  }
}

function generate() {
  const next = view.value?.nextPage
  if (next === null || next === undefined) return
  // Asíncrono (202): el resultado llega por WebSocket y recarga el panel.
  return mutate(() => (next === 0 ? generateCover(props.storyId) : generatePage(props.storyId, next)))
}

const warningLabels: Record<VersionWarning, string> = {
  NONE: '',
  TOO_MANY_WORDS: 'Aviso: demasiadas palabras',
  SENTENCE_COUNT: 'Aviso: número de frases fuera de lo esperado',
  BOTH: 'Aviso: demasiadas palabras y número de frases fuera de lo esperado',
}

function warningLabel(warning: VersionWarning) {
  return warningLabels[warning]
}

function select(pageId: number, versionId: number) {
  return mutate(() => selectVersion(props.storyId, pageId, versionId))
}

// Asíncrono (202): el resultado llega por WebSocket y recarga el panel.
function regeneratePage(pageId: number) {
  return mutate(() => regenerate(props.storyId, pageId))
}

function accept(pageId: number) {
  return mutate(() => acceptPage(props.storyId, pageId))
}

// Reapertura: las propuestas sustituyen al histórico; la versión aceptada nunca se ofrece como vigente.
function shownVersions(page: PageDetail) {
  return page.proposals.length ? page.proposals : page.versions
}

// Aceptar propuesta: la primera llamada no cambia nada si hay páginas posteriores y pide confirmación.
function acceptReopened(pageId: number) {
  return mutate(async () => {
    const result = await acceptProposal(props.storyId, pageId, false)
    if (result.requiresConfirmation) cascade.value = { pageId, affectedPages: result.affectedPages }
  })
}

function confirmCascade() {
  const pending = cascade.value
  cascade.value = null
  if (!pending) return
  return mutate(() => acceptProposal(props.storyId, pending.pageId, true))
}

function discard(pageId: number) {
  return mutate(() => discardProposals(props.storyId, pageId))
}

function hint(pageId: number) {
  const text = (hints[pageId] ?? '').trim()
  if (!text) {
    hintErrors[pageId] = 'El hint es obligatorio'
    return
  }
  delete hintErrors[pageId]
  return mutate(async () => {
    await sendHint(props.storyId, pageId, text)
    hints[pageId] = ''
  })
}

// Eventos WebSocket: solo señales de sincronización; el estado siempre se relee de REST.
const websocketStore = useWebsocketStore()
// Página (0 = portada) cuya generación anunció STARTED; indicador local, no sustituye a view.generating.
const operation = ref<number | null>(null)
// Motivo de un rechazo de hint o fallo recibido por evento.
const eventError = ref<string | null>(null)

watch(
  () => websocketStore.lastStoryPageEvent?.id,
  async () => {
    const event = websocketStore.lastStoryPageEvent
    if (!event || event.storyId !== props.storyId) return
    if (event.type === 'STARTED') {
      operation.value = event.pageNumber
      eventError.value = null
      return
    }
    operation.value = null
    eventError.value = event.type === 'VERSION_PRESENTED' ? null : (event.reason ?? 'La generación no se completó')
    // Recargar libera la interacción (generating) y refleja el resultado real.
    await load()
  },
)

// Narración completada del cuento abierto: recarga para que aparezca el reproductor (sin audio optimista).
watch(
  () => websocketStore.lastNarrationEvent?.id,
  async () => {
    const event = websocketStore.lastNarrationEvent
    if (!event || event.storyId !== props.storyId || event.type !== 'COMPLETED') return
    await load()
  },
)

// Reconexión (reinicio del servidor o eventos perdidos): recarga el panel completo.
watch(
  () => websocketStore.connectionId,
  async () => {
    operation.value = null
    await load()
  },
)

onBeforeUnmount(() => {
  stopAudio()
  releaseImages()
})

async function initialLoad() {
  loading.value = true
  await load()
  loading.value = false
}

initialLoad()
</script>

<style scoped>
.error {
  margin: 8px 0 0;
  color: #e5484d;
}

.info {
  color: var(--text-h);
}

.page {
  margin-bottom: 12px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.image {
  display: block;
  max-width: 100%;
  margin: 8px 0;
  border-radius: 6px;
}

.title {
  margin: 8px 0 4px;
  color: var(--text-h);
}

.scene {
  font-style: italic;
}

.versions {
  padding: 0;
  list-style: none;
}

.version {
  margin: 8px 0;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.version.current {
  border-width: 2px;
}

.warning {
  color: #b45309;
}

textarea {
  display: block;
  width: 100%;
  font: inherit;
}

.row {
  display: flex;
  gap: 12px;
}

.badge {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
  background: #fef3c7;
  border-radius: 999px;
}

.btn {
  margin-top: 8px;
  padding: 8px 16px;
  font: inherit;
  color: var(--text-h);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.btn:hover:not(:disabled) {
  background: rgba(120, 120, 120, 0.12);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
