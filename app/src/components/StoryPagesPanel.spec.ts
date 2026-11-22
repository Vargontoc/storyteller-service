import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import StoryPagesPanel from './StoryPagesPanel.vue'
import { useWebsocketStore } from '../stores/websocket'
import { ApiError } from '../api/httpClient'
import * as api from '../api/storyPages'
import type { PageDetail, PageVersionView, StoryPagesView } from '../api/storyPages'

vi.mock('../api/storyPages')

const mocked = vi.mocked(api)

function page(overrides: Partial<PageDetail> = {}): PageDetail {
  return {
    id: 10,
    page: 1,
    status: 'ACCEPTED',
    beatId: 1,
    title: null,
    accepted: { text: 'Había una vez', scene: 'Un bosque', hasImage: false },
    versions: [],
    proposals: [],
    actions: ['HINT'],
    ...overrides,
  }
}

function view(overrides: Partial<StoryPagesView> = {}): StoryPagesView {
  return { storyId: 7, status: 'SCRIPT_ACCEPTED', generating: false, nextPage: null, pages: [], ...overrides }
}

let pinia: Pinia

async function mountPanel(v: StoryPagesView) {
  mocked.getStoryPagesView.mockResolvedValue(v)
  const wrapper = mount(StoryPagesPanel, { props: { storyId: 7 }, global: { plugins: [pinia], stubs: { teleport: true } } })
  await flushPromises()
  return wrapper
}

function buttonByText(wrapper: ReturnType<typeof mount>, text: string) {
  return wrapper.findAll('button').find((b) => b.text().includes(text))
}

beforeEach(() => {
  pinia = createPinia()
  setActivePinia(pinia)
  vi.resetAllMocks()
  URL.createObjectURL = vi.fn(() => 'blob:img')
  URL.revokeObjectURL = vi.fn()
  mocked.getAcceptedImage.mockResolvedValue(new Blob(['x']))
})

afterEach(() => vi.restoreAllMocks())

