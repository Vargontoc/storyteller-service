<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onClose" @keydown.esc="onClose">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Revisar guión">
          <header class="modal-header">
            <h2 class="title">Revisar guión</h2>
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

            <div class="field">
              <label for="review-hint">Comentario</label>
              <textarea
                id="review-hint"
                v-model="hint"
                rows="4"
                placeholder="Indica qué te gustaría cambiar del guión..."
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
import { ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { reviewStory, type StoryReview } from '../api/reviews'

interface Props {
  open: boolean
  storyId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; accepted: [review: StoryReview] }>()

const hint = ref('')
const submitting = ref(false)
const rejectedReason = ref<string | null>(null)
const error = ref<string | null>(null)

async function onSubmit() {
  if (!props.storyId || !hint.value.trim()) return

  submitting.value = true
  error.value = null
  rejectedReason.value = null

  try {
    const review = await reviewStory(props.storyId, hint.value.trim())

    if (review.rejectedReason) {
      rejectedReason.value = review.rejectedReason
    } else {
      emit('accepted', review)
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
</style>
