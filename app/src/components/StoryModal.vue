<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onOverlayClose" @keydown.esc="onOverlayClose">
        <div class="modal-row">
          <div
            class="modal"
            :class="{ 'modal-blocked': showReviewDetail }"
            :inert="showReviewDetail"
            role="dialog"
            aria-modal="true"
            :aria-label="title"
          >
            <header class="modal-header">
              <h2 class="title">{{ title }}</h2>
              <button type="button" class="icon-button" aria-label="Cerrar" title="Cerrar" @click="onClose">
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M6.7 5.3a1 1 0 0 0-1.4 1.4L10.6 12l-5.3 5.3a1 1 0 1 0 1.4 1.4L12 13.4l5.3 5.3a1 1 0 0 0 1.4-1.4L13.4 12l5.3-5.3a1 1 0 0 0-1.4-1.4L12 10.6 6.7 5.3Z"
                  />
                </svg>
              </button>
            </header>

            <nav class="tabs" role="tablist">
              <button
                v-for="tab in visibleTabs"
                :key="tab.id"
                type="button"
                role="tab"
                class="tab"
                :class="{ active: activeTab === tab.id }"
                :aria-selected="activeTab === tab.id"
                :disabled="!isUnlocked(tab.id)"
                @click="activeTab = tab.id"
              >
                {{ tab.label }}
              </button>
            </nav>

            <div class="tab-content">
              <div v-if="activeTab === 'guion'" class="guion-tab">
                <p v-if="loadingStory">Cargando cuento...</p>
                <p v-else-if="storyError" class="error">{{ storyError }}</p>

                <template v-else-if="story">
                  <button
                    v-if="pendingReview"
                    type="button"
                    class="badge badge-button"
                    @click="showReviewDetail = true"
                  >
                    Revisión pendiente
                  </button>

                  <dl class="guion-fields">
                    <div class="guion-field">
                      <dt>Tamaño</dt>
                      <dd>{{ story.size }}</dd>
                    </div>
                    <div class="guion-field">
                      <dt>Páginas generadas</dt>
                      <dd>{{ story.pages }}</dd>
                    </div>
                    <div class="guion-field">
                      <dt>Nº personajes</dt>
                      <dd>{{ story.actors }}</dd>
                    </div>
                  </dl>

                  <dl class="guion-field">
                    <dt>Sinopsis</dt>
                    <dd>{{ story.synopsis }}</dd>
                  </dl>

                  <div class="guion-actions">
                    <div class="guion-actions-left">
                      <button type="button" class="btn" @click="onGeneratePage">Generar página</button>
                      <button
                        type="button"
                        class="btn"
                        :disabled="!directorAgentActive"
                        :title="directorAgentActive ? '' : 'El agente director no está disponible'"
                        @click="showReviewModal = true"
                      >
                        Revisar guión
                      </button>
                    </div>

                    <button type="button" class="btn btn-danger" @click="emit('delete', story)">
                      Eliminar cuento
                    </button>
                  </div>
                </template>
              </div>
              <div v-else-if="activeTab === 'portada'"></div>
              <div v-else-if="activeTab === 'personajes'"></div>
              <div v-else-if="activeTab === 'paginas'"></div>
            </div>
          </div>

          <div v-if="showReviewDetail && pendingReview" class="modal review-panel" role="dialog" aria-modal="true" aria-label="Revisión pendiente">
            <header class="modal-header">
              <h2 class="title">Revisión pendiente</h2>
              <button
                type="button"
                class="icon-button"
                aria-label="Cerrar"
                title="Cerrar"
                :disabled="resolvingReview"
                @click="showReviewDetail = false"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M6.7 5.3a1 1 0 0 0-1.4 1.4L10.6 12l-5.3 5.3a1 1 0 1 0 1.4 1.4L12 13.4l5.3 5.3a1 1 0 0 0 1.4-1.4L13.4 12l5.3-5.3a1 1 0 0 0-1.4-1.4L12 10.6 6.7 5.3Z"
                  />
                </svg>
              </button>
            </header>

            <div class="review-body">
              <p v-if="reviewActionError" class="error">{{ reviewActionError }}</p>

              <dl class="guion-field">
                <dt>Sinopsis propuesta</dt>
                <dd>{{ pendingReview.previewStory.summary }}</dd>
              </dl>

              <dl class="guion-field">
                <dt>Nº personajes</dt>
                <dd>{{ pendingReview.previewStory.characters.length }}</dd>
              </dl>

              <dl class="guion-field">
                <dt>Comentario del usuario</dt>
                <dd>{{ pendingReview.hint }}</dd>
              </dl>

              <div class="review-actions">
                <button type="button" class="btn" :disabled="resolvingReview" @click="onDiscardReview">
                  Descartar
                </button>
                <button type="button" class="btn btn-primary" :disabled="resolvingReview" @click="onAcceptReview">
                  Aceptar
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <review-story-modal
    :open="showReviewModal"
    :story-id="storyId"
    @close="showReviewModal = false"
    @accepted="onReviewAccepted"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import { confirmStoryReview, getPendingStoryReview, type StoryReview } from '../api/reviews'
import { getStory, getStoryPages, type PageSummary, type StorySummary } from '../api/storyteller'
import { useServerStatusStore } from '../stores/serverStatus'
import { useToastStore } from '../stores/toast'
import ReviewStoryModal from './ReviewStoryModal.vue'

type TabId = 'guion' | 'portada' | 'personajes' | 'paginas'

