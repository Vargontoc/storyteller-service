import { defineStore } from 'pinia'
import { apiConfig } from '../api/config'
import { useToastStore } from './toast'

export type NotificationType = 'INFO' | 'ERROR' | 'SUCCESS'

export interface WebsocketNotification {
  type: NotificationType
  text: string
}

interface NotificationEvent extends WebsocketNotification {
  id: number
}

const RECONNECT_DELAY_MS = 3000

let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | undefined
let nextEventId = 0

function toWsUrl(path: string): string {
  return `${apiConfig.baseUrl.replace(/^http/, 'ws')}${path}`
}

export const useWebsocketStore = defineStore('websocket', {
  state: (): { lastEvent: NotificationEvent | null } => ({
    lastEvent: null,
  }),
  actions: {
    connect() {
      if (socket) return

      socket = new WebSocket(toWsUrl('/storyteller'))

      socket.addEventListener('message', (event) => {
        let data: WebsocketNotification
        try {
          data = JSON.parse(event.data)
        } catch {
          return
        }
        if (!data || typeof data.text !== 'string' || typeof data.type !== 'string') return

        this.lastEvent = { ...data, id: nextEventId++ }
        useToastStore().show(data.text, data.type.toLowerCase() as Lowercase<NotificationType>)
      })

      socket.addEventListener('close', () => {
        socket = null
        reconnectTimer = setTimeout(() => this.connect(), RECONNECT_DELAY_MS)
      })

      socket.addEventListener('error', () => {
        socket?.close()
      })
    },

    disconnect() {
      clearTimeout(reconnectTimer)
      socket?.close()
      socket = null
    },
  },
})
