<template>
    <p v-if="loading">Cargando personajes...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else-if="story">
            <button 
                v-if="hasReview"
                type="button"
                class="badge badge-button"
                @click="$emit('review', storyId)">Revisión pendiente</button>
        <dl class="fields">
            <div class="field">
                <dt>Tamaño</dt>
                <dd>{{ story.size }}</dd>
            </div>
            <div class="field">
                <dt>Páginas generadas</dt>
                <dd>{{ story.pages }}</dd>
            </div>
            <div class="field">
                <dt>Nº personajes</dt>
                <dd>{{ story.actors }}</dd>
            </div>
        </dl>

        <dl class="field">
            <dt>Sinopsis</dt>
            <dd>{{ story.synopsis }}</dd>
        </dl>

        
        <div class="actions">
            <div class="actions-left">
                <button type="button" class="btn" @click="emit('generatePage')">Generar página</button>
                <button
                type="button"
                class="btn"
                :disabled="!directorAgentActive"
                :title="directorAgentActive ? '' : 'El agente director no está disponible'"
                @click="showReviewModal = true"
                >
                Revisar guión
                </button>
                <button v-if="story.pages == story.size" type="button"   class="btn" @click="callAudioStory()">
                    Generar narración
                </button>
            </div>

            <button type="button" class="btn btn-danger" @click="emit('delete')">
                Eliminar cuento
            </button>
        </div>
    </div>

    <review-modal
        :open="showReviewModal"
        :type-review="'SCRIPT'"
        :story-id="storyId"
        :id="null"
        @close="showReviewModal = false"
        @accepted="onReviewAccepted" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { getStory, type StorySummary } from '../../api/storyteller';
import { useServerStatusStore } from '../../stores/serverStatus';
import { type ActorReview, getPendingStoryReview, isStoryReview, type StoryReview } from '../../api/reviews';
import { ApiError } from '../../api/httpClient';
import ReviewModal from '../ReviewModal.vue';
import { useToastStore } from '../../stores/toast';
import { generateStoryAudio } from '../../api/generations.ts';
const toastStore = useToastStore()
interface Props {
    storyId: number
}
const props = defineProps<Props>()
const emit = defineEmits<{ review: [id: number], delete: [], generatePage: []}>()

const serverStatus = useServerStatusStore()

const showReviewModal = ref<boolean>(false)
const loading = ref<boolean>()
const error = ref<string | null>(null)
const story = ref<StorySummary | null>(null)
const hasReview = ref<boolean>(false)
const directorAgentActive = computed(() => serverStatus.data?.ollama.director ?? false)


function onReviewAccepted(review: StoryReview | ActorReview) {
    if (!isStoryReview(review)) return

    showReviewModal.value = false
    toastStore.show('Revisión aceptada y creada')
}

async function loadStory() {
    loading.value = true
    error.value = null

    try{
        const [result, review] = await Promise.all([getStory(props.storyId), getPendingStoryReview(props.storyId)])
        story.value = result
        if(review) {
            hasReview.value = true
        }
        
    }catch(err) {
        error.value = err instanceof ApiError ? err.message : 'Unexpected error'
    } finally {
        loading.value = false
    }
}

async function callAudioStory() {
    if(!props.storyId) return;
    await generateStoryAudio(props.storyId)
}

loadStory()

</script>

<style lang="css" scoped>
.error {
    margin: 0;
    color: #e5484d;
}
.fields {
    display: flex;
    flex-wrap: wrap;
    gap: 24px;
    margin: 0;
}

.field {
    margin: 0;
}

.field dt {
    font-size: 12px;
    font-weight: 600;
    color: var(--text);
}

.field dd {
    margin: 4px 0 0;
    color: var(--text-h);
}

.actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.actions-left {
    display: flex;
    gap: 12px;
}

.btn {
    margin-top: 15px;
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
</style>