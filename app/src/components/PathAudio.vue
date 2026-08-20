<template>
  <button
    type="button"
    class="speaker"
    :class="{ playing }"
    :disabled="!src || playing"
    :aria-label="playing ? 'Reproduciendo audio' : 'Reproducir audio'"
    :title="playing ? 'Reproduciendo audio' : 'Reproducir audio'"
    @click="play"
  >
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <path
        fill="currentColor"
        d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.06c1.48-.74 2.5-2.26 2.5-4.03zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"
      />
    </svg>
  </button>
  <slot></slot>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { getStorytellerAsset } from '../api/storyteller'

interface Props {
  path: string | null
}

const props = defineProps<Props>()
const src = ref<string | null>(null)
const playing = ref(false)
let audio: HTMLAudioElement | null = null

function stopAudio() {
  if (audio) {
    audio.pause()
    audio = null
  }
  playing.value = false
}

function revoke() {
  stopAudio()
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

function play() {
  if (!src.value || playing.value) return

  audio = new Audio(src.value)
  audio.addEventListener('ended', stopAudio)
  audio.addEventListener('error', stopAudio)
  playing.value = true
  audio.play()
}

watch(() => props.path, load, { immediate: true })
onUnmounted(revoke)
</script>

<style scoped>
.speaker {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  color: var(--text-h);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 50%;
  transition: background-color 0.15s ease;
}

.speaker:hover:not(:disabled) {
  background: rgba(120, 120, 120, 0.12);
}

.speaker:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.speaker.playing {
  color: #2563eb;
  border-color: #2563eb;
  opacity: 1;
  animation: speaker-pulse 1.2s ease-in-out infinite;
}

@keyframes speaker-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
