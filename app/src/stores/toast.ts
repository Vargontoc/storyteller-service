import { defineStore } from 'pinia'

export type ToastType = 'info' | 'error' | 'success'

interface Toast {
  id: number
  message: string
  type: ToastType
}

let nextId = 0

export const useToastStore = defineStore('toast', {
  state: (): { toasts: Toast[] } => ({ toasts: [] }),
  actions: {
    show(message: string, type: ToastType = 'info', durationMs = 3000) {
      const id = nextId++
      this.toasts.push({ id, message, type })
      setTimeout(() => {
        this.toasts = this.toasts.filter((toast) => toast.id !== id)
      }, durationMs)
    },
  },
})
