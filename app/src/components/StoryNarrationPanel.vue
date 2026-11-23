<template>
  <p v-if="loading">Cargando narración...</p>
  <p v-else-if="loadError" class="error">{{ loadError }}</p>
  <div v-else-if="state" class="narration">
    <h4>Narración</h4>

    <p v-if="state.narrating" class="info narrating">Narrando el cuento...</p>
    <p v-else-if="state.hasAudio && state.voiceName" class="info narrated">Narrado con {{ state.voiceName }}</p>
    <p v-if="!state.narrating && state.lastResult === 'ERROR'" class="error result-error">
      La narración no se completó. Puedes volver a intentarlo.
    </p>

    <p v-if="actionError" class="error action-error">{{ actionError }}</p>
    <p v-if="!state.canNarrate && disabledText" class="info disabled-reason">{{ disabledText }}</p>

    <div class="row">
      <button type="button" class="btn narrate" :disabled="!state.canNarrate || state.narrating || busy" @click="openChooser">
        {{ state.hasAudio ? 'Renarrar' : 'Narrar' }}
      </button>
    </div>

    <div v-if="choosing" class="chooser">
      <p v-if="!voices.length" class="warning no-voices">
        No hay voces en español disponibles. Sube una en la sección Voces.
      </p>
      <template v-else>
        <label v-for="voice in voices" :key="voiceKey(voice)" class="voice">
          <input v-model="selected" type="radio" name="narration-voice" :value="voiceKey(voice)" />
          {{ voice.name }}<span v-if="voice.system" class="badge">Voz del sistema</span>
        </label>
        <div class="row">
          <button type="button" class="btn start" :disabled="selected === null || busy" @click="submit">Iniciar narración</button>
          <button type="button" class="btn" @click="choosing = false">Cancelar</button>
        </div>
      </template>
    </div>

    <ConfirmModal
      :open="confirming"
      title="Renarrar el cuento"
      message="Se sustituirá el audio actual del cuento por la nueva narración. ¿Continuar?"
      confirm-label="Renarrar"
      danger
      @confirm="confirmRenarrate"
      @cancel="confirming = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { useWebsocketStore } from '../stores/websocket'
import ConfirmModal from './ConfirmModal.vue'
import { getNarrationState, startNarration, type NarrationDisabledReason, type NarrationState } from '../api/storyNarration'
import { listEligibleVoices, type Voice } from '../api/voices'

interface Props {
  storyId: number
}
const props = defineProps<Props>()

const loading = ref(false)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busy = ref(false)
const state = ref<NarrationState | null>(null)

// Selector de voz: solo elegibles; la voz del sistema se preselecciona si está disponible.
const choosing = ref(false)
const voices = ref<Voice[]>([])
const selected = ref<string | null>(null)
const confirming = ref(false)

const disabledTexts: Record<NarrationDisabledReason, string> = {
  PAGES_NOT_ACCEPTED: 'Faltan páginas interiores por aceptar para poder narrar.',
  IN_PROGRESS: 'Ya hay una narración en curso.',
  NO_OUTLINE: 'El cuento no tiene escaleta, no se puede narrar.',
}

const rejectionTexts: Record<string, string> = {
  GENERATION_IN_PROGRESS: 'Hay otra generación en curso. Inténtalo de nuevo cuando termine.',
  NO_VOICES_AVAILABLE: 'No hay voces en español disponibles.',
  VOICE_NOT_AVAILABLE: 'La voz elegida ya no está disponible.',
  PAGES_NOT_ACCEPTED: disabledTexts.PAGES_NOT_ACCEPTED,
  IN_PROGRESS: 'El cuento ya se está narrando.',
  NO_OUTLINE: disabledTexts.NO_OUTLINE,
}

const disabledText = computed(() => (state.value?.disabledReason ? disabledTexts[state.value.disabledReason] : null))

function voiceKey(voice: Voice) {
  return voice.system ? 'system' : String(voice.id)
}

function messageOf(err: unknown) {
  if (err instanceof ApiError) return (err.code && rejectionTexts[err.code]) || err.message
  return 'Error inesperado'
}

// Fuente de verdad: el estado siempre viene de REST.
async function load() {
  try {
    state.value = await getNarrationState(props.storyId)
    loadError.value = null
  } catch (err) {
    loadError.value = messageOf(err)
  }
}

async function openChooser() {
  actionError.value = null
  try {
    voices.value = await listEligibleVoices()
  } catch (err) {
    actionError.value = messageOf(err)
    return
  }
  selected.value = voices.value.some((v) => v.system) ? 'system' : null
  choosing.value = true
}

function submit() {
  if (selected.value === null) return
  if (state.value?.hasAudio) {
    confirming.value = true
    return
  }
  return start()
}

function confirmRenarrate() {
  confirming.value = false
  return start()
}

// Asíncrono (202): solo se muestra el curso devuelto; el resultado llega por WebSocket.
async function start() {
  if (selected.value === null) return
  busy.value = true
  actionError.value = null
  try {
    state.value = await startNarration(props.storyId, selected.value === 'system' ? 'system' : Number(selected.value))
    choosing.value = false
  } catch (err) {
    actionError.value = messageOf(err)
    choosing.value = false
    await load()
  } finally {
    busy.value = false
  }
}

// Eventos WebSocket: solo señales de recarga del cuento abierto.
const websocketStore = useWebsocketStore()

watch(
  () => websocketStore.lastNarrationEvent?.id,
  async () => {
    const event = websocketStore.lastNarrationEvent
    if (!event || event.storyId !== props.storyId) return
    actionError.value = null
    await load()
  },
)

// Reconexión (reinicio del servidor o eventos perdidos): recarga el estado.
watch(
  () => websocketStore.connectionId,
  () => load(),
)

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

.warning {
  color: #b45309;
}

.chooser {
  margin-top: 8px;
}

.voice {
  display: block;
  margin: 4px 0;
}

.row {
  display: flex;
  gap: 12px;
}

.badge {
  margin-left: 8px;
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
