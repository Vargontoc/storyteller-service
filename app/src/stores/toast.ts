import { defineStore } from 'pinia'

interface Toast {
  id: number
  message: string
}

let nextId = 0

export const useToastStore = defineStore('toast', {
  state: (): { toasts: Toast[] } => ({ toasts: [] }),
  actions: {
    show(message: string, durationMs = 3000) {
      const id = nextId++
      this.toasts.push({ id, message })
      setTimeout(() => {
        this.toasts = this.toasts.filter((toast) => toast.id !== id)
      }, durationMs)
    },
  },
})
