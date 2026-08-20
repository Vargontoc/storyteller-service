<template>
  <section class="storyteller">
    <div class="toolbar">
      <button type="button" class="icon-button" aria-label="Crear cuento" title="Crear cuento" @click="onCreateClick">
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 5a1 1 0 0 1 1 1v5h5a1 1 0 1 1 0 2h-5v5a1 1 0 1 1-2 0v-5H6a1 1 0 1 1 0-2h5V6a1 1 0 0 1 1-1Z"
          />
        </svg>
      </button>
    </div>

    <p v-if="loading">Cargando cuentos...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else>
      <table class="stories-table">
        <thead>
          <tr>
            <th>Portada</th>
            <th>Título</th>
            <th>Sinopsis</th>
            <th>Tamaño</th>
            <th>Nº personajes</th>
            <th>Páginas generadas</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="story in items" :key="story.id" class="story-row" @dblclick="onRowDoubleClick(story)">
            <td><path-image :path="story.cover" /></td>
            <td>{{ story.title }}</td>
            <td class="synopsis">{{ story.synopsis }}</td>
            <td>{{ story.size }}</td>
            <td>{{ story.actors }}</td>
            <td>{{ story.pages }}</td>
            <td @dblclick.stop>
              <button
                type="button"
                class="icon-button"
                aria-label="Eliminar cuento"
                title="Eliminar cuento"
                :disabled="deletingId === story.id"
                @click.stop="onDeleteClick(story)"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M9 3a1 1 0 0 0-1 1v1H4a1 1 0 1 0 0 2h1v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7h1a1 1 0 1 0 0-2h-4V4a1 1 0 0 0-1-1H9Zm1 6a1 1 0 1 1 2 0v8a1 1 0 1 1-2 0V9Zm5-1a1 1 0 0 0-1 1v8a1 1 0 1 0 2 0V9a1 1 0 0 0-1-1Z"
                  />
                </svg>
              </button>
            </td>
          </tr>
          <tr v-if="items.length === 0">
            <td colspan="7" class="empty">No hay cuentos todavía</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button type="button" :disabled="currentPage <= 0" @click="goToPage(currentPage - 1)">Anterior</button>
        <span>Página {{ currentPage + 1 }} de {{ Math.max(totalPages, 1) }}</span>
        <button type="button" :disabled="currentPage + 1 >= totalPages" @click="goToPage(currentPage + 1)">
          Siguiente
        </button>
      </div>
    </template>

    <confirm-modal
      :open="pendingDelete !== null"
      title="Eliminar cuento"
      :message="`¿Seguro que quieres eliminar &quot;${pendingDelete?.title}&quot;? Esta acción no se puede deshacer.`"
      confirm-label="Eliminar"
      danger
      @confirm="confirmDelete"
      @cancel="pendingDelete = null"
    />

    <topic-selection-modal :open="showTopicModal" @close="showTopicModal = false" @created="onStoryCreated" />

    <story-modal
      :open="showStoryModal"
      :mode="storyModalMode"
      :story-id="activeStoryId"
      @close="onStoryModalClose"
      @delete="onDeleteClick"
      @updated="onStoryUpdated"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '../api/httpClient'
import { deleteStory, getStory, getStories, type StorySummary } from '../api/storyteller'
import ConfirmModal from '../components/ConfirmModal.vue'
import PathImage from '../components/PathImage.vue'
import TopicSelectionModal from '../components/TopicSelectionModal.vue'
import StoryModal from '../components/StoryModal.vue'
const PAGE_SIZE = 10

const items = ref<StorySummary[]>([])
const currentPage = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const deletingId = ref<number | null>(null)
const pendingDelete = ref<StorySummary | null>(null)
const showTopicModal = ref(false)
const showStoryModal = ref(false)
const storyModalMode = ref<'create' | 'edit'>('create')
const activeStoryId = ref<number | null>(null)

async function loadPage(page: number) {
  loading.value = true
  error.value = null

  try {
    const result = await getStories(page, PAGE_SIZE)
    items.value = result.items
    currentPage.value = result.currentPage
    totalPages.value = result.totalPages
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    loading.value = false
  }
}

function goToPage(page: number) {
  if (page < 0 || page >= totalPages.value) return
  loadPage(page)
}

function onDeleteClick(story: StorySummary) {
  pendingDelete.value = story
}

async function confirmDelete() {
  const story = pendingDelete.value
  if (!story) return

  pendingDelete.value = null
  deletingId.value = story.id

  try {
    await deleteStory(story.id)

    if (activeStoryId.value === story.id) {
      showStoryModal.value = false
      activeStoryId.value = null
    }

    const isLastItemOnPage = items.value.length === 1 && currentPage.value > 0
    await loadPage(isLastItemOnPage ? currentPage.value - 1 : currentPage.value)
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Unexpected error'
  } finally {
    deletingId.value = null
  }
}

function onCreateClick() {
  showTopicModal.value = true
}

function onStoryCreated(storyId: number) {
  showTopicModal.value = false
  storyModalMode.value = 'create'
  activeStoryId.value = storyId
  showStoryModal.value = true
}

function onRowDoubleClick(story: StorySummary) {
  storyModalMode.value = 'edit'
  activeStoryId.value = story.id
  showStoryModal.value = true
}

function onStoryModalClose() {
  showStoryModal.value = false
  activeStoryId.value = null
  loadPage(currentPage.value)
}

async function onStoryUpdated(storyId: number) {
  const index = items.value.findIndex((item) => item.id === storyId)
  if (index === -1) return

  try {
    items.value.splice(index, 1, await getStory(storyId))
  } catch {
    // Best-effort row refresh; the modal already reflects the latest state.
  }
}

onMounted(() => loadPage(0))
</script>

<style scoped>
.storyteller {
  padding: 0 20px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
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

.icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.stories-table {
  width: 100%;
  border-collapse: collapse;
}

.stories-table th,
.stories-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.story-row {
  cursor: pointer;
}

.synopsis {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  text-align: center;
  color: var(--text);
}

.error {
  color: #e5484d;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}
</style>
