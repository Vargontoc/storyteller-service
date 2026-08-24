<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onClose" @keydown.esc="onClose">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Revisar guión">
          <header class="modal-header">
            <h2 class="title">{{ title }}</h2>
            <button
              type="button"
              class="icon-button"
              aria-label="Cerrar"
              title="Cerrar"
              :disabled="submitting"
              @click="onClose"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M6.7 5.3a1 1 0 0 0-1.4 1.4L10.6 12l-5.3 5.3a1 1 0 1 0 1.4 1.4L12 13.4l5.3 5.3a1 1 0 0 0 1.4-1.4L13.4 12l5.3-5.3a1 1 0 0 0-1.4-1.4L12 10.6 6.7 5.3Z"
                />
              </svg>
            </button>
          </header>

          <div class="body">
            <p v-if="error" class="error">{{ error }}</p>
            <p v-if="rejectedReason" class="rejection">
              <strong>El agente ha rechazado la revisión:</strong> {{ rejectedReason }}
            </p>

            <div v-if="typeReview == 'ACTOR'" class="field">
              <label for="topic-select">Campo para modificar:</label>
              <div class="topic-row">
                <select id="topic-select" v-model="actorTargetSelected" >
                  <option :value="null" disabled>{{ 'Selecciona un objetivo de modificación...' }}</option>
                  <option v-for="t in actorTarget" :key="t" :value="t">
                    {{ getActorTargetText(t) }}
                  </option>
                </select>
              </div>
            </div>

            <div v-if="typeReview == 'PAGE'" class="field">
              <label for="topic-select">Campo para modificar:</label>
              <div class="topic-row">
                <select id="topic-select" v-model="pageTargetSelected" >
                  <option :value="null" disabled>{{ 'Selecciona un objetivo de modificación...' }}</option>
                  <option v-for="t in pageTarget" :key="t" :value="t">
                    {{ getPageTargetText(t) }}
                  </option>
                </select>
              </div>
            </div>

            <div class="field">
              <label for="review-hint">Comentario</label>
              <textarea
                id="review-hint"
                v-model="hint"
                rows="4"
                :placeholder="hintPlaceholder"
                :disabled="submitting"
              ></textarea>
            </div>

            <div class="actions">
              <button type="button" class="btn" :disabled="submitting" @click="onClose">Cancelar</button>
              <button type="button" class="btn btn-primary" :disabled="!hint.trim() || submitting" @click="onSubmit">
                {{ submitting ? 'Enviando...' : 'Aceptar' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { reviewActor, reviewPage, reviewStory, type ActorReview, type ReviewPageTarget, type ReviewType, type StoryReview } from '../api/reviews'
import { type ReviewActorTarget } from '../api/reviews'

interface Props {
  open: boolean
  storyId: number | null
  id: number | null
  typeReview: ReviewType
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; accepted: [review: StoryReview | ActorReview] }>()

const hint = ref('')
const submitting = ref(false)
const rejectedReason = ref<string | null>(null)
const error = ref<string | null>(null)
const actorTarget = ref<ReviewActorTarget[]>(['VISUAL', 'ROLE', 'BOTH'])
const pageTarget = ref<ReviewPageTarget[]>(['TEXT', 'SCENE', 'BOTH'])
const actorTargetSelected = ref<ReviewActorTarget | null>(null)
const pageTargetSelected = ref<ReviewPageTarget | null>(null) 

const title = computed(() => {
  switch(props.typeReview){
    case 'ACTOR': return 'Revisar personaje'
    case 'SCRIPT': return 'Revisar guión'
    case 'COVER': return 'Revisar portada'
    case 'PAGE': return 'Revisar página'
  }
})

const hintPlaceholder = computed(() => {
  let phrase = "Indica qué te gustaría cambiar ";
    switch(props.typeReview){
    case 'ACTOR': phrase += 'del personaje..'; break;
    case 'SCRIPT': phrase += 'del guión...'; break;
    case 'COVER': phrase += 'de la portada...'; break;
    case 'PAGE': phrase += 'de la página...'; break;
  }
  return phrase;
})

function getActorTargetText(target: ReviewActorTarget){
  switch(target) {
    case 'VISUAL' : return 'Modificar visual';
    case 'ROLE' : return 'Modificar rol';
    case 'BOTH' : return 'Modificar rol y descripción'
  }
}

function getPageTargetText(target: ReviewPageTarget){
    switch(target) {
    case 'TEXT' : return 'Modificar texto';
    case 'SCENE' : return 'Modificar escena';
    case 'BOTH' : return 'Modificar texto y escena'
  }
}

async function onSubmit() {
    if(!props.storyId || !hint.value.trim()) return

    submitting.value = true
    error.value = null
    rejectedReason.value = null

    try {

      switch(props.typeReview) {
        case 'SCRIPT':
          const review = await reviewStory(props.storyId, hint.value.trim())
    
          if (review.hintAccepted === false && review.rejectedReason) {
            rejectedReason.value = review.rejectedReason
          } else {
            emit('accepted', review)
          }
          break;
        case 'COVER':
          if(!props.id) return;
          const coverReview = await reviewPage(props.storyId, props.id, 'SCENE', hint.value.trim())
    
          if (coverReview.hintAccepted === false && coverReview.rejectedReason) {
            rejectedReason.value = coverReview.rejectedReason
          } else {
            emit('accepted', coverReview)
          }
          break;
          
          break;
        case 'ACTOR':
          if(!props.id || actorTargetSelected.value == null) return;

          const actorReview = await reviewActor(props.storyId, props.id, actorTargetSelected.value, hint.value.trim())
          if (actorReview.hintAccepted === false && actorReview.rejectedReason) {
            rejectedReason.value = actorReview.rejectedReason
          } else {
            emit('accepted', actorReview)
          }
          break;
        case 'PAGE':
          if(!props.id || pageTargetSelected.value == null) return;

          const pageReview = await reviewPage(props.storyId, props.id, pageTargetSelected.value, hint.value.trim())
          if (pageReview.hintAccepted === false && pageReview.rejectedReason) {
            rejectedReason.value = pageReview.rejectedReason
          } else {
            emit('accepted', pageReview)
          }
          break;
          break;
      }
    } catch (err) {
      error.value = err instanceof ApiError ? err.message : 'Unexpected error'
    } finally {
      submitting.value = false
    }
}


watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      hint.value = ''
      rejectedReason.value = null
      error.value = null
    }
  },
)

function onClose() {
  if (submitting.value) return
  emit('close')
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 110;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.45);
}

.modal {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 440px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 20px 0;
}

.title {
  margin: 0;
  font-size: 18px;
  color: var(--text-h);
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  color: var(--text-h);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.icon-button:hover:not(:disabled) {
  background: rgba(120, 120, 120, 0.12);
}

.icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-h);
}

textarea {
  padding: 8px;
  font: inherit;
  color: var(--text-h);
  resize: vertical;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
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

.btn-primary {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.error {
  margin: 0;
  color: #e5484d;
}

.rejection {
  margin: 0;
  padding: 10px 12px;
  color: #92400e;
  background: #fef3c7;
  border-radius: 6px;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.topic-row {
  display: flex;
  gap: 8px;
}

.topic-row select {
  flex: 1;
  min-width: 0;
  padding: 8px;
  font: inherit;
  color: var(--text-h);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
}
</style>
