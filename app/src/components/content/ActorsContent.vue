<template>
    <nav class="tabs" role="tablist">
        <button
            v-for="actor in actors"
            :key="actor.id"
            type="button"
            role="tab"
            class="tab"
            :class="{active: activeTab == actor.id}"
            :aria-selected="activeTab == actor.id"
            @click="activeTab = actor.id"
        >{{ actor.name }}</button>
    </nav>
    
    <div class="tab-content">
        <p v-if="loading">Cargando personajes...</p>
        <p v-else-if="error" class="error">{{ error }}</p>
        <template v-else>
            <button 
                v-if="currentActortHasReview"
                type="button"
                class="badge badge-button"
                @click="$emit('review', getActiveActor().id)">Revisión pendiente</button>

            <dl class="fields">
                <div class="field">
                    <dt>Imagen</dt>
                    <dd>
                        <path-image :path="getActiveActor().image" variant="portrait" >
                            <button type="button" class="btn" @click="callGenerateImage">
                                Generar Imagen
                            </button>
                        </path-image>
                    </dd>
                </div>
            </dl>
            <dl class="fields">
                <div class="field">
                    <dt>Rol narrativo</dt>
                    <dd>{{ getActiveActor().role }}</dd>
                </div>
            </dl>
            <dl class="fields">
                <div class="field">
                    <dt>Descripcion visual</dt>
                    <dd>{{ getActiveActor().description }}</dd>
                </div>
            </dl>

            <div class="actions">
                <div class="actions-left">
                    <button
                        type="button"
                        class="btn"
                        @click="callReview"
                    >
                    Revisar personaje
                    </button>
                </div>
            </div>
        </template>
    </div>

    <review-modal
        :open="showReviewModal"
        :type-review="'ACTOR'"
        :story-id="storyId"
        :id="getActiveActor().id ?? null"
        @close="showReviewModal = false"
        @accepted="onReviewAccepted" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { getStoryActors, type ActorSummary } from '../../api/storyteller';
import { ApiError } from '../../api/httpClient';
import { useServerStatusStore } from '../../stores/serverStatus';
import ReviewModal from '../ReviewModal.vue';
import { getActorReview, isActorReview, type ActorReview, type StoryReview } from '../../api/reviews';
import { useToastStore } from '../../stores/toast';
import PathImage from '../PathImage.vue';
import { generateImage } from '../../api/generations';
interface Props {
    storyId: number
}

const props = defineProps<Props>()
const serverStatus = useServerStatusStore()
const toastStore = useToastStore()
const emit = defineEmits<{ review: [id: number], generateAsset: [isGeneration: boolean]}>()

const actors = ref<ActorSummary[]>([])
const loading = ref<boolean>(false)
const error = ref<string | null>(null)
const activeTab = ref<number | null>(null)
const showReviewModal = ref<boolean>(false)
const pendingReview = ref<ActorReview[]>([])

const imageServiceActive = computed(() => serverStatus.data?.comfy ?? false)
const directorAgentActive = computed(() => serverStatus.data?.ollama.director ?? false)
const currentActortHasReview = computed(() => {
    let has = false;
    pendingReview.value.forEach((r) => {
        if(r.characterId == getActiveActor().id){ has = true }
    })
    return has;
})

async function refreshActor(id: number) {
    const result = await getStoryActors(props.storyId)
    const updated = result.find((a) => a.id === id)
    if (!updated) return

    const index = actors.value.findIndex((a) => a.id === id)
    if (index !== -1) {
        actors.value[index] = updated
    }
}

function callReview(){
    if(directorAgentActive.value == false){
        toastStore.show("El Agente director está detenido o inaccesible")
        return
    }
    showReviewModal.value = true
}

async function callGenerateImage() {
    if(imageServiceActive.value == false){
        toastStore.show("El servicio de imagenes está detenido")
        return
    }
    const id = getActiveActor().id
    emit('generateAsset', true)
    toastStore.show("Generando imagen...")
    try {
        await Promise.all([generateImage('ACTOR', id)])
        await refreshActor(id)
        toastStore.show("Imagen generada")
    }catch(err) {
        toastStore.show("Hubo un error en la generación de la imagen")
    }finally {
        emit('generateAsset', false)
    }
}


async function loadActors()
{
    loading.value = true
    error.value = null
    try {
        const [result] = await Promise.all([getStoryActors(props.storyId)])
        actors.value = result
        activeTab.value = actors.value[0].id
        actors.value.forEach((a) => loadReview(a.id))
    } catch(err) {
        error.value = err instanceof ApiError ? err.message : 'Unexpected error'
    } finally {
        loading.value = false
    }
}

async function loadReview(id: number){
    const [review] = await  Promise.all([getActorReview(id)]);
    if(review){
        pendingReview.value.push(review)
    }
}

function getActiveActor() {
    let actor = {} as ActorSummary
    actors.value.forEach((e, i) => {
        if(e.id == activeTab.value){
            actor = e;
        }
    });

    return actor;
}

function onReviewAccepted(review: StoryReview | ActorReview) {
    if (!isActorReview(review)) return

    showReviewModal.value = false
    addOrUpdateReview(review);
    toastStore.show('Revisión aceptada y creada')
}

function addOrUpdateReview(review: ActorReview) {
    let updated = false;
    pendingReview.value.forEach((r, i) => {
        if(r.characterId == review.characterId){
            pendingReview.value[i] = review
            updated = true 
        }
    })

    if(updated == false) {
        pendingReview.value.push(review)
    }
}

loadActors();

</script>

<style scoped>
.error {
    margin: 0;
    color: #e5484d;
}
.tabs {
    margin-top: -35px;
    display: flex;
    gap: 4px;
    padding: 16px 20px 0;
    border-bottom: 1px solid var(--border);
    overflow-y: auto;
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
