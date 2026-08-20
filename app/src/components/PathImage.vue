<template>
  <div class="cover" :class="variant">
    <img v-if="src" :src="src" alt="" class="cover-img" />
    <div v-else class="cover-placeholder">NO IMAGE</div>
  </div>
  <slot></slot>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { getStorytellerAsset } from '../api/storyteller'

interface Props {
  path: string | null
  variant?: 'table' | 'portrait'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'table',
})

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
.cover.table {
  width: 64px;
  height: 64px;
}

.cover.table .cover-img,
.cover.table .cover-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover.portrait {
  width: fit-content;
  max-width: 100%;
}

.cover.portrait .cover-img {
  display: block;
  max-width: 320px;
  max-height: 420px;
  object-fit: contain;
}

.cover.portrait .cover-placeholder {
  width: 240px;
  height: 320px;
}

.cover-img {
  border-radius: 4px;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
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
