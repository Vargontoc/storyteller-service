<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useServerStatusStore } from './stores/serverStatus'
import { useWebsocketStore } from './stores/websocket'
import ToastContainer from './components/ToastContainer.vue'
import HeaderView from './views/HeaderView.vue';
import StorytellerView from './views/StorytellerView.vue';
import CharacterCatalogView from './views/CharacterCatalogView.vue'
import VoiceLibraryView from './views/VoiceLibraryView.vue'
const STATUS_POLL_INTERVAL_MS = 5_000

const serverStatus = useServerStatusStore()
const websocketStore = useWebsocketStore()
let statusPollId: ReturnType<typeof setInterval> | undefined
const activeView = ref<'stories' | 'characters' | 'voices'>('stories')

onMounted(() => {
  serverStatus.fetchStatus()
  statusPollId = setInterval(() => serverStatus.fetchStatus(), STATUS_POLL_INTERVAL_MS)
  websocketStore.connect()
})

onUnmounted(() => {
  clearInterval(statusPollId)
  websocketStore.disconnect()
})
</script>

<template>
  <main id="app-root">
    <header-view :api="serverStatus.data" />

    <p v-if="serverStatus.error" class="status-error" aria-live="polite">
      Sin conexión con el servidor.
    </p>
    <template v-else>
      <nav class="main-nav" aria-label="Secciones principales">
        <button type="button" :class="{ active: activeView === 'stories' }" @click="activeView = 'stories'">Cuentos</button>
        <button type="button" :class="{ active: activeView === 'characters' }" @click="activeView = 'characters'">Personajes</button>
        <button type="button" :class="{ active: activeView === 'voices' }" @click="activeView = 'voices'">Voces</button>
      </nav>
      <storyteller-view v-if="activeView === 'stories'" />
      <character-catalog-view v-else-if="activeView === 'characters'" />
      <voice-library-view v-else />
    </template>

    <toast-container />
  </main>
</template>

<style scoped>
#app-root {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.status-error {
  margin: 0;
  padding: 0 20px;
  color: #e5484d;
}

.main-nav { display: flex; gap: 8px; padding: 0 20px; }
.main-nav button { padding: 8px 12px; font: inherit; color: var(--text-h); cursor: pointer; background: transparent; border: 1px solid var(--border); border-radius: 6px; }
.main-nav button.active { color: #fff; background: #2563eb; border-color: #2563eb; }
</style>
