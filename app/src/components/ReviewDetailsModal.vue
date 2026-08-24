<template>
    <div class="modal review-panel" role="dialog" aria-modal="true" aria-label="Revisión pendiente">
        <header class="modal-header">
            <h2 class="title">Revisión pendiente</h2>
            <button
                type="button"
                class="icon-button"
                aria-label="Cerrar"
                title="Cerrar"
                :disabled="resolvingReview"
                @click="$emit('close')"
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
            <p v-if="reviewError" class="error">{{ reviewError }}</p>
            <review-actor-content v-if="props.type == 'ACTOR'" :id="props.id" />
            <review-story-content v-if="props.type == 'SCRIPT'" :id="props.id" />
            <review-page-content v-if="props.type == 'COVER' || props.type == 'PAGE'" :id="props.id" />
            <div class="review-actions">
                <button type="button" class="btn" :disabled="clickAction" @click="onResolveReview('DISCARDED')">
                    Descartar
                </button>
                <button type="button" class="btn btn-primary" :disabled="clickAction" @click="onResolveReview('CONFIRMED')">
                    Aceptar
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { confirmActorReview, confirmStoryReview, type RevisionStatus, type ReviewType, confirmPageReview } from '../api/reviews';
import ReviewActorContent from './content/ReviewActorContent.vue';
import ReviewStoryContent from './content/ReviewStoryContent.vue';
import ReviewPageContent from './content/ReviewPageContent.vue';
import { ApiError } from '../api/httpClient';

interface Props {
    type: ReviewType
    id: number | undefined
}

const props = defineProps<Props>()
const emit = defineEmits(['close', 'apply'])

const resolvingReview = ref<boolean>(false)
const reviewError = ref<string | null>(null)
const clickAction = ref<boolean>(false)

function onResolveReview(status: RevisionStatus) {
    reviewError.value = null
    clickAction.value = true
    if(!props.id) return

    if(props.type == 'SCRIPT') {
        confirmStoryReview(props.id, status).then(() => {
            emit('apply', status)
        }).catch((err) => {
            reviewError.value =  err instanceof ApiError ? err.message : 'Unexpected error'
        }).finally(() => {
            clickAction.value = true
        })
    }

    if(props.type == 'ACTOR') {
        confirmActorReview(props.id, status).then(() => {
            emit('apply', status)
        }).catch((err) => {
            reviewError.value =  err instanceof ApiError ? err.message : 'Unexpected error'
        }).finally(() => {
            clickAction.value = true
        })
    }

    if(props.type == 'COVER' || props.type == 'PAGE'){
        confirmPageReview(props.id, status).then(() => {
            emit('apply', status)
        }).catch((err) => {
            reviewError.value =  err instanceof ApiError ? err.message : 'Unexpected error'
        }).finally(() => {
            clickAction.value = true
        })

    }
}


</script>

<style lang="css" scoped>

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
</style>