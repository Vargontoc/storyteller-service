import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getStoryPages } from './storyteller'

const fetchMock = vi.fn()

function ok(data: unknown) {
  return new Response(JSON.stringify({ success: true, data }), { status: 200 })
}

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('getStoryPages', () => {
  it('cuento antiguo: devuelve la lista tal cual', async () => {
    const list = [{ id: 1, page: 0, text: 't', scene: 's', image: null, audio: null }]
    fetchMock.mockResolvedValue(ok(list))

    expect(await getStoryPages(7)).toEqual(list)
  })

  it('cuento con escaleta: la respuesta objeto no es una lista y se normaliza a []', async () => {
    fetchMock.mockResolvedValue(
      ok({ storyId: 7, status: 'SCRIPT_ACCEPTED', generating: false, nextPage: 0, pages: [] }),
    )

    const result = await getStoryPages(7)

    expect(result).toEqual([])
    expect(result.some((p) => p.page === 0)).toBe(false)
  })
})
