<template>
  <p v-if="loading">Cargando guion...</p>
  <p v-else-if="loadError" class="error">{{ loadError }}</p>
  <div v-else-if="script">
    <p v-if="script.scriptVersion === null">Este cuento no tiene escaleta.</p>
    <template v-else>
      <h3 class="title">{{ script.title }}</h3>
      <span class="badge">{{ script.status === 'SCRIPT_ACCEPTED' ? 'Guion aprobado' : 'Pendiente de aprobación' }}</span>
      <p class="synopsis">{{ script.synopsis }}</p>

      <section>
        <h4>Reparto</h4>
        <p v-if="script.cast.length === 0">Sin personajes secundarios.</p>
        <ul v-else>
          <li v-for="member in script.cast" :key="member.name">{{ member.name }} - {{ member.role }}</li>
        </ul>
      </section>

      <section>
        <h4>Escaleta</h4>
        <ol class="beats">
          <li v-for="beat in sortedBeats" :key="beat.page" class="beat">
            <template v-if="editingPage === beat.page">
              <label>Qué ocurre <textarea v-model="form.event" rows="2" /></label>
              <label>Función narrativa <input v-model="form.narrativeFunction" /></label>
              <label>Quiénes aparecen (separados por comas) <input v-model="form.participants" /></label>
              <label>Emoción o tono <input v-model="form.tone" /></label>
              <label>Nota visual <textarea v-model="form.visualNote" rows="2" /></label>
              <p v-if="editError" class="error">{{ editError }}</p>
              <div class="row">
                <button type="button" class="btn" :disabled="busy" @click="saveBeat(beat.page)">Guardar</button>
                <button type="button" class="btn" :disabled="busy" @click="cancelEdit">Cancelar</button>
              </div>
            </template>
            <template v-else>
              <BeatFields :page="beat.page" :beat="beat" />
              <button type="button" class="btn" :disabled="busy" @click="startEdit(beat)">Editar</button>
            </template>
          </li>
        </ol>
      </section>

      <p v-if="actionError" class="error">{{ actionError }}</p>

      <div class="row">
        <button
          v-if="script.status === 'SCRIPT_PENDING'"
          type="button"
          class="btn"
          :disabled="busy"
          @click="approve"
        >
          Aprobar guion
        </button>
      </div>

      <section>
        <h4>Hint global</h4>
        <textarea v-model="hint" rows="2" placeholder="Qué quieres cambiar del guion" />
        <button type="button" class="btn" :disabled="busy || !hint.trim()" @click="sendHint">Enviar hint</button>
        <p v-if="hintRejection" class="error">Hint rechazado: {{ hintRejection }}</p>
      </section>

      <section v-if="review && review.candidateOutline">
        <h4>Propuesta candidata</h4>
        <p v-if="review.changedBeats.length === 0">Sin cambios respecto a la versión anterior.</p>
        <ol class="beats">
          <li
            v-for="(beat, index) in review.candidateOutline.beats"
            :key="index"
            class="beat"
            :class="{ changed: review.changedBeats.includes(index + 1) }"
          >
            <BeatFields :page="index + 1" :beat="beat" />
            <span v-if="review.changedBeats.includes(index + 1)" class="badge">Cambiado</span>
          </li>
        </ol>
        <div class="row">
          <button type="button" class="btn" :disabled="busy" @click="resolveReview('CONFIRMED')">Aceptar propuesta</button>
          <button type="button" class="btn" :disabled="busy" @click="resolveReview('DISCARDED')">Descartar</button>
        </div>
      </section>
    </template>
  </div>

  <ConfirmModal
    :open="invalidationPages !== null"
    title="Páginas afectadas"
    :message="`Editar este beat eliminará ${invalidationPages} página(s) ya generadas. ¿Continuar?`"
    confirm-label="Editar y eliminar páginas"
    danger
    @confirm="resolveInvalidation(true)"
    @cancel="resolveInvalidation(false)"
  />
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, ref, type PropType } from 'vue'
import ConfirmModal from './ConfirmModal.vue'
import { ApiError } from '../api/httpClient'
import {
  approveStoryScript,
  editStoryBeat,
  getStoryScript,
  type ScriptBeat,
  type StoryBeatFields,
  type StoryScript,
} from '../api/storyScript'
import {
  confirmStoryReview,
  getPendingStoryReview,
  reviewStory,
  type StoryReview,
} from '../api/reviews'

interface Props {
  storyId: number
}
const props = defineProps<Props>()
// Avisa al modal de que el estado persistido del guion cambió (aprobación, revisión confirmada o edición aplicada).
const emit = defineEmits<{ changed: [] }>()

// Presentación de los cinco campos de un beat (solo lectura).
const BeatFields = defineComponent({
  props: {
    page: { type: Number, required: true },
    beat: { type: Object as PropType<StoryBeatFields>, required: true },
  },
  setup(p) {
    return () =>
      h('dl', { class: 'beat-fields' }, [
        h('dt', `Página ${p.page}`),
        h('dd', p.beat.event),
        h('dd', `Función: ${p.beat.narrativeFunction}`),
        h('dd', `Aparecen: ${p.beat.participants.join(', ')}`),
        h('dd', `Tono: ${p.beat.tone}`),
        h('dd', `Nota visual: ${p.beat.visualNote}`),
      ])
  },
})

