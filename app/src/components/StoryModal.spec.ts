import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import StoryModal from './StoryModal.vue'
import StoryPagesPanel from './StoryPagesPanel.vue'
import StoryNarrationPanel from './StoryNarrationPanel.vue'
import PagesContent from './content/PagesContent.vue'
import * as storyteller from '../api/storyteller'
import * as reviews from '../api/reviews'
import * as scriptApi from '../api/storyScript'
import type { StoryScript } from '../api/storyScript'

vi.mock('../api/storyteller')
vi.mock('../api/reviews')
vi.mock('../api/storyScript')
vi.mock('../api/generations')

const stubs = {
  teleport: true,
  StoryPagesPanel: true,
  StoryNarrationPanel: true,
  PagesContent: true,
  StoryContent: true,
  StoryScriptPanel: true,
  ActorsContent: true,
  ReviewDetailsModal: true,
}

function script(overrides: Partial<StoryScript> = {}): StoryScript {
  return {
    id: 7,
    title: 'Cuento',
    synopsis: '',
    status: 'SCRIPT_ACCEPTED',
    scriptVersion: 1,
    cast: [],
    beats: [],
    ...overrides,
  }
}

// legacyPages: lista legacy de getStoryPages (solo la usa el cuento antiguo).
async function openModal(s: StoryScript, legacyPages: { page: number }[] = []) {
  vi.mocked(storyteller.getStory).mockResolvedValue({ id: 7, title: 'Cuento', pages: 0, size: 3 } as never)
  vi.mocked(storyteller.getStoryPages).mockResolvedValue(legacyPages as never)
  vi.mocked(reviews.getPendingStoryReview).mockResolvedValue(null as never)
  vi.mocked(scriptApi.getStoryScript).mockResolvedValue(s)

  const wrapper = mount(StoryModal, {
    props: { open: false, mode: 'edit', storyId: 7 },
    global: { stubs },
  })
  // El modal carga el cuento al pasar a abierto.
  await wrapper.setProps({ open: true })
  await flushPromises()
  return wrapper
}

function tabLabels(wrapper: Awaited<ReturnType<typeof openModal>>) {
  return wrapper.findAll('[role="tab"]').map((t) => t.text())
}

async function openTab(wrapper: Awaited<ReturnType<typeof openModal>>, label: string) {
  await wrapper.findAll('[role="tab"]').find((t) => t.text() === label)!.trigger('click')
}

describe('StoryModal: pestaña de páginas', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('cuento con escaleta monta StoryPagesPanel aunque la lista legacy esté vacía', async () => {
    const wrapper = await openModal(script({ scriptVersion: 2 }))

    expect(tabLabels(wrapper)).toEqual(['Guión', 'Personajes', 'Páginas'])
    await openTab(wrapper, 'Páginas')

    expect(wrapper.findComponent(StoryPagesPanel).exists()).toBe(true)
    expect(wrapper.findComponent(PagesContent).exists()).toBe(false)
  })

  it('cuento sin escaleta mantiene PagesContent y no monta el panel', async () => {
    const wrapper = await openModal(script({ scriptVersion: null }), [{ page: 0 }, { page: 1 }])

    expect(tabLabels(wrapper)).toEqual(['Guión', 'Personajes', 'Portada', 'Páginas'])
    await openTab(wrapper, 'Páginas')

    expect(wrapper.findComponent(PagesContent).exists()).toBe(true)
    expect(wrapper.findComponent(StoryPagesPanel).exists()).toBe(false)
  })

  it('cuento con escaleta muestra el panel de narración junto a las páginas', async () => {
    const wrapper = await openModal(script({ scriptVersion: 2 }))
    await openTab(wrapper, 'Páginas')

    const panel = wrapper.findComponent(StoryNarrationPanel)
    expect(panel.exists()).toBe(true)
    expect(panel.props('storyId')).toBe(7)
    expect(wrapper.findAllComponents(StoryNarrationPanel)).toHaveLength(1)
  })

  it('un cambio en las páginas vuelve a montar el panel de narración para releer su estado', async () => {
    const wrapper = await openModal(script({ scriptVersion: 2 }))
    await openTab(wrapper, 'Páginas')
    const before = wrapper.findComponent(StoryNarrationPanel).vm

    wrapper.findComponent(StoryPagesPanel).vm.$emit('changed')
    await flushPromises()

    expect(wrapper.findAllComponents(StoryNarrationPanel)).toHaveLength(1)
    expect(wrapper.findComponent(StoryNarrationPanel).vm).not.toBe(before)
  })

  it('cuento antiguo no muestra el panel de narración y conserva su vista', async () => {
    const wrapper = await openModal(script({ scriptVersion: null }), [{ page: 0 }, { page: 1 }])
    await openTab(wrapper, 'Páginas')

    expect(wrapper.findComponent(StoryNarrationPanel).exists()).toBe(false)
    expect(wrapper.findComponent(PagesContent).exists()).toBe(true)
  })

  it('con guion pendiente no hay acceso operativo a páginas', async () => {
    const wrapper = await openModal(script({ status: 'SCRIPT_PENDING' }))

    expect(tabLabels(wrapper)).toEqual(['Guión', 'Personajes'])
    expect(wrapper.findComponent(StoryPagesPanel).exists()).toBe(false)
  })
})
