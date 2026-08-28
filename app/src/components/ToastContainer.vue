<template>
  <Teleport to="body">
    <div class="toast-container" aria-live="polite">
      <TransitionGroup name="toast">
        <div v-for="toast in toastStore.toasts" :key="toast.id" class="toast" :class="`toast-${toast.type}`">
          {{ toast.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useToastStore } from '../stores/toast'

const toastStore = useToastStore()
</script>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 24px;
  left: 50%;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transform: translateX(-50%);
}

.toast {
  padding: 10px 18px;
  color: #fff;
  white-space: nowrap;
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
}

.toast-info {
  background: #2563eb;
}

.toast-error {
  background: #dc2626;
}

.toast-success {
  background: #16a34a;
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
