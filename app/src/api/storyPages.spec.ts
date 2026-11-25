import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './httpClient'
import {
  acceptPage,
  acceptProposal,
  discardProposals,
  generateCover,
  generatePage,
  getAcceptedImage,
  getPageDetail,
  getStoryPagesView,
  regenerate,
  selectVersion,
  sendHint,
  uploadPageImage,
} from './storyPages'

const BASE = 'http://localhost:8080/api/v1/storyteller/7'

const fetchMock = vi.fn()

function ok(data: unknown, status = 200) {
  return new Response(JSON.stringify({ success: true, data }), { status })
}

function rejected(status: number, message: string) {
  return new Response(JSON.stringify({ success: false, message }), { status })
}

// Petición enviada en la última llamada: url, método y cuerpo.
function lastCall() {
  const [url, init] = fetchMock.mock.calls[fetchMock.mock.calls.length - 1]
  return { url: url as string, method: init.method as string, body: init.body as string | undefined }
}

beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('consultas', () => {
  it('lee el estado con portada y páginas', async () => {
    const view = { storyId: 7, status: 'SCRIPT_ACCEPTED', generating: false, nextPage: 0, pages: [] }
    fetchMock.mockResolvedValue(ok(view))

    const result = await getStoryPagesView(7)

    expect(lastCall()).toEqual({ url: `${BASE}/pages`, method: 'GET', body: undefined })
    expect(result).toEqual(view)
  })

  it('lee el detalle de una página por id', async () => {
    const detail = {
      id: 21,
      page: 0,
      status: 'ACCEPTED',
      beatId: null,
      title: 'La luna',
      accepted: { text: null, scene: 'Luna sonriente', hasImage: true },
      versions: [],
      proposals: [],
      actions: ['HINT'],
    }
    fetchMock.mockResolvedValue(ok(detail))

    const result = await getPageDetail(7, 21)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/21`, method: 'GET', body: undefined })
    expect(result.accepted?.hasImage).toBe(true)
    expect(result.title).toBe('La luna')
  })

  it('cuento narrado: hasAudio global e interior, portada sin audio', async () => {
    const view = {
      storyId: 7,
      status: 'PAGES_ACCEPTED',
      generating: false,
      nextPage: null,
      hasAudio: true,
      narrating: false,
      pages: [
        { id: 21, page: 0, hasAudio: false },
        { id: 22, page: 1, hasAudio: true },
      ],
    }
    fetchMock.mockResolvedValue(ok(view))

    const result = await getStoryPagesView(7)

    expect(result.hasAudio).toBe(true)
    expect(result.narrating).toBe(false)
    expect(result.pages[0]!.hasAudio).toBe(false)
    expect(result.pages[1]!.hasAudio).toBe(true)
  })

  it('cuento narrándose: narrating', async () => {
    fetchMock.mockResolvedValue(
      ok({ storyId: 7, status: 'PAGES_ACCEPTED', generating: false, nextPage: null, hasAudio: false, narrating: true, pages: [] }),
    )

    const result = await getStoryPagesView(7)

    expect(result.narrating).toBe(true)
    expect(result.hasAudio).toBe(false)
  })

  it('el detalle de una página interior indica su audio', async () => {
    fetchMock.mockResolvedValue(ok({ id: 22, page: 1, hasAudio: true }))

    const result = await getPageDetail(7, 22)

    expect(result.hasAudio).toBe(true)
  })

  it('obtiene la imagen aceptada como blob', async () => {
    fetchMock.mockResolvedValue(new Response('png', { status: 200 }))

    const blob = await getAcceptedImage(7, 21)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/21/image`, method: 'GET', body: undefined })
    expect(blob.size).toBe(3)
  })
})

describe('generación asíncrona', () => {
  it('pide la portada', async () => {
    fetchMock.mockResolvedValue(ok({ storyId: 7, pageNumber: 0 }, 202))

    const result = await generateCover(7)

    expect(lastCall()).toEqual({ url: `${BASE}/cover/generate`, method: 'POST', body: undefined })
    expect(result).toEqual({ storyId: 7, pageNumber: 0 })
  })

  it('pide la página por su número', async () => {
    fetchMock.mockResolvedValue(ok({ storyId: 7, pageNumber: 3 }, 202))

    const result = await generatePage(7, 3)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/3/generate`, method: 'POST', body: undefined })
    expect(result.pageNumber).toBe(3)
  })

  it('envía el hint con el id de la página', async () => {
    fetchMock.mockResolvedValue(ok({ storyId: 7, pageNumber: 2 }, 202))

    await sendHint(7, 22, 'más corto')

    expect(lastCall()).toEqual({
      url: `${BASE}/pages/22/hint`,
      method: 'POST',
      body: JSON.stringify({ hint: 'más corto' }),
    })
  })

  it('regenera sin hint', async () => {
    fetchMock.mockResolvedValue(ok({ storyId: 7, pageNumber: 2 }, 202))

    await regenerate(7, 22)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/22/regenerate`, method: 'POST', body: undefined })
  })
})

