import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { approveStoryScript, editStoryBeat, getStoryScript } from './storyScript'

const fetchMock = vi.fn()

function ok(data: unknown) {
  return new Response(JSON.stringify({ success: true, data }), { status: 200 })
}

function lastCall() {
  const [url, init] = fetchMock.mock.calls[fetchMock.mock.calls.length - 1]
  return { url: url as string, init }
}

const beat = { event: 'e', narrativeFunction: 'f', participants: ['Nubi'], tone: 't', visualNote: 'v' }

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('guion', () => {
  it('expone setting por beat y el planteamiento de portada', async () => {
    fetchMock.mockResolvedValue(
      ok({
        id: 7,
        beats: [{ page: 1, ...beat, setting: 'Bosque' }],
        cover: { setting: 'Claro', tone: 'cálido' },
      }),
    )
    const script = await getStoryScript(7)
    expect(script.beats[0].setting).toBe('Bosque')
    expect(script.cover).toEqual({ setting: 'Claro', tone: 'cálido' })
  })

  it('un cuento anterior deja los campos ausentes o null sin error', async () => {
    fetchMock.mockResolvedValue(ok({ id: 7, beats: [{ page: 1, ...beat, setting: null }], cover: null }))
    const script = await getStoryScript(7)
    expect(script.beats[0].setting).toBeNull()
    expect(script.cover).toBeNull()

    fetchMock.mockResolvedValue(ok({ id: 8, beats: [{ page: 1, ...beat }] }))
    const old = await getStoryScript(8)
    expect(old.beats[0].setting).toBeUndefined()
    expect(old.cover).toBeUndefined()
  })
})

describe('edición de beat', () => {
  it('envía setting en el cuerpo', async () => {
    fetchMock.mockResolvedValue(ok({ status: 'APPLIED', reason: null, invalidatedPages: 0, story: null }))
    await editStoryBeat(7, 2, { ...beat, setting: 'Playa' })
    const { url, init } = lastCall()
    expect(url).toContain('/generations/story/7/beat/2')
    expect(init.method).toBe('PUT')
    expect(JSON.parse(init.body).setting).toBe('Playa')
  })
})

describe('aprobación', () => {
  it('distingue portada iniciada y no iniciada', async () => {
    fetchMock.mockResolvedValue(ok({ story: { id: 7 }, coverStarted: true }))
    expect((await approveStoryScript(7)).coverStarted).toBe(true)

    fetchMock.mockResolvedValue(ok({ story: { id: 7 }, coverStarted: false }))
    const result = await approveStoryScript(7)
    expect(result.coverStarted).toBe(false)
    expect(result.story.id).toBe(7)
  })
})