interface Props {
  open: boolean
  mode: 'create' | 'edit'
  storyId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; delete: [story: StorySummary]; updated: [storyId: number] }>()

const serverStatus = useServerStatusStore()
const toastStore = useToastStore()
const directorAgentActive = computed(() => serverStatus.data?.ollama.director ?? false)

const activeTab = ref<TabId>('guion')

const story = ref<StorySummary | null>(null)
const pages = ref<PageSummary[]>([])
const loadingStory = ref(false)
const storyError = ref<string | null>(null)
const pendingReview = ref<StoryReview | null>(null)
const showReviewModal = ref(false)
const showReviewDetail = ref(false)
const resolvingReview = ref(false)
const reviewActionError = ref<string | null>(null)

const hasCoverPage = computed(() => pages.value.some((page) => page.page === 0))
const hasRegularPage = computed(() => pages.value.some((page) => page.page >= 1))

const visibleTabs = computed<{ id: TabId; label: string }[]>(() => {
  const result: { id: TabId; label: string }[] = [{ id: 'guion', label: 'Guión' }]
  if (hasCoverPage.value) result.push({ id: 'portada', label: 'Portada' })
  result.push({ id: 'personajes', label: 'Personajes' })
  if (hasRegularPage.value) result.push({ id: 'paginas', label: 'Páginas' })
  return result
})

const unlockedTabs = computed<TabId[]>(() => (props.mode === 'create' ? ['guion'] : ['guion', 'personajes']))

const title = computed(() => story.value?.title ?? (props.mode === 'create' ? 'Crear cuento' : 'Editar cuento'))

function isUnlocked(tab: TabId) {
  return tab === 'portada' || tab === 'paginas' ? true : unlockedTabs.value.includes(tab)
}

async function loadStory(storyId: number) {
  loadingStory.value = true
  storyError.value = null

  try {
    const [storyResult, pendingReviewResult, pagesResult] = await Promise.all([
      getStory(storyId),
      getPendingStoryReview(storyId),
      getStoryPages(storyId),
    ])
    story.value = storyResult
    pendingReview.value = pendingReviewResult
    pages.value = pagesResult
  } catch (err) {
    storyError.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    loadingStory.value = false
  }
}

function onReviewAccepted(review: StoryReview) {
  showReviewModal.value = false
  pendingReview.value = review
  toastStore.show('Revisión aceptada y creada')
}

async function resolveReview(status: 'CONFIRMED' | 'DISCARDED') {
  if (!props.storyId) return

  resolvingReview.value = true
  reviewActionError.value = null

  try {
    await confirmStoryReview(props.storyId, status)
    showReviewDetail.value = false
    pendingReview.value = null
    toastStore.show(status === 'CONFIRMED' ? 'Revisión confirmada' : 'Revisión descartada')
    await loadStory(props.storyId)

    if (status === 'CONFIRMED') {
      emit('updated', props.storyId)
    }
  } catch (err) {
    reviewActionError.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    resolvingReview.value = false
  }
}

function onAcceptReview() {
  resolveReview('CONFIRMED')
}

function onDiscardReview() {
  resolveReview('DISCARDED')
}

watch(
  () => [props.open, props.storyId] as const,
  ([isOpen, storyId]) => {
    if (!isOpen) return

    activeTab.value = 'guion'
    story.value = null
    pages.value = []
    pendingReview.value = null
    showReviewModal.value = false
    showReviewDetail.value = false
    reviewActionError.value = null

    if (storyId) {
      loadStory(storyId)
    }
  },
)

function onClose() {
  emit('close')
}

function onOverlayClose() {
  if (showReviewDetail.value) {
    showReviewDetail.value = false
    return
  }
  onClose()
}

function onGeneratePage() {
  // TODO: wire up once the page generation flow is scoped.
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
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.45);
}

.modal-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: center;
  gap: 16px;
  max-width: 100%;
}

.modal {
  display: flex;
  flex-direction: column;
  width: 640px;
  max-width: 100%;
  max-height: 80vh;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
  transition: opacity 0.15s ease;
}

.modal-blocked {
  pointer-events: none;
  opacity: 0.4;
}

.review-panel {
  width: 320px;
}

.review-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  overflow-y: auto;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.badge {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
  white-space: nowrap;
  background: #fef3c7;
  border-radius: 999px;
}

.badge-button {
  align-self: flex-start;
  cursor: pointer;
  border: none;
}

.badge-button:hover {
  background: #fde68a;
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

.tabs {
  display: flex;
  gap: 4px;
  padding: 16px 20px 0;
  border-bottom: 1px solid var(--border);
}

.tab {
  padding: 8px 14px;
  font: inherit;
  color: var(--text);
  cursor: pointer;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
}

.tab.active {
  color: var(--text-h);
  border-bottom-color: var(--text-h);
}

.tab:disabled {
  color: var(--text);
  cursor: not-allowed;
  opacity: 0.4;
}

.tab-content {
  flex: 1;
  min-height: 240px;
  padding: 20px;
  overflow-y: auto;
}

.guion-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.guion-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin: 0;
}

.guion-field {
  margin: 0;
}

.guion-field dt {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}

.guion-field dd {
  margin: 4px 0 0;
  color: var(--text-h);
}

.guion-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.guion-actions-left {
  display: flex;
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

.btn-danger {
  color: #e5484d;
  border-color: #e5484d;
}

.btn-danger:hover {
  color: #fff;
  background: #e5484d;
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
