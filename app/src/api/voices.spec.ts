import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './httpClient'
import { deleteVoice, getLanguages, listEligibleVoices, listVoices, uploadVoice } from './voices'

const BASE = 'http://localhost:8080/api/v1/voices'

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

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('listado', () => {
  it('lista la biblioteca con la voz del sistema primera y sin id', async () => {
    const library = {
      voices: [
        { name: 'Sistema', language: 'es', system: true },
        { id: 3, name: 'Mamá', language: 'es', system: false },
      ],
      systemVoiceName: 'Sistema',
      systemVoiceAvailable: true,
      systemVoiceUnavailableReason: null,
      systemVoiceNameCollision: false,
    }
    fetchMock.mockResolvedValue(ok(library))

    const result = await listVoices()

    expect(lastCall().url).toBe(BASE)
    expect(lastCall().init.method).toBe('GET')
    expect(result.voices[0].system).toBe(true)
    expect(result.voices[0].id).toBeUndefined()
    expect(result).toEqual(library)
  })

  it('conserva disponibilidad, motivo y colisión de nombre de la voz del sistema', async () => {
    fetchMock.mockResolvedValue(
      ok({
        voices: [],
        systemVoiceName: 'Sistema',
        systemVoiceAvailable: false,
        systemVoiceUnavailableReason: 'Muestra no encontrada',
        systemVoiceNameCollision: true,
      }),
    )

    const result = await listVoices()

    expect(result.systemVoiceAvailable).toBe(false)
    expect(result.systemVoiceUnavailableReason).toBe('Muestra no encontrada')
    expect(result.systemVoiceNameCollision).toBe(true)
  })
})

describe('idiomas', () => {
  it('devuelve el catálogo con el español por defecto', async () => {
    const catalog = {
      languages: [
        { code: 'es', name: 'Español' },
        { code: 'en', name: 'English' },
      ],
      defaultLanguage: { code: 'es', name: 'Español' },
    }
    fetchMock.mockResolvedValue(ok(catalog))

    const result = await getLanguages()

    expect(lastCall().url).toBe(`${BASE}/languages`)
    expect(result.defaultLanguage).toEqual({ code: 'es', name: 'Español' })
  })
})

describe('subida', () => {
  it('envía multipart con los cuatro campos y sin Content-Type JSON', async () => {
    fetchMock.mockResolvedValue(ok({ id: 9, name: 'Papá', language: 'es', system: false }, 201))
    const file = new File(['audio'], 'voz.wav', { type: 'audio/wav' })

    const result = await uploadVoice({ name: 'Papá', language: 'es', permission: true, file })

    const { url, init } = lastCall()
    expect(url).toBe(BASE)
    expect(init.method).toBe('POST')
    expect(init.body).toBeInstanceOf(FormData)
    const form = init.body as FormData
    expect(form.get('name')).toBe('Papá')
    expect(form.get('language')).toBe('es')
    expect(form.get('permission')).toBe('true')
    expect((form.get('file') as File).name).toBe('voz.wav')
    expect(init.headers['Content-Type']).toBeUndefined()
    expect(result).toEqual({ id: 9, name: 'Papá', language: 'es', system: false })
  })

  it.each([
    [400, 'NAME_REQUIRED'],
    [400, 'LANGUAGE_REQUIRED'],
    [400, 'PERMISSION_REQUIRED'],
    [400, 'INVALID_FORMAT'],
    [400, 'INVALID_DURATION'],
    [409, 'NAME_DUPLICATED'],
  ])('rechazo %i %s llega con su motivo', async (status, code) => {
    fetchMock.mockResolvedValue(rejected(status, code, `texto ${code}`))
    const file = new File(['x'], 'v.wav')

    const error = await uploadVoice({ name: 'x', language: 'es', permission: false, file }).catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(status)
    expect(error.code).toBe(code)
    expect(error.message).toBe(`texto ${code}`)
  })
})

describe('borrado', () => {
  it('borra una voz subida por id', async () => {
    fetchMock.mockResolvedValue(ok(null))

    await deleteVoice(3)

    expect(lastCall().url).toBe(`${BASE}/3`)
    expect(lastCall().init.method).toBe('DELETE')
  })

  it('rechaza borrar la voz del sistema con su motivo', async () => {
    fetchMock.mockResolvedValue(rejected(409, 'SYSTEM_VOICE_NOT_DELETABLE', 'La voz del sistema no se puede borrar'))

    const error = await deleteVoice('system').catch((e) => e)

    expect(lastCall().url).toBe(`${BASE}/system`)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.code).toBe('SYSTEM_VOICE_NOT_DELETABLE')
  })

  it('voz inexistente llega como VOICE_NOT_FOUND', async () => {
    fetchMock.mockResolvedValue(rejected(404, 'VOICE_NOT_FOUND', 'No existe'))

    const error = await deleteVoice(99).catch((e) => e)

    expect(error.status).toBe(404)
    expect(error.code).toBe('VOICE_NOT_FOUND')
  })
})

describe('elegibles', () => {
  it('devuelve solo las voces que entrega el servidor', async () => {
    const eligible = [
      { name: 'Sistema', language: 'es', system: true },
      { id: 3, name: 'Mamá', language: 'es', system: false },
    ]
    fetchMock.mockResolvedValue(ok(eligible))

    const result = await listEligibleVoices()

    expect(lastCall().url).toBe(`${BASE}/eligible`)
    expect(result).toEqual(eligible)
    expect(result.every((v) => v.language === 'es')).toBe(true)
  })
})
