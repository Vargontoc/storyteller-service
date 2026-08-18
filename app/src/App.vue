<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useServerStatusStore } from './stores/serverStatus'
import HeaderView from './views/HeaderView.vue';
import StorytellerView from './views/StorytellerView.vue';
const STATUS_POLL_INTERVAL_MS = 5_000

const serverStatus = useServerStatusStore()
let statusPollId: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  serverStatus.fetchStatus()
  statusPollId = setInterval(() => serverStatus.fetchStatus(), STATUS_POLL_INTERVAL_MS)
})

onUnmounted(() => {
  clearInterval(statusPollId)
})
</script>

<template>
  <main id="app-root">
    <header-view :api="serverStatus.data" />

    <p v-if="serverStatus.error" class="status-error" aria-live="polite">
      Sin conexión con el servidor.
    </p>
    <storyteller-view v-else></storyteller-view>
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
</style>
