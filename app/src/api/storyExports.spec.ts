import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './httpClient'
import { exportNubiGame, exportVisualAgent } from './storyExports'

const BASE = 'http://localhost:8080/api/v1/storyteller/7/export'
const fetchMock = vi.fn()

function zip(filename: string) {
  return new Response('PK', {
    status: 200,
    headers: {
      'Content-Type': 'application/octet-stream',
      'Content-Disposition': `attachment; filename="${filename}"`,
    },
  })
}

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('descarga', () => {
  it('entrega el zip de Nubi Game con su nombre', async () => {
    fetchMock.mockResolvedValue(zip('story-7.zip'))

    const result = await exportNubiGame(7)

    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE}/nubi-game`)
    expect(fetchMock.mock.calls[0][1].method).toBe('GET')
    expect(result.kind).toBe('download')
    if (result.kind === 'download') {
      expect(result.filename).toBe('story-7.zip')
      expect(result.blob.size).toBe(2)
    }
  })

  it('entrega el zip del agente visual con su nombre', async () => {
    fetchMock.mockResolvedValue(zip('story-7-visual-agent.zip'))

    const result = await exportVisualAgent(7)

    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE}/visual-agent`)
    expect(result).toMatchObject({ kind: 'download', filename: 'story-7-visual-agent.zip' })
  })
})

describe('rechazo', () => {
  const faults = [
    { page: 0, missing: 'COVER_NOT_ACCEPTED', message: 'Portada sin aceptar', code: null },
    { page: 3, missing: 'IMAGE', message: 'Falta la imagen', code: null },
    { page: 3, missing: 'AUDIO', message: 'Falta el audio', code: null },
    { page: -1, missing: 'PROTAGONIST_MISSING', message: 'Sin protagonista', code: 'RULE_1' },
  ]

  it('conserva todas las faltas con su portada o página', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ success: false, message: 'El cuento no se puede exportar', data: { faults } }),
        { status: 409 },
      ),
    )

    const result = await exportVisualAgent(7)

    expect(result).toEqual({ kind: 'rejected', message: 'El cuento no se puede exportar', faults })
  })

  it('propaga el 404 como ApiError', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ success: false, message: 'Cuento no encontrado' }), { status: 404 }),
    )

    await expect(exportNubiGame(7)).rejects.toMatchObject({
      constructor: ApiError,
      status: 404,
      message: 'Cuento no encontrado',
    })
  })
})
