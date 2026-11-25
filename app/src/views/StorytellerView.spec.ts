import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ApiError } from '../api/httpClient'
import StorytellerView from './StorytellerView.vue'
import * as storytellerApi from '../api/storyteller'
import * as exportsApi from '../api/storyExports'

vi.mock('../api/storyteller', () => ({
  getStories: vi.fn(),
  getStory: vi.fn(),
  deleteStory: vi.fn(),
}))
vi.mock('../api/storyExports', () => ({
  exportNubiGame: vi.fn(),
  exportVisualAgent: vi.fn(),
}))

const stories = vi.mocked(storytellerApi)
const exportsMock = vi.mocked(exportsApi)

const story = { id: 7, cover: null, title: 'La luna', synopsis: 's', actors: 1, size: 2, pages: 2 }

async function mountView() {
  const wrapper = mount(StorytellerView, {
    global: { stubs: { PathImage: true, TopicSelectionModal: true, StoryModal: true, ConfirmModal: true } },
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.resetAllMocks()
  setActivePinia(createPinia())
  stories.getStories.mockResolvedValue({ items: [story], pageSize: 10, currentPage: 0, totalPages: 1, totalItems: 1 })
  URL.createObjectURL = vi.fn(() => 'blob:x')
  URL.revokeObjectURL = vi.fn()
})

describe('StorytellerView exportaciones', () => {
  it('ofrece una única acción para Nubi Game y una para el agente visual, sin descarga heredada', async () => {
    const wrapper = await mountView()
    expect(wrapper.findAll('[data-export="nubi-game"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-export="visual-agent"]')).toHaveLength(1)
    expect(wrapper.find('[aria-label="Descargar cuento"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Exportar para Nubi Game')
    expect(wrapper.text()).toContain('Exportar para agente visual')
  })

  it('descarga el zip con el nombre recibido y revoca la URL', async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const blob = new Blob(['zip'])
    exportsMock.exportNubiGame.mockResolvedValue({ kind: 'download', blob, filename: 'luna-nubi.zip' })
    const wrapper = await mountView()

    await wrapper.find('[data-export="nubi-game"]').trigger('click')
    await flushPromises()

    expect(exportsMock.exportNubiGame).toHaveBeenCalledWith(7)
    expect(URL.createObjectURL).toHaveBeenCalledWith(blob)
    expect(click).toHaveBeenCalledTimes(1)
    expect(click.mock.contexts[0]).toMatchObject({ download: 'luna-nubi.zip' })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x')
    expect(wrapper.find('.export-rejection').exists()).toBe(false)
    click.mockRestore()
  })

  it('la acción visual llama a la exportación visual', async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    exportsMock.exportVisualAgent.mockResolvedValue({ kind: 'download', blob: new Blob(['z']), filename: 'v.zip' })
    const wrapper = await mountView()

    await wrapper.find('[data-export="visual-agent"]').trigger('click')
    await flushPromises()

    expect(exportsMock.exportVisualAgent).toHaveBeenCalledWith(7)
    expect(exportsMock.exportNubiGame).not.toHaveBeenCalled()
    click.mockRestore()
  })

  it('ante un rechazo muestra todas las faltas con su contexto y no descarga', async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    exportsMock.exportNubiGame.mockResolvedValue({
      kind: 'rejected',
      message: 'El cuento no se puede exportar',
      faults: [
        { page: 0, missing: 'IMAGE', message: 'Falta la imagen', code: null },
        { page: 2, missing: 'AUDIO', message: 'Falta el audio', code: null },
        { page: -1, missing: 'SCRIPT_NOT_APPROVED', message: 'Guion sin aprobar', code: null },
      ],
    })
    const wrapper = await mountView()

    await wrapper.find('[data-export="nubi-game"]').trigger('click')
    await flushPromises()

    const faults = wrapper.findAll('.export-fault')
    expect(faults).toHaveLength(3)
    expect(faults[0].text()).toContain('Portada')
    expect(faults[0].text()).toContain('Falta la imagen')
    expect(faults[1].text()).toContain('Página 2')
    expect(faults[2].text()).toContain('Cuento')
    expect(wrapper.text()).toContain('El cuento no se puede exportar')
    expect(URL.createObjectURL).not.toHaveBeenCalled()
    expect(click).not.toHaveBeenCalled()
    click.mockRestore()
  })

  it('un error de la API no descarga ni deja un rechazo previo', async () => {
    exportsMock.exportVisualAgent.mockRejectedValue(new ApiError('Cuento no encontrado', 404))
    const wrapper = await mountView()

    await wrapper.find('[data-export="visual-agent"]').trigger('click')
    await flushPromises()

    expect(URL.createObjectURL).not.toHaveBeenCalled()
    expect(wrapper.find('.export-rejection').exists()).toBe(false)
  })
})