const loading = ref(false)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busy = ref(false)
const script = ref<StoryScript | null>(null)
const review = ref<StoryReview | null>(null)
const hint = ref('')
const hintRejection = ref<string | null>(null)

const editingPage = ref<number | null>(null)
const editError = ref<string | null>(null)
const form = reactive({ event: '', narrativeFunction: '', participants: '', tone: '', visualNote: '' })
// Páginas que el servidor avisa que se eliminarán; null = sin aviso abierto.
const invalidationPages = ref<number | null>(null)

const sortedBeats = computed(() => [...(script.value?.beats ?? [])].sort((a, b) => a.page - b.page))

function messageOf(err: unknown) {
  return err instanceof ApiError ? err.message : 'Error inesperado'
}

// Fuente de verdad: guion y revisión pendiente siempre desde REST.
async function load() {
  try {
    const [loadedScript, loadedReview] = await Promise.all([
      getStoryScript(props.storyId),
      getPendingStoryReview(props.storyId),
    ])
    script.value = loadedScript
    review.value = loadedReview
    loadError.value = null
  } catch (err) {
    loadError.value = messageOf(err)
  }
}

async function initialLoad() {
  loading.value = true
  await load()
  loading.value = false
}

// Ejecuta una mutación y recarga el guion; los rechazos del servidor se muestran sin cambiar la vista.
async function mutate(action: () => Promise<unknown>, notify = false) {
  busy.value = true
  actionError.value = null
  try {
    await action()
    await load()
    if (notify) emit('changed')
  } catch (err) {
    actionError.value = messageOf(err)
  } finally {
    busy.value = false
  }
}

function approve() {
  return mutate(() => approveStoryScript(props.storyId), true)
}

function sendHint() {
  return mutate(async () => {
    const result = await reviewStory(props.storyId, hint.value.trim())
    hintRejection.value = result.hintAccepted === false ? (result.rejectedReason ?? 'Sin motivo') : null
    if (result.hintAccepted !== false) hint.value = ''
  })
}

function resolveReview(status: 'CONFIRMED' | 'DISCARDED') {
  return mutate(() => confirmStoryReview(props.storyId, status), status === 'CONFIRMED')
}

function startEdit(beat: ScriptBeat) {
  editingPage.value = beat.page
  editError.value = null
  form.event = beat.event
  form.narrativeFunction = beat.narrativeFunction
  form.participants = beat.participants.join(', ')
  form.tone = beat.tone
  form.visualNote = beat.visualNote
}

function cancelEdit() {
  editingPage.value = null
  editError.value = null
}

function formBeat(): StoryBeatFields {
  return {
    event: form.event.trim(),
    narrativeFunction: form.narrativeFunction.trim(),
    participants: form.participants.split(',').map((p) => p.trim()).filter(Boolean),
    tone: form.tone.trim(),
    visualNote: form.visualNote.trim(),
  }
}

// Aplica el resultado del servidor; devuelve true si la edición quedó aplicada.
async function handleEditResult(result: Awaited<ReturnType<typeof editStoryBeat>>) {
  if (result.status === 'REJECTED') {
    editError.value = result.reason ?? 'La edición no cumple las reglas de edad.'
  } else if (result.status === 'CONFIRMATION_REQUIRED') {
    invalidationPages.value = result.invalidatedPages
  } else if (result.status === 'APPLIED') {
    cancelEdit()
    await load()
    emit('changed')
  }
}

async function saveBeat(page: number) {
  busy.value = true
  editError.value = null
  try {
    await handleEditResult(await editStoryBeat(props.storyId, page, formBeat()))
  } catch (err) {
    editError.value = messageOf(err)
  } finally {
    busy.value = false
  }
}

// Segunda petición: true confirma y aplica, false cancela sin efectos (el guion local no cambia).
async function resolveInvalidation(confirm: boolean) {
  const page = editingPage.value
  invalidationPages.value = null
  if (page === null) return
  busy.value = true
  try {
    await handleEditResult(await editStoryBeat(props.storyId, page, formBeat(), confirm))
  } catch (err) {
    editError.value = messageOf(err)
  } finally {
    busy.value = false
  }
}

initialLoad()
</script>

<style scoped>
.error {
  margin: 8px 0 0;
  color: #e5484d;
}

.title {
  margin: 0 0 4px;
  color: var(--text-h);
}

.synopsis {
  color: var(--text-h);
}

.beats {
  padding-left: 20px;
}

.beat {
  margin-bottom: 12px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.beat.changed {
  background: #fef3c7;
  border-color: #f59e0b;
}

.beat label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
}

.beat input,
.beat textarea,
section > textarea {
  display: block;
  width: 100%;
  font: inherit;
}

.beat-fields {
  margin: 0 0 8px;
}

.beat-fields dt {
  font-weight: 600;
  color: var(--text-h);
}

.beat-fields dd {
  margin: 2px 0 0;
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
