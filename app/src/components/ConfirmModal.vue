<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="overlay" @click.self="onCancel" @keydown.esc="onCancel">
        <div class="modal" role="alertdialog" aria-modal="true" :aria-label="title">
          <h2 class="title">{{ title }}</h2>
          <p class="message">{{ message }}</p>

          <div class="actions">
            <button type="button" class="btn" @click="onCancel">{{ cancelLabel }}</button>
            <button
              ref="confirmButton"
              type="button"
              class="btn"
              :class="{ danger }"
              @click="onConfirm"
            >
              {{ confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

interface Props {
  open: boolean
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Confirmar',
  confirmLabel: 'Confirmar',
  cancelLabel: 'Cancelar',
  danger: false,
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const confirmButton = ref<HTMLButtonElement | null>(null)

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      nextTick(() => confirmButton.value?.focus())
    }
  },
)

function onConfirm() {
  emit('confirm')
}

function onCancel() {
  emit('cancel')
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 110;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.45);
}

.modal {
  width: 100%;
  max-width: 360px;
  padding: 24px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.title {
  margin: 0 0 8px;
  font-size: 18px;
  color: var(--text-h);
}

.message {
  margin: 0 0 20px;
  color: var(--text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.btn:hover {
  background: rgba(120, 120, 120, 0.12);
}

.btn.danger {
  color: #fff;
  background: #e5484d;
  border-color: #e5484d;
}

.btn.danger:hover {
  background: #c93a3f;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
