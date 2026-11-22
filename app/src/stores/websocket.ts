import { defineStore } from 'pinia'
import { apiConfig } from '../api/config'
import { useToastStore } from './toast'

export type NotificationType = 'INFO' | 'ERROR' | 'SUCCESS'

export interface WebsocketNotification {
  type: NotificationType
  text: string
}

export type PortraitGenerationEventType = 'STARTED' | 'PROGRESS' | 'SUCCESS' | 'ERROR'
export type PortraitGenerationStatus = 'RUNNING' | 'COMPLETED' | 'FAILED'
export interface PortraitGenerationEvent {
  catalogCharacterId: number
  type: PortraitGenerationEventType
  status: PortraitGenerationStatus
  reason: string
  id: number
}

// Evento de generación de portada (pageNumber 0) o página de un cuento. `reason` solo en rechazo de hint o fallo.
export type StoryPageGenerationEventType = 'STARTED' | 'VERSION_PRESENTED' | 'HINT_REJECTED' | 'FAILED'
export interface StoryPageGenerationEvent {
  storyId: number
  pageNumber: number
  type: StoryPageGenerationEventType
  reason?: string
  id: number
}

const PAGE_EVENT_TYPES: string[] = ['STARTED', 'VERSION_PRESENTED', 'HINT_REJECTED', 'FAILED']

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
  state: (): { lastEvent: NotificationEvent | null; lastPortraitEvent: PortraitGenerationEvent | null; lastStoryPageEvent: StoryPageGenerationEvent | null; connectionId: number } => ({
    lastEvent: null,
    lastPortraitEvent: null,
    lastStoryPageEvent: null,
    connectionId: 0,
  }),
  actions: {
    connect() {
      if (socket) return

      socket = new WebSocket(toWsUrl('/storyteller'))

      socket.addEventListener('open', () => {
        this.connectionId += 1
      })

      socket.addEventListener('message', (event) => {
        let data: unknown
        try {
          data = JSON.parse(event.data)
        } catch {
          return
        }
        if (!data || typeof data !== 'object') return
        const portrait = data as Partial<PortraitGenerationEvent>
        if (typeof portrait.catalogCharacterId === 'number' && typeof portrait.type === 'string' && typeof portrait.status === 'string' && typeof portrait.reason === 'string') {
          this.lastPortraitEvent = { catalogCharacterId: portrait.catalogCharacterId, type: portrait.type as PortraitGenerationEventType, status: portrait.status as PortraitGenerationStatus, reason: portrait.reason, id: nextEventId++ }
          const notificationType: NotificationType = portrait.type === 'ERROR' ? 'ERROR' : portrait.type === 'SUCCESS' ? 'SUCCESS' : 'INFO'
          useToastStore().show(portrait.reason, notificationType.toLowerCase() as Lowercase<NotificationType>)
          return
        }
        const page = data as Partial<StoryPageGenerationEvent>
        if (typeof page.storyId === 'number' && typeof page.pageNumber === 'number' && typeof page.type === 'string' && PAGE_EVENT_TYPES.includes(page.type)) {
          this.lastStoryPageEvent = {
            storyId: page.storyId,
            pageNumber: page.pageNumber,
            type: page.type as StoryPageGenerationEventType,
            reason: typeof page.reason === 'string' ? page.reason : undefined,
            id: nextEventId++,
          }
          return
        }
        const notification = data as Partial<WebsocketNotification>
        if (typeof notification.text !== 'string' || typeof notification.type !== 'string') return

        this.lastEvent = { type: notification.type as NotificationType, text: notification.text, id: nextEventId++ }
        useToastStore().show(notification.text, notification.type.toLowerCase() as Lowercase<NotificationType>)
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
