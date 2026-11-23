import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useWebsocketStore } from './websocket'

// Socket simulado: guarda los listeners y permite emitir eventos a mano.
class FakeSocket {
  static last: FakeSocket
  listeners: Record<string, (event: { data?: string }) => void> = {}
  constructor() {
    FakeSocket.last = this
  }
  addEventListener(name: string, listener: (event: { data?: string }) => void) {
    this.listeners[name] = listener
  }
  close() {}
  emit(name: string, data?: unknown) {
    this.listeners[name]({ data: data === undefined ? undefined : JSON.stringify(data) })
  }
}

describe('websocket store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('WebSocket', FakeSocket)
  })

  afterEach(() => {
    useWebsocketStore().disconnect()
    vi.unstubAllGlobals()
  })

  function connected() {
    const store = useWebsocketStore()
    store.connect()
    return store
  }

  it('expone cuento y página de VERSION_PRESENTED', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'VERSION_PRESENTED', storyId: 7, pageNumber: 3 })

    expect(store.lastStoryPageEvent).toMatchObject({ type: 'VERSION_PRESENTED', storyId: 7, pageNumber: 3 })
    expect(store.lastStoryPageEvent?.reason).toBeUndefined()
  })

  it('expone el motivo de HINT_REJECTED', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'HINT_REJECTED', storyId: 7, pageNumber: 0, reason: 'Hint no válido' })

    expect(store.lastStoryPageEvent).toMatchObject({ type: 'HINT_REJECTED', pageNumber: 0, reason: 'Hint no válido' })
  })

  it('conserva el tratamiento de un evento de retratos', () => {
    const store = connected()
    FakeSocket.last.emit('message', { catalogCharacterId: 5, type: 'STARTED', status: 'RUNNING', reason: 'Generando' })

    expect(store.lastPortraitEvent).toMatchObject({ catalogCharacterId: 5, type: 'STARTED', status: 'RUNNING', reason: 'Generando' })
    expect(store.lastStoryPageEvent).toBeNull()
  })

  it('expone STARTED de narración con su cuento y sin tocar el evento de página', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'STARTED', storyId: 9 })

    expect(store.lastNarrationEvent).toMatchObject({ type: 'STARTED', storyId: 9 })
    expect(store.lastNarrationEvent?.voiceName).toBeUndefined()
    expect(store.lastStoryPageEvent).toBeNull()
  })

  it('expone COMPLETED de narración con la voz', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'COMPLETED', storyId: 9, voiceName: 'Abuela' })

    expect(store.lastNarrationEvent).toMatchObject({ type: 'COMPLETED', storyId: 9, voiceName: 'Abuela' })
  })

  it('COMPLETED sin voz no inventa valor', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'COMPLETED', storyId: 9 })

    expect(store.lastNarrationEvent?.type).toBe('COMPLETED')
    expect(store.lastNarrationEvent?.voiceName).toBeUndefined()
  })

  it('expone FAILED de narración con su cuento y sin motivo', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'FAILED', storyId: 4 })

    expect(store.lastNarrationEvent).toMatchObject({ type: 'FAILED', storyId: 4 })
    expect(store.lastStoryPageEvent).toBeNull()
  })

  it('eventos de narración iguales consecutivos obtienen identificador distinto', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'STARTED', storyId: 9 })
    const first = store.lastNarrationEvent?.id
    FakeSocket.last.emit('message', { type: 'STARTED', storyId: 9 })

    expect(store.lastNarrationEvent?.id).not.toBe(first)
  })

  it('STARTED y FAILED de página (con pageNumber) no son narración', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'STARTED', storyId: 7, pageNumber: 0 })
    FakeSocket.last.emit('message', { type: 'FAILED', storyId: 7, pageNumber: 2, reason: 'Fallo' })

    expect(store.lastStoryPageEvent).toMatchObject({ type: 'FAILED', storyId: 7, pageNumber: 2, reason: 'Fallo' })
    expect(store.lastNarrationEvent).toBeNull()
  })

  it('un evento de narración no se trata como página', () => {
    const store = connected()
    FakeSocket.last.emit('message', { type: 'FAILED', storyId: 7 })

    expect(store.lastNarrationEvent).toMatchObject({ type: 'FAILED', storyId: 7 })
    expect(store.lastStoryPageEvent).toBeNull()
  })

  it('tras reconectar cambia connectionId para que los suscriptores recarguen', () => {
    const store = connected()
    FakeSocket.last.emit('open')
    const before = store.connectionId
    FakeSocket.last.emit('open')

    expect(store.connectionId).toBe(before + 1)
  })

  it('una nueva apertura incrementa connectionId', () => {
    const store = connected()
    FakeSocket.last.emit('open')
    FakeSocket.last.emit('open')

    expect(store.connectionId).toBe(2)
  })
})
