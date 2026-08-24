<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onOverlayClose" @keydown.esc="onOverlayClose">
        <div class="modal-row">
          <div
            class="modal"
            :class="{ 'modal-blocked': showReviewDetail || isStateGeneration }"
            :inert="showReviewDetail || isStateGeneration"
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
                <story-content v-if="storyId"
                  :key="contentKey"
                  :story-id="storyId"
                  @generate-page="onGeneratePage"
                  @delete="onDelete"
                  @review="showReview" />
              </div>
              <div v-else-if="activeTab === 'portada'">
                <pages-content v-if="storyId"
                  :key="contentKey"
                  :story-id="storyId"
                  :is-cover="true"
                  @review="showReview"
                  @generate-asset="onAssetGeneration"></pages-content>

              </div>
              <div v-else-if="activeTab === 'personajes'">
                <actors-content v-if="storyId"
                :key="contentKey"
                :story-id="storyId"
                @review="showReview"
                @generate-asset="onAssetGeneration"

                ></actors-content>
              </div>
              <div v-else-if="activeTab === 'paginas'">
                <!-- review no existe todavía para páginas en backend: @review queda sin usar en pages-content hasta que exista -->
                <pages-content v-if="storyId"
                  :key="contentKey"
                  :story-id="storyId"
                  :is-cover="false"
                  @review="showReview"
                  @generate-asset="onAssetGeneration"></pages-content>
              </div>
            </div>
          </div>

          <review-details-modal
            v-if="showReviewDetail"
            @close="showReviewDetail = false"
            @apply="applyReview"
            :type="currentType"
            :id="currentId" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/httpClient'
import {  getPendingStoryReview, type ReviewType, type StoryReview, type RevisionStatus } from '../api/reviews'
import { getStory, getStoryPages, type PageSummary, type StorySummary } from '../api/storyteller'
import { useToastStore } from '../stores/toast'
import ActorsContent from './content/ActorsContent.vue'
import PagesContent from './content/PagesContent.vue'
import StoryContent from './content/StoryContent.vue'
import ReviewDetailsModal from './ReviewDetailsModal.vue'
import { generatePage } from '../api/generations'
type TabId = 'guion' | 'portada' | 'personajes' | 'paginas'

interface Props {
  open: boolean
  mode: 'create' | 'edit'
  storyId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; delete: [story: StorySummary]; updated: [storyId: number] }>()

const toastStore = useToastStore()
const currentType = computed((): ReviewType => {
  switch(activeTab.value){
    case 'guion': return 'SCRIPT';
    case 'personajes': return 'ACTOR';
    case 'portada': return 'COVER';
    case 'paginas': return 'PAGE';
  }
})

const currentId = ref<number | undefined>(undefined)

const activeTab = ref<TabId>('guion')

const story = ref<StorySummary | null>(null)
const pages = ref<PageSummary[]>([])
const loadingStory = ref(false)
const storyError = ref<string | null>(null)
const pendingReview = ref<StoryReview | null>(null)
const showReviewModal = ref(false)
const showReviewDetail = ref(false)
const reviewActionError = ref<string | null>(null)
const isStateGeneration = ref<boolean>(false)
const contentKey = ref(0)

const hasCoverPage = computed(() => pages.value.some((page) => page.page === 0))
const hasRegularPage = computed(() => pages.value.some((page) => page.page >= 1))

const visibleTabs = computed<{ id: TabId; label: string }[]>(() => {
  const result: { id: TabId; label: string }[] = [{ id: 'guion', label: 'Guión' }]
  result.push({ id: 'personajes', label: 'Personajes' })
  if (hasCoverPage.value) result.push({ id: 'portada', label: 'Portada' })
  if (hasRegularPage.value) result.push({ id: 'paginas', label: 'Páginas' })
  return result
})

const unlockedTabs = computed<TabId[]>(() => (props.mode === 'create' ? ['guion'] : ['guion', 'personajes']))

const title = computed(() => story.value?.title ?? (props.mode === 'create' ? 'Crear cuento' : 'Editar cuento'))

function onAssetGeneration(run: boolean) {
    isStateGeneration.value = run
}
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

function showReview(id: number){
  currentId.value = id;
  showReviewDetail.value = true
}

function onDelete(){
  if(story.value == null)
    return
  emit('delete', story.value)
}


async function applyReview(status: RevisionStatus){
  if (!props.storyId) return

  showReviewDetail.value = false
  toastStore.show(status === 'CONFIRMED' ? 'Revisión confirmada' : 'Revisión descartada')
  await loadStory(props.storyId)
  contentKey.value++
  if(status == 'CONFIRMED'){
    emit('updated', props.storyId)
  }
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

async function onGeneratePage() {
  if(!props.storyId || !story.value) return
  if(story.value.pages == story.value.size)
  {
    toastStore.show('Ya has alcanzado el número máximo de páginas')
    return
  }
  isStateGeneration.value = true
  try {
    const [result] = await Promise.all([generatePage(props.storyId)])
    toastStore.show(result.page == 0 ? 'Portada generada' : 'Nueva página generada')

    await loadStory(props.storyId)
    emit('updated', props.storyId)

  }catch(err) {
    toastStore.show('Algo fue mal en la generación de páginas')
  }finally {
    isStateGeneration.value = false
  }

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