describe('StoryPagesPanel', () => {
  it('muestra página aceptada con imagen, texto y escena', async () => {
    const wrapper = await mountPanel(view({ pages: [page({ accepted: { text: 'Hola', scene: 'Un bosque', hasImage: true } })] }))

    expect(mocked.getAcceptedImage).toHaveBeenCalledWith(7, 10)
    expect(wrapper.find('img').attributes('src')).toBe('blob:img')
    expect(wrapper.text()).toContain('Hola')
    expect(wrapper.text()).toContain('Un bosque')
    expect(wrapper.text()).toContain('Aceptada')
  })

  it('muestra portada aceptada con imagen, título y escena', async () => {
    const cover = page({ id: 1, page: 0, title: 'Mi cuento', accepted: { text: null, scene: 'Portada en el mar', hasImage: true } })
    const wrapper = await mountPanel(view({ pages: [cover] }))

    expect(wrapper.text()).toContain('Portada')
    expect(wrapper.text()).toContain('Mi cuento')
    expect(wrapper.text()).toContain('Portada en el mar')
    expect(wrapper.find('img').exists()).toBe(true)
  })

  it('muestra página aceptada sin imagen sin pedirla', async () => {
    const wrapper = await mountPanel(view({ pages: [page()] }))

    expect(mocked.getAcceptedImage).not.toHaveBeenCalled()
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('Había una vez')
  })

  it('muestra página pendiente con aceptar y hint, y acepta recargando', async () => {
    const pending = page({ status: 'PENDING', accepted: null, actions: ['ACCEPT', 'HINT'] })
    const wrapper = await mountPanel(view({ pages: [pending] }))
    mocked.acceptPage.mockResolvedValue({ storyId: 7, pageId: 10, pageNumber: 1, status: 'ACCEPTED' })

    expect(wrapper.text()).toContain('Pendiente')
    expect(buttonByText(wrapper, 'Enviar hint')).toBeDefined()

    await buttonByText(wrapper, 'Aceptar')!.trigger('click')
    await flushPromises()

    expect(mocked.acceptPage).toHaveBeenCalledWith(7, 10)
    expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
    expect(mocked.generatePage).not.toHaveBeenCalled()
  })

  it('página aceptada sin propuestas: hint y sin aceptar ni histórico', async () => {
    const wrapper = await mountPanel(view({ pages: [page()] }))

    expect(buttonByText(wrapper, 'Enviar hint')).toBeDefined()
    expect(buttonByText(wrapper, 'Aceptar')).toBeUndefined()
    expect(wrapper.findAll('.version')).toHaveLength(0)
  })

  it('ofrece generar la siguiente página solo si nextPage lo indica', async () => {
    const wrapper = await mountPanel(view({ nextPage: 2, pages: [page()] }))
    mocked.generatePage.mockResolvedValue({ storyId: 7, pageNumber: 2 })

    await buttonByText(wrapper, 'Generar página 2')!.trigger('click')
    await flushPromises()

    expect(mocked.generatePage).toHaveBeenCalledWith(7, 2)
  })

  it('ofrece generar la portada con nextPage 0', async () => {
    const wrapper = await mountPanel(view({ nextPage: 0 }))
    mocked.generateCover.mockResolvedValue({ storyId: 7, pageNumber: 0 })

    await buttonByText(wrapper, 'Generar portada')!.trigger('click')

    expect(mocked.generateCover).toHaveBeenCalledWith(7)
  })

  it('cuento completo: no ofrece generar', async () => {
    const wrapper = await mountPanel(view({ status: 'PAGES_ACCEPTED', nextPage: null, pages: [page()] }))

    expect(buttonByText(wrapper, 'Generar')).toBeUndefined()
    expect(wrapper.text()).toContain('Cuento completo')
  })

  it('no hay campos editables para texto ni escena', async () => {
    const wrapper = await mountPanel(view({ pages: [page({ actions: [] })] }))

    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.find('[contenteditable]').exists()).toBe(false)

    const withHint = await mountPanel(view({ pages: [page()] }))
    // El único campo es el del hint, vacío: nunca el texto ni la escena.
    const values = withHint.findAll('textarea').map((t) => (t.element as HTMLTextAreaElement).value)
    expect(values).toEqual([''])
  })

  it('hint vacío no se envía y avisa', async () => {
    const wrapper = await mountPanel(view({ pages: [page()] }))
    await wrapper.find('textarea').setValue('   ')

    await buttonByText(wrapper, 'Enviar hint')!.trigger('click')

    expect(mocked.sendHint).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('El hint es obligatorio')
  })

  it('envía el hint recortado', async () => {
    const wrapper = await mountPanel(view({ pages: [page()] }))
    mocked.sendHint.mockResolvedValue({ storyId: 7, pageNumber: 1 })
    await wrapper.find('textarea').setValue('  más corto  ')

    await buttonByText(wrapper, 'Enviar hint')!.trigger('click')
    await flushPromises()

    expect(mocked.sendHint).toHaveBeenCalledWith(7, 10, 'más corto')
  })

  it('muestra el motivo cuando el servidor rechaza', async () => {
    const wrapper = await mountPanel(view({ nextPage: 2, pages: [page()] }))
    mocked.generatePage.mockRejectedValue(new ApiError('Hay una generación en curso', 409))

    await buttonByText(wrapper, 'Generar página 2')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Hay una generación en curso')
  })

  describe('histórico y avisos', () => {
    function versions(n: number, currentIndex = 0, warnings: Record<number, PageVersionView['warning']> = {}): PageVersionView[] {
      return Array.from({ length: n }, (_, i) => ({
        id: 100 + i,
        text: `Texto ${i + 1}`,
        scene: `Escena ${i + 1}`,
        warning: warnings[i] ?? 'NONE',
        current: i === currentIndex,
      }))
    }

    function pending(vs: PageVersionView[], actions: PageDetail['actions']) {
      return page({ status: 'PENDING', accepted: null, versions: vs, actions })
    }

    it('muestra todas las versiones con su aviso, la vigente destacada y selección en las demás', async () => {
      const vs = versions(3, 1, { 0: 'TOO_MANY_WORDS', 2: 'BOTH' })
      const wrapper = await mountPanel(view({ pages: [pending(vs, ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))

      const items = wrapper.findAll('.version')
      expect(items).toHaveLength(3)
      expect(items.map((i) => i.classes('current'))).toEqual([false, true, false])
      expect(items[0].text()).toContain('Texto 1')
      expect(items[0].text()).toContain('demasiadas palabras')
      expect(items[1].text()).toContain('Vigente')
      expect(items[2].text()).toContain('número de frases')
      expect(items.map((i) => i.findAll('button').length)).toEqual([1, 0, 1])
    })

    it('muestra las 11 versiones recibidas', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(11), ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))

      expect(wrapper.findAll('.version')).toHaveLength(11)
      expect(wrapper.text()).toContain('Texto 11')
    })

    it('seleccionar otra versión llama al servidor y recarga', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(3), ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))
      mocked.selectVersion.mockResolvedValue({ storyId: 7, pageId: 10, versionId: 100, warning: 'NONE' })

      await wrapper.findAll('.version')[2].find('button').trigger('click')
      await flushPromises()

      expect(mocked.selectVersion).toHaveBeenCalledWith(7, 10, 102)
      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
    })

    it('portada pendiente con versiones sin texto muestra su escena', async () => {
      const cover = page({
        page: 0,
        status: 'PENDING',
        accepted: null,
        title: 'Mi cuento',
        versions: [{ id: 1, text: null, scene: 'Mar', warning: 'NONE', current: true }],
        actions: ['ACCEPT', 'HINT'],
      })
      const wrapper = await mountPanel(view({ pages: [cover] }))

      expect(wrapper.find('.version').text()).toContain('Mar')
    })

    it('aviso de palabras en la vigente: aceptar y hint, sin regenerar', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(2, 0, { 0: 'TOO_MANY_WORDS' }), ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))

      expect(wrapper.text()).toContain('demasiadas palabras')
      expect(buttonByText(wrapper, 'Aceptar')).toBeDefined()
      expect(buttonByText(wrapper, 'Enviar hint')).toBeDefined()
      expect(buttonByText(wrapper, 'Regenerar sin hint')).toBeUndefined()
    })

    it('aviso de frases en la vigente: hint y regenerar sin hint, sin aceptar', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(2, 0, { 0: 'SENTENCE_COUNT' }), ['HINT', 'REGENERATE', 'SELECT_VERSION'])] }))
      mocked.regenerate.mockResolvedValue({ storyId: 7, pageNumber: 1 })

      expect(buttonByText(wrapper, 'Aceptar')).toBeUndefined()
      expect(buttonByText(wrapper, 'Enviar hint')).toBeDefined()

      await buttonByText(wrapper, 'Regenerar sin hint')!.trigger('click')
      await flushPromises()

      expect(mocked.regenerate).toHaveBeenCalledWith(7, 10)
      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
    })

    it('aviso de frases en una versión no vigente: no hay regeneración', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(2, 0, { 1: 'SENTENCE_COUNT' }), ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))

      expect(wrapper.text()).toContain('número de frases')
      expect(buttonByText(wrapper, 'Regenerar sin hint')).toBeUndefined()
      expect(buttonByText(wrapper, 'Aceptar')).toBeDefined()
    })

    it('el histórico no muestra imágenes ni las pide', async () => {
      const wrapper = await mountPanel(view({ pages: [pending(versions(11), ['ACCEPT', 'HINT', 'SELECT_VERSION'])] }))

      expect(wrapper.find('img').exists()).toBe(false)
      expect(mocked.getAcceptedImage).not.toHaveBeenCalled()
    })
  })

  describe('reapertura y descarte en cascada', () => {
    const proposals: PageVersionView[] = [
      { id: 200, text: 'Propuesta 1', scene: 'Escena P1', warning: 'NONE', current: true },
      { id: 201, text: 'Propuesta 2', scene: 'Escena P2', warning: 'NONE', current: false },
    ]
    const reopenedActions: PageDetail['actions'] = ['HINT', 'SELECT_VERSION', 'ACCEPT_PROPOSAL', 'DISCARD_PROPOSALS']

    function reopened(overrides: Partial<PageDetail> = {}) {
      return page({ id: 20, page: 2, proposals, actions: reopenedActions, ...overrides })
    }

    function accepted(pageId: number, pageNumber: number) {
      return { storyId: 7, pageId, pageNumber, status: 'ACCEPTED' as const }
    }

    it('separa la aceptada de las propuestas, con la acción de descartar', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))

      expect(wrapper.text()).toContain('Había una vez')
      expect(wrapper.text()).toContain('Propuestas')
      const items = wrapper.findAll('.version')
      expect(items).toHaveLength(2)
      expect(items[0].text()).toContain('Propuesta 1')
      expect(items[0].text()).toContain('Vigente')
      expect(items.some((i) => i.text().includes('Había una vez'))).toBe(false)
      expect(buttonByText(wrapper, 'Descartar propuestas')).toBeDefined()
    })

    it('la aceptada muestra su imagen y las propuestas no', async () => {
      const withImage = reopened({ accepted: { text: 'Aceptada', scene: 'Bosque', hasImage: true } })
      const wrapper = await mountPanel(view({ pages: [withImage] }))

      expect(mocked.getAcceptedImage).toHaveBeenCalledTimes(1)
      expect(wrapper.findAll('img')).toHaveLength(1)
      expect(wrapper.find('.version img').exists()).toBe(false)
    })

    it('solo se puede marcar como vigente entre las propuestas', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))
      mocked.selectVersion.mockResolvedValue({ storyId: 7, pageId: 20, versionId: 201, warning: 'NONE' })

      const markers = wrapper.findAll('button').filter((b) => b.text().includes('Marcar como vigente'))
      expect(markers).toHaveLength(1)

      await markers[0].trigger('click')
      await flushPromises()

      expect(mocked.selectVersion).toHaveBeenCalledWith(7, 20, 201)
    })

    it('aceptar la propuesta de la página 2 con dos posteriores pide confirmación con el número', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))
      mocked.acceptProposal.mockResolvedValue({ ...accepted(20, 2), status: 'ACCEPTED', requiresConfirmation: true, affectedPages: 2 })

      await buttonByText(wrapper, 'Aceptar propuesta')!.trigger('click')
      await flushPromises()

      expect(mocked.acceptProposal).toHaveBeenCalledTimes(1)
      expect(mocked.acceptProposal).toHaveBeenCalledWith(7, 20, false)
      expect(wrapper.find('[role="alertdialog"]').text()).toContain('2 páginas posteriores')
    })

    it('cancelar no vuelve a llamar con confirm=true', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))
      mocked.acceptProposal.mockResolvedValue({ ...accepted(20, 2), requiresConfirmation: true, affectedPages: 2 })
      await buttonByText(wrapper, 'Aceptar propuesta')!.trigger('click')
      await flushPromises()

      await buttonByText(wrapper, 'Cancelar')!.trigger('click')
      await flushPromises()

      expect(mocked.acceptProposal).toHaveBeenCalledTimes(1)
      expect(mocked.acceptProposal).not.toHaveBeenCalledWith(7, 20, true)
      expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
      expect(wrapper.text()).toContain('Propuesta 2')
    })

    it('confirmar llama con confirm=true y recarga el estado', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))
      mocked.acceptProposal
        .mockResolvedValueOnce({ ...accepted(20, 2), requiresConfirmation: true, affectedPages: 2 })
        .mockResolvedValueOnce({ ...accepted(20, 2), requiresConfirmation: false, affectedPages: 2 })
      await buttonByText(wrapper, 'Aceptar propuesta')!.trigger('click')
      await flushPromises()
      const loadsBefore = mocked.getStoryPagesView.mock.calls.length
      mocked.getStoryPagesView.mockResolvedValue(view({ nextPage: 3, pages: [page({ id: 20, page: 2 })] }))

      await buttonByText(wrapper, 'Aceptar y eliminar')!.trigger('click')
      await flushPromises()

      expect(mocked.acceptProposal).toHaveBeenLastCalledWith(7, 20, true)
      expect(mocked.getStoryPagesView.mock.calls.length).toBe(loadsBefore + 1)
      expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
      expect(buttonByText(wrapper, 'Generar página 3')).toBeDefined()
    })

    it('portada sin posteriores: sin confirmación y recarga', async () => {
      const cover = page({ id: 1, page: 0, title: 'Mi cuento', accepted: { text: null, scene: 'Mar', hasImage: false }, proposals, actions: reopenedActions })
      const wrapper = await mountPanel(view({ pages: [cover] }))
      mocked.acceptProposal.mockResolvedValue({ ...accepted(1, 0), requiresConfirmation: false, affectedPages: 0 })

      await buttonByText(wrapper, 'Aceptar propuesta')!.trigger('click')
      await flushPromises()

      expect(mocked.acceptProposal).toHaveBeenCalledTimes(1)
      expect(mocked.acceptProposal).toHaveBeenCalledWith(7, 1, false)
      expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
    })

    it('descartar propuestas llama al servidor y recarga', async () => {
      const wrapper = await mountPanel(view({ pages: [reopened()] }))
      mocked.discardProposals.mockResolvedValue({ storyId: 7, pageId: 20, status: 'ACCEPTED' })
      mocked.getStoryPagesView.mockResolvedValue(view({ pages: [page({ id: 20, page: 2 })] }))

      await buttonByText(wrapper, 'Descartar propuestas')!.trigger('click')
      await flushPromises()

      expect(mocked.discardProposals).toHaveBeenCalledWith(7, 20)
      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
      expect(wrapper.findAll('.version')).toHaveLength(0)
      expect(wrapper.text()).toContain('Había una vez')
    })
  })

  describe('eventos de generación (WebSocket)', () => {
    let eventId = 0

    async function emit(type: 'STARTED' | 'VERSION_PRESENTED' | 'HINT_REJECTED' | 'FAILED', storyId = 7, pageNumber = 1, reason?: string) {
      useWebsocketStore(pinia).lastStoryPageEvent = { storyId, pageNumber, type, reason, id: ++eventId }
      await flushPromises()
    }

    it('versión presentada del cuento abierto recarga y muestra el resultado sin pulsar Recargar', async () => {
      const wrapper = await mountPanel(view({ nextPage: 1 }))
      expect(wrapper.text()).not.toContain('Texto nuevo')
      mocked.getStoryPagesView.mockResolvedValue(view({ pages: [page({ status: 'PENDING', accepted: null, versions: [{ id: 1, text: 'Texto nuevo', scene: 'Bosque', warning: 'NONE', current: true }], actions: ['ACCEPT', 'HINT'] })] }))

      await emit('VERSION_PRESENTED', 7, 1)

      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
      expect(wrapper.text()).toContain('Texto nuevo')
    })

    it('evento de otro cuento no recarga ni muestra nada', async () => {
      const wrapper = await mountPanel(view())

      await emit('VERSION_PRESENTED', 99, 1)
      await emit('FAILED', 99, 1, 'Ollama caído')
      await emit('STARTED', 99, 1)

      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(1)
      expect(wrapper.text()).not.toContain('Ollama caído')
      expect(wrapper.find('.operation').exists()).toBe(false)
    })

    it('STARTED muestra generación en curso sin tocar el estado del servidor', async () => {
      const wrapper = await mountPanel(view({ nextPage: 1, generating: false }))

      await emit('STARTED', 7, 1)

      expect(wrapper.find('.operation').text()).toContain('página 1')
      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(1)

      await emit('STARTED', 7, 0)
      expect(wrapper.find('.operation').text()).toContain('portada')
    })

    it('el indicador desaparece al presentarse la versión', async () => {
      const wrapper = await mountPanel(view())
      await emit('STARTED', 7, 1)

      await emit('VERSION_PRESENTED', 7, 1)

      expect(wrapper.find('.operation').exists()).toBe(false)
    })

    it('hint rechazado presenta el motivo y conserva el contenido', async () => {
      const wrapper = await mountPanel(view({ pages: [page()] }))

      await emit('HINT_REJECTED', 7, 1, 'El hint contradice la escaleta')

      expect(wrapper.find('.event-error').text()).toContain('El hint contradice la escaleta')
      expect(wrapper.text()).toContain('Había una vez')
      expect(wrapper.text()).toContain('Un bosque')
    })

    it('fallo muestra el error y permite reintentar tras sincronizar', async () => {
      const wrapper = await mountPanel(view({ nextPage: 1, generating: true }))
      expect(buttonByText(wrapper, 'Generar página 1')!.attributes('disabled')).toBeDefined()
      mocked.getStoryPagesView.mockResolvedValue(view({ nextPage: 1, generating: false }))
      mocked.generatePage.mockResolvedValue({ storyId: 7, pageNumber: 1 })

      await emit('FAILED', 7, 1, 'Ollama no responde')

      expect(wrapper.find('.event-error').text()).toContain('Ollama no responde')
      const retry = buttonByText(wrapper, 'Generar página 1')!
      expect(retry.attributes('disabled')).toBeUndefined()
      await retry.trigger('click')
      await flushPromises()
      expect(mocked.generatePage).toHaveBeenCalledWith(7, 1)
    })

    it('el error se limpia al iniciarse una nueva generación', async () => {
      const wrapper = await mountPanel(view())
      await emit('FAILED', 7, 1, 'Ollama no responde')

      await emit('STARTED', 7, 1)

      expect(wrapper.find('.event-error').exists()).toBe(false)
    })

    it('la reconexión recarga el panel completo', async () => {
      const wrapper = await mountPanel(view({ generating: true }))
      mocked.getStoryPagesView.mockResolvedValue(view({ generating: false, nextPage: 1 }))

      useWebsocketStore(pinia).connectionId += 1
      await flushPromises()

      expect(mocked.getStoryPagesView).toHaveBeenCalledTimes(2)
      expect(buttonByText(wrapper, 'Generar página 1')).toBeDefined()
      expect(wrapper.text()).not.toContain('Generación en curso')
    })
  })

  it('libera las URL de imagen al desmontar', async () => {
    const wrapper = await mountPanel(view({ pages: [page({ accepted: { text: 'a', scene: 'b', hasImage: true } })] }))

    wrapper.unmount()

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:img')
  })
})
