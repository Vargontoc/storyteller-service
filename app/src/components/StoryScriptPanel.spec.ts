import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import StoryScriptPanel from './StoryScriptPanel.vue'
import ConfirmModal from './ConfirmModal.vue'
import { ApiError } from '../api/httpClient'
import * as scriptApi from '../api/storyScript'
import * as reviews from '../api/reviews'
import type { BeatEditResult, StoryScript } from '../api/storyScript'

vi.mock('../api/storyScript')
vi.mock('../api/reviews')

const script: StoryScript = {
  id: 7,
  title: 'Cuento',
  synopsis: 'Sinopsis',
  status: 'SCRIPT_ACCEPTED',
  scriptVersion: 1,
  cast: [],
  beats: [
    { page: 1, event: 'Algo ocurre', narrativeFunction: 'inicio', participants: ['Luna'], tone: 'alegre', visualNote: 'bosque' },
  ],
}

function editResult(overrides: Partial<BeatEditResult>): BeatEditResult {
  return { status: 'CONFIRMATION_REQUIRED', reason: null, invalidatedPages: 2, story: null, ...overrides }
}

async function openEdit() {
  vi.mocked(scriptApi.getStoryScript).mockResolvedValue(script)
  vi.mocked(reviews.getPendingStoryReview).mockResolvedValue(null as never)
  const wrapper = mount(StoryScriptPanel, {
    props: { storyId: 7 },
    global: { stubs: { teleport: true } },
  })
  await flushPromises()
  await wrapper.find('.beat button').trigger('click') // Editar
  return wrapper
}

async function save(wrapper: Awaited<ReturnType<typeof openEdit>>) {
  await wrapper.findAll('.beat button').find((b) => b.text() === 'Guardar')!.trigger('click')
  await flushPromises()
}

const modalMessage = (wrapper: Awaited<ReturnType<typeof openEdit>>) =>
  wrapper.findComponent(ConfirmModal).props('message')

describe('StoryScriptPanel: aviso al editar un beat', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('con audioWillBeDeleted el aviso menciona el borrado del audio y la voz', async () => {
    const wrapper = await openEdit()
    vi.mocked(scriptApi.editStoryBeat).mockResolvedValue(editResult({ audioWillBeDeleted: true }))
    await save(wrapper)

    expect(wrapper.findComponent(ConfirmModal).props('open')).toBe(true)
    expect(modalMessage(wrapper)).toContain('2 página(s)')
    expect(modalMessage(wrapper)).toContain('Se borrará todo el audio de la narración y la voz')
  })

  it('sin audioWillBeDeleted (false o ausente) el aviso es el de siempre', async () => {
    const wrapper = await openEdit()
    vi.mocked(scriptApi.editStoryBeat).mockResolvedValue(editResult({ audioWillBeDeleted: false }))
    await save(wrapper)
    expect(modalMessage(wrapper)).toBe('Editar este beat eliminará 2 página(s) ya generadas. ¿Continuar?')

    vi.mocked(scriptApi.editStoryBeat).mockResolvedValue(editResult({}))
    await save(wrapper)
    expect(modalMessage(wrapper)).toBe('Editar este beat eliminará 2 página(s) ya generadas. ¿Continuar?')
  })

  it('al confirmar con éxito recarga el guion y emite changed', async () => {
    const wrapper = await openEdit()
    vi.mocked(scriptApi.editStoryBeat)
      .mockResolvedValueOnce(editResult({ audioWillBeDeleted: true }))
      .mockResolvedValueOnce(editResult({ status: 'APPLIED', invalidatedPages: 0 }))
    await save(wrapper)
    const loadsBefore = vi.mocked(scriptApi.getStoryScript).mock.calls.length

    wrapper.findComponent(ConfirmModal).vm.$emit('confirm')
    await flushPromises()

    expect(scriptApi.editStoryBeat).toHaveBeenLastCalledWith(7, 1, expect.anything(), true)
    expect(vi.mocked(scriptApi.getStoryScript).mock.calls.length).toBe(loadsBefore + 1)
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('un 409 IN_PROGRESS muestra el motivo del servidor sin recargar ni emitir changed', async () => {
    const wrapper = await openEdit()
    const reason = 'El cuento se está narrando: no se pueden reabrir, cambiar ni eliminar sus páginas'
    vi.mocked(scriptApi.editStoryBeat)
      .mockResolvedValueOnce(editResult({ audioWillBeDeleted: true }))
      .mockRejectedValueOnce(new ApiError(reason, 409, 'IN_PROGRESS'))
    await save(wrapper)
    const loadsBefore = vi.mocked(scriptApi.getStoryScript).mock.calls.length

    wrapper.findComponent(ConfirmModal).vm.$emit('confirm')
    await flushPromises()

    expect(wrapper.text()).toContain(reason)
    expect(vi.mocked(scriptApi.getStoryScript).mock.calls.length).toBe(loadsBefore)
    expect(wrapper.emitted('changed')).toBeUndefined()
  })
})
