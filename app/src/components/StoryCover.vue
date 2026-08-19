<template>
  <div class="cover">
    <img v-if="src" :src="src" alt="" class="cover-img" />
    <div v-else class="cover-placeholder">NO IMAGE</div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { getStorytellerAsset } from '../api/storyteller'

interface Props {
  path: string | null
}

const props = defineProps<Props>()
const src = ref<string | null>(null)

function revoke() {
  if (src.value) {
    URL.revokeObjectURL(src.value)
    src.value = null
  }
}

async function load(path: string | null) {
  revoke()
  if (!path) return

  try {
    const blob = await getStorytellerAsset(path)
    src.value = URL.createObjectURL(blob)
  } catch {
    src.value = null
  }
}

watch(() => props.path, load, { immediate: true })
onUnmounted(revoke)
</script>

<style scoped>
.cover {
  width: 64px;
  height: 64px;
}

.cover-img {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 4px;
  font-size: 10px;
  font-weight: 600;
  color: var(--text);
  text-align: center;
  background: rgba(120, 120, 120, 0.12);
  border: 1px dashed var(--border);
  border-radius: 4px;
}
</style>
