<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onClose" @keydown.esc="onClose">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Crear cuento">
          <header class="modal-header">
            <h2 class="title">Crear cuento</h2>
            <button type="button" class="icon-button" aria-label="Cerrar" title="Cerrar" @click="onClose">
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

            <div class="field">
              <label for="topic-select">Tema</label>
              <div class="topic-row">
                <select id="topic-select" v-model.number="selectedTopicId" :disabled="loadingTopics">
                  <option :value="null" disabled>{{ loadingTopics ? 'Cargando temas...' : 'Selecciona un tema' }}</option>
                  <option v-for="topic in topics" :key="topic.id" :value="topic.id">
                    {{ topic.type }} - {{ topic.description }}
                  </option>
                </select>
                <button
                  type="button"
                  class="btn"
                  :disabled="!topicAgentActive || generatingTopic"
                  :title="topicAgentActive ? '' : 'El agente de temas no está disponible'"
                  @click="onGenerateTopic"
                >
                  {{ generatingTopic ? 'Generando...' : 'Generar tema' }}
                </button>
              </div>
            </div>

            <div class="field">
              <span class="label">Tamaño del cuento</span>
              <div class="size-options">
                <label v-for="option in sizeOptions" :key="option.value" class="size-option">
                  <input v-model="selectedSize" type="radio" name="story-size" :value="option.value" />
                  {{ option.label }}
                </label>
              </div>
            </div>

            <button
              type="button"
              class="btn btn-primary"
              :disabled="!selectedTopicId || !selectedSize || !directorAgentActive || creatingStory"
              :title="directorAgentActive ? '' : 'El agente director no está disponible'"
              @click="onCreateStory"
            >
              {{ creatingStory ? 'Creando...' : 'Crear cuento' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { generateStory, generateTopic, type StorySize } from '../api/generations'
import { getTopics, type Topic } from '../api/topics'
import { useServerStatusStore } from '../stores/serverStatus'

interface Props {
  open: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; created: [storyId: number] }>()

const serverStatus = useServerStatusStore()
const topicAgentActive = computed(() => serverStatus.data?.ollama.topic ?? false)
const directorAgentActive = computed(() => serverStatus.data?.ollama.director ?? false)

const sizeOptions: { value: StorySize; label: string }[] = [
  { value: 'S', label: 'S - 8' },
  { value: 'M', label: 'M - 16' },
  { value: 'L', label: 'L - 32' },
]

const topics = ref<Topic[]>([])
const selectedTopicId = ref<number | null>(null)
const selectedSize = ref<StorySize | null>(null)
const loadingTopics = ref(false)
const generatingTopic = ref(false)
const creatingStory = ref(false)
const error = ref<string | null>(null)

async function loadTopics() {
  loadingTopics.value = true
  error.value = null

  try {
    topics.value = await getTopics()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    loadingTopics.value = false
  }
}

async function onGenerateTopic() {
  generatingTopic.value = true
  error.value = null

  try {
    const topic = await generateTopic()
    topics.value = [...topics.value, topic]
    selectedTopicId.value = topic.id
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    generatingTopic.value = false
  }
}

async function onCreateStory() {
  if (!selectedTopicId.value || !selectedSize.value) return

  creatingStory.value = true
  error.value = null

  try {
    const story = await generateStory(selectedTopicId.value, selectedSize.value)
    emit('created', story.id)
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    creatingStory.value = false
  }
}

function resetState() {
  selectedTopicId.value = null
  selectedSize.value = null
  error.value = null
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      resetState()
      loadTopics()
    }
  },
)

function onClose() {
  emit('close')
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
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
  max-width: 480px;
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

.icon-button:hover {
  background: rgba(120, 120, 120, 0.12);
}

.body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field label,
.field .label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-h);
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

.size-options {
  display: flex;
  gap: 16px;
}

.size-option {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-h);
}

.btn {
  padding: 8px 16px;
  font: inherit;
  color: var(--text-h);
  white-space: nowrap;
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

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
