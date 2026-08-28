<template>
    <div class="fields" v-if="review">
        <dl class="field">
            <dt>Sinopsis propuesta</dt>
            <dd>{{ review.previewStory.synopsis }}</dd>
        </dl>

        <dl class="field">
            <dt>Nº personajes</dt>
            <dd>{{ review.previewStory.characters.length }}</dd>
        </dl>

        <dl class="field">
            <dt>Comentario del usuario</dt>
            <dd>{{ review.hint }}</dd>
        </dl>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getPendingStoryReview, type StoryReview } from '../../api/reviews';

interface Props { id: number | undefined }
const props = defineProps<Props>()

const review = ref<StoryReview | null>(null);

async function loadReview(id: number | undefined) {
    if(id == undefined) return
    try {
        const [result] = await Promise.all([getPendingStoryReview(id)])
        if(result){
            review.value = result
        }
    }catch(err) {
        console.log(err)
    }
}

loadReview(props.id)

</script>


<style lang="css" scoped>

.fields {
    display: flex;
    flex-direction: column;
    gap: 16px;
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
</style>