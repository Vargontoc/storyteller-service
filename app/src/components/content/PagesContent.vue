
<template>
    <nav v-if="!isCover" class="tabs">
        <button
            v-for="actor in pages"
            :key="actor.id"
            type="button"
            role="tab"
            class="tab"
            :class="{active: activeTab == actor.id}"
            :aria-selected="activeTab == actor.id"  
            @click="activeTab = actor.id"
        >{{ 'Pagina ' + actor.page }}</button>
    </nav>
    <div class="tab-content">
        <p v-if="loading">Cargando paginas...</p>
        <p v-else-if="error" class="error">{{ error }}</p>
        <template v-else>
            <button 
                v-if="currentPageHasReview"
                type="button"
                class="badge badge-button"
                @click="$emit('review', getPageActive().id)">Revisión pendiente</button>

            <dl class="fields">
                <div class="field">
                    <dt>Imagen</dt>
                    <dd>
                        <path-image :path="getPageActive().image" variant="portrait">
                            <button type="button" class="btn" @click="callGenerateImage">
                                Generar Imagen
                            </button>
                        </path-image>
                    </dd>
                </div>
                
                <div class="field" v-if="!isCover">
                    <dt>Audio</dt>
                    <dd>
                        <path-audio :path="getPageActive().audio" ></path-audio>
                    </dd>
                </div>
                <div class="field">
                    <dt></dt>
                    <dd>
                    </dd>
                </div>
            </dl>
            <dl v-if="!isCover" class="fields">
                <div class="field">
                    <dt>Texto</dt>
                    <dd>{{ getPageActive().text }}</dd>
                </div>
            </dl>
            <dl class="fields">
                <div class="field">
                    <dt>Escena</dt>
                    <dd>{{ getPageActive().scene }}</dd>
                </div>
            </dl>

            <div class="actions">
                <div class="actions-left">
                    <button
                        type="button"
                        class="btn"
                        @click="callReview"
                    >
                    Revisar {{ props.isCover ? ' portada' : ' página' }}
                    </button>
                </div>
            </div>
        </template>
    </div>

    <review-modal
        :open="showReviewModal"
        :type-review="getPageActive().page == 0 ? 'COVER': 'PAGE'"
        :story-id="storyId"
        :id="getPageActive().id ?? null"
        @close="showReviewModal = false"
        @accepted="onReviewAccepted"
    />
</template>


<script setup lang="ts">
import { computed, ref } from 'vue';
import { getStoryPages, type PageSummary } from '../../api/storyteller';
import { useServerStatusStore } from '../../stores/serverStatus';
import { ApiError } from '../../api/httpClient';
import { getPageReview, isPageReview, type ActorReview, type PageReview, type StoryReview } from '../../api/reviews';
import { useToastStore } from '../../stores/toast';
import ReviewModal from '../ReviewModal.vue';

import PathImage from '../PathImage.vue';
import PathAudio from '../PathAudio.vue';
import { generateAudio, generateImage } from '../../api/generations';

interface Props {
    storyId: number,
    isCover: boolean
}
const props = defineProps<Props>()
const serverStatus = useServerStatusStore()
const toastStore = useToastStore()
const emit = defineEmits<{ review: [id: number], generateAsset: [isGeneration: boolean]}>() 

const pages = ref<PageSummary[]>([])
const loading = ref<boolean>(false)
const error = ref<string | null>(null)
const activeTab = ref<number | null>(null)
const showReviewModal = ref<boolean>(false)
const pendingReview = ref<PageReview[]>([])
const writerAgentActive = computed(() => serverStatus.data?.ollama.scriptwriter ?? false )
const audioServiceActive = computed(() => serverStatus.data?.chatterbox ?? false)
const imageServiceActive = computed(() => serverStatus.data?.comfy ?? false)
const currentPageHasReview = computed(() => {
    let has = false
    pendingReview.value.forEach((r) => {
        if(r.idPage == getPageActive().id) { has = true }
    })
    return has;
})

async function refreshPage(id: number) {
    const result = await getStoryPages(props.storyId)
    const updated = result.find((p) => p.id === id)
    if (!updated) return

    const index = pages.value.findIndex((p) => p.id === id)
    if (index !== -1) {
        pages.value[index] = updated
    }
}


function callReview(){
    if(writerAgentActive.value == false){
        toastStore.show("El Agente escritor está detenido o inaccesible")
        return
    }
    showReviewModal.value = true
}
async function callGenerateAudio() {
    if(audioServiceActive.value == false){
        toastStore.show("El servicio de audio está detenido")
        return
    }

    if(props.isCover == true) return

    const id = getPageActive().id
    emit('generateAsset', true)
    toastStore.show("Generando audio...")
    try {
        await Promise.all([generateAudio({
            pageId: id,
            preset: 'NEUTRO'
        })])
        await refreshPage(id)
        toastStore.show("Audio generado")
    }catch(err) {
        toastStore.show("Hubo un error en la generación del audio")
    }finally {
        emit('generateAsset', false)
    }
}

async function callGenerateImage() {
    if(imageServiceActive.value == false){
        toastStore.show("El servicio de imagenes está detenido")
        return
    }

    const id = getPageActive().id
    emit('generateAsset', true)
    toastStore.show("Generando imagen...")
    try {
        await Promise.all([generateImage(props.isCover ? 'COVER' : 'PAGE', id)])
        await refreshPage(id)
        toastStore.show("Imagen generada")
    }catch(err) {
        toastStore.show("Hubo un error en la generación de la imagen")
    }finally {
        emit('generateAsset', false)
    }
}

async function loadPages()
{
    loading.value = true
    error.value  = null

    try {
        const [result] = await Promise.all([getStoryPages(props.storyId)])
        pages.value = []
        result.forEach((p) => {
            if(props.isCover === true && p.page == 0){
                pages.value.push(p)
            }else if(props.isCover === false && p.page != 0) {
                pages.value.push(p)
            }
        })
        activeTab.value = pages.value[0].id
        pages.value.forEach((p) => loadReview(p.id))


    }catch(err) {
        error.value = err instanceof ApiError ? err.message : 'Unexpected error'
    }finally {
        loading.value = false
    }
}

async function loadReview(id: number){
    const [review] = await  Promise.all([getPageReview(id)]);
    if(review){
        pendingReview.value.push(review)
    }
}

function getPageActive() {
    let page = {} as PageSummary
    pages.value.forEach((e)=> {
        if(e.id == activeTab.value){
            page = e
        }
    })
    return page;
}

function onReviewAccepted(review: StoryReview | ActorReview | PageReview){
    if(!isPageReview(review)) return

    showReviewModal.value = false
    addOrUpdateReview(review)
    toastStore.show('Revisión aceptada y creada')
}

function addOrUpdateReview(review: PageReview) {
    let updated  = false
    pendingReview.value.forEach((r, i) => {
        if(r.idPage == review.idPage){
            pendingReview.value[i] = review
            updated = true
        }
    })

    if(updated == false)
        pendingReview.value.push(review)
}

loadPages()
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