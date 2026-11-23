import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './httpClient'
import { getNarrationState, getPageAudio, startNarration } from './storyNarration'

const BASE = 'http://localhost:8080/api/v1/storyteller/7'

const fetchMock = vi.fn()

function ok(data: unknown, status = 200) {
  return new Response(JSON.stringify({ success: true, data }), { status })
}

function rejected(status: number, code: string, message: string) {
  return new Response(JSON.stringify({ success: false, message, errors: [code], data: null }), { status })
}

function lastCall() {
  const [url, init] = fetchMock.mock.calls[fetchMock.mock.calls.length - 1]
  return { url: url as string, init }
}

const state = {
  storyId: 7,
  hasOutline: true,
  canNarrate: false,
  disabledReason: 'IN_PROGRESS',
  narrating: true,
  hasAudio: false,
  voiceName: null,
  lastResult: 'IN_PROGRESS',
}

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('inicio', () => {
  it('inicia con una voz numérica enviando voiceId como string', async () => {
    fetchMock.mockResolvedValue(ok(state, 202))

    const result = await startNarration(7, 12)

    const { url, init } = lastCall()
    expect(url).toBe(`${BASE}/narration`)
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body)).toEqual({ voiceId: '12' })
    expect(result).toEqual(state)
    expect(result.narrating).toBe(true)
  })

  it('inicia con la voz del sistema', async () => {
    fetchMock.mockResolvedValue(ok(state, 202))

    await startNarration(7, 'system')

    expect(JSON.parse(lastCall().init.body)).toEqual({ voiceId: 'system' })
  })

  it.each([
    'NO_VOICES_AVAILABLE',
    'GENERATION_IN_PROGRESS',
    'PAGES_NOT_ACCEPTED',
    'NO_OUTLINE',
    'IN_PROGRESS',
    'VOICE_NOT_AVAILABLE',
  ])('rechazo %s llega con su motivo', async (code) => {
    fetchMock.mockResolvedValue(rejected(409, code, `texto ${code}`))

    const error = await startNarration(7, 'system').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.code).toBe(code)
    expect(error.message).toBe(`texto ${code}`)
  })
})

describe('estado', () => {
  it('devuelve el estado completo con todos los campos', async () => {
    const full = {
      storyId: 7,
      hasOutline: true,
      canNarrate: true,
      disabledReason: null,
      narrating: false,
      hasAudio: true,
      voiceName: 'Mamá',
      lastResult: 'COMPLETED',
    }
    fetchMock.mockResolvedValue(ok(full))

    const result = await getNarrationState(7)

    expect(lastCall().url).toBe(`${BASE}/narration`)
    expect(lastCall().init.method).toBe('GET')
    expect(result).toEqual(full)
  })

  it('conserva el motivo de deshabilitado', async () => {
    fetchMock.mockResolvedValue(
      ok({ ...state, canNarrate: false, disabledReason: 'PAGES_NOT_ACCEPTED', narrating: false, lastResult: null }),
    )

    const result = await getNarrationState(7)

    expect(result.canNarrate).toBe(false)
    expect(result.disabledReason).toBe('PAGES_NOT_ACCEPTED')
  })
})

describe('audio de página', () => {
  it('devuelve el blob de una página interior', async () => {
    fetchMock.mockResolvedValue(new Response(new Uint8Array([1, 2, 3]), { status: 200, headers: { 'Content-Type': 'audio/wav' } }))

    const blob = await getPageAudio(7, 2)

    expect(lastCall().url).toBe(`${BASE}/pages/2/audio`)
    expect(lastCall().init.method).toBe('GET')
    expect(blob.size).toBe(3)
    expect(blob.type).toBe('audio/wav')
  })

  it('una página sin audio llega como ApiError 404 AUDIO_NOT_FOUND', async () => {
    fetchMock.mockResolvedValue(rejected(404, 'AUDIO_NOT_FOUND', 'No hay audio'))

    const error = await getPageAudio(7, 3).catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(404)
    expect(error.code).toBe('AUDIO_NOT_FOUND')
  })

  it('rechaza la portada en el cliente sin llamar al servidor', async () => {
    const error = await getPageAudio(7, 0).catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(404)
    expect(error.code).toBe('AUDIO_NOT_FOUND')
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