describe('mutaciones síncronas', () => {
  it('marca una versión como vigente', async () => {
    const data = { storyId: 7, pageId: 22, versionId: 5, warning: 'TOO_MANY_WORDS' }
    fetchMock.mockResolvedValue(ok(data))

    const result = await selectVersion(7, 22, 5)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/22/versions/5/select`, method: 'POST', body: undefined })
    expect(result).toEqual(data)
  })

  it('acepta una página pendiente', async () => {
    const data = { storyId: 7, pageId: 22, pageNumber: 2, status: 'ACCEPTED' }
    fetchMock.mockResolvedValue(ok(data))

    const result = await acceptPage(7, 22)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/22/accept`, method: 'POST', body: undefined })
    expect(result.status).toBe('ACCEPTED')
  })

  it('aceptar una propuesta sin confirmar expone las páginas afectadas', async () => {
    const data = {
      storyId: 7,
      pageId: 22,
      pageNumber: 2,
      status: 'ACCEPTED',
      requiresConfirmation: true,
      affectedPages: 2,
    }
    fetchMock.mockResolvedValue(ok(data))

    const result = await acceptProposal(7, 22)

    expect(lastCall()).toEqual({
      url: `${BASE}/pages/22/accept-proposal?confirm=false`,
      method: 'POST',
      body: undefined,
    })
    expect(result.requiresConfirmation).toBe(true)
    expect(result.affectedPages).toBe(2)
  })

  it('aceptar una propuesta confirmada envía confirm=true', async () => {
    const data = {
      storyId: 7,
      pageId: 22,
      pageNumber: 2,
      status: 'ACCEPTED',
      requiresConfirmation: false,
      affectedPages: 0,
    }
    fetchMock.mockResolvedValue(ok(data))

    const result = await acceptProposal(7, 22, true)

    expect(lastCall().url).toBe(`${BASE}/pages/22/accept-proposal?confirm=true`)
    expect(result.requiresConfirmation).toBe(false)
  })

  it('descarta las propuestas', async () => {
    const data = { storyId: 7, pageId: 22, status: 'ACCEPTED' }
    fetchMock.mockResolvedValue(ok(data))

    const result = await discardProposals(7, 22)

    expect(lastCall()).toEqual({ url: `${BASE}/pages/22/proposals/discard`, method: 'POST', body: undefined })
    expect(result).toEqual(data)
  })
})

describe('imagen subida y hint de portada', () => {
  it('sube el fichero a la página 3 como multipart sin fijar Content-Type', async () => {
    const data = { storyId: 7, pageId: 24, pageNumber: 3, status: 'ACCEPTED' }
    fetchMock.mockResolvedValue(ok(data))
    const file = new File(['png'], 'foto.png', { type: 'image/png' })

    const result = await uploadPageImage(7, 24, file)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe(`${BASE}/pages/24/image`)
    expect(init.method).toBe('POST')
    expect(init.body).toBeInstanceOf(FormData)
    expect((init.body as FormData).get('file')).toBe(file)
    expect(new Headers(init.headers).has('Content-Type')).toBe(false)
    expect(result).toEqual(data)
  })

  it('envía el hint de la portada con el id de la página 0', async () => {
    fetchMock.mockResolvedValue(ok({ storyId: 7, pageNumber: 0 }, 202))

    const result = await sendHint(7, 21, 'que se vea la luna llena')

    expect(lastCall()).toEqual({
      url: `${BASE}/pages/21/hint`,
      method: 'POST',
      body: JSON.stringify({ hint: 'que se vea la luna llena' }),
    })
    expect(result.pageNumber).toBe(0)
  })

  async function expectRejection(call: () => Promise<unknown>, status: number, message: string) {
    fetchMock.mockResolvedValue(rejected(status, message))
    const error = await call().catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe(message)
    expect((error as ApiError).status).toBe(status)
  }

  it('rechazo: el fichero no es una imagen', () =>
    expectRejection(
      () => uploadPageImage(7, 24, new File(['hola'], 'nota.txt', { type: 'text/plain' })),
      415,
      'El fichero no es una imagen',
    ))

  it('rechazo: subida a portada o página sin aceptar', () =>
    expectRejection(
      () => uploadPageImage(7, 21, new File(['png'], 'a.png', { type: 'image/png' })),
      409,
      'La página no está aceptada',
    ))

  it('rechazo: hint de portada sin planteamiento de portada', () =>
    expectRejection(
      () => sendHint(7, 21, 'luna'),
      409,
      'Al guion le faltan el lugar y el tono de la portada',
    ))
})

describe('rechazos del servidor', () => {
  async function expectRejection(call: () => Promise<unknown>, status: number, message: string) {
    fetchMock.mockResolvedValue(rejected(status, message))
    const error = await call().catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe(message)
    expect((error as ApiError).status).toBe(status)
  }

  it('generación en curso', () =>
    expectRejection(() => generatePage(7, 2), 409, 'Hay una generación en curso'))

  it('predecesor sin aceptar', () =>
    expectRejection(() => generatePage(7, 3), 409, 'Primero hay que aceptar la página 2'))

  it('hint vacío', () => expectRejection(() => sendHint(7, 22, ''), 400, 'El hint es obligatorio'))

  it('aviso de frases al aceptar', () =>
    expectRejection(
      () => acceptPage(7, 22),
      409,
      'La página no cumple el número de frases: regenérala antes de aceptar',
    ))

  it('narración en curso al reabrir o cambiar', async () => {
    const message = 'El cuento se está narrando: no se pueden reabrir, cambiar ni eliminar sus páginas'
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ success: false, message, errors: ['IN_PROGRESS'] }), { status: 409 }),
    )

    for (const call of [
      () => acceptProposal(7, 22, true),
      () => selectVersion(7, 22, 5),
      () => sendHint(7, 22, 'más corto'),
      () => regenerate(7, 22),
    ]) {
      const error = await call().catch((e: unknown) => e)
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).message).toBe(message)
      expect((error as ApiError).status).toBe(409)
    }
  })

  it('cuento completo', () =>
    expectRejection(() => generatePage(7, 9), 409, 'El cuento ya tiene todas las páginas aceptadas'))
})
