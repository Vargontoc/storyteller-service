import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import StoryContent from './StoryContent.vue'
import * as storytellerApi from '../../api/storyteller'
import * as reviewsApi from '../../api/reviews'

vi.mock('../../api/storyteller')
vi.mock('../../api/reviews', async (importOriginal) => ({
  ...(await importOriginal<typeof reviewsApi>()),
  getPendingStoryReview: vi.fn(),
}))
vi.mock('../../api/generations.ts')

const story = { id: 1, cover: null, title: 'T', synopsis: 'S', actors: 2, size: 3, pages: 3 }

async function mountContent(outlined: boolean) {
  const wrapper = mount(StoryContent, {
    props: { storyId: 1, outlined },
    global: { stubs: { ReviewModal: true } },
  })
  await flushPromises()
  return wrapper
}

const hasNarrationButton = (w: Awaited<ReturnType<typeof mountContent>>) =>
  w.findAll('button').some((b) => b.text() === 'Generar narración')

describe('StoryContent', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(storytellerApi.getStory).mockResolvedValue({ ...story })
    vi.mocked(reviewsApi.getPendingStoryReview).mockResolvedValue(null as never)
  })

  it('oculta el botón antiguo de narración en cuentos con escaleta', async () => {
    expect(hasNarrationButton(await mountContent(true))).toBe(false)
  })

  it('muestra el botón antiguo en cuentos sin escaleta con pages == size', async () => {
    expect(hasNarrationButton(await mountContent(false))).toBe(true)
  })

  it('no muestra el botón sin escaleta si faltan páginas', async () => {
    vi.mocked(storytellerApi.getStory).mockResolvedValue({ ...story, pages: 1 })
    expect(hasNarrationButton(await mountContent(false))).toBe(false)
  })
})
