import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import StoryNarrationPanel from './StoryNarrationPanel.vue'
import { useWebsocketStore } from '../stores/websocket'
import { ApiError } from '../api/httpClient'
import * as narrationApi from '../api/storyNarration'
import * as voicesApi from '../api/voices'
import type { NarrationState } from '../api/storyNarration'
import type { Voice } from '../api/voices'

vi.mock('../api/storyNarration')
vi.mock('../api/voices')

const narration = vi.mocked(narrationApi)
const voicesMock = vi.mocked(voicesApi)

function state(overrides: Partial<NarrationState> = {}): NarrationState {
  return {
    storyId: 7,
    hasOutline: true,
    canNarrate: true,
    disabledReason: null,
    narrating: false,
    hasAudio: false,
    voiceName: null,
    lastResult: null,
    ...overrides,
  }
}

const systemVoice: Voice = { name: 'Narradora', language: 'es', system: true }
const abuela: Voice = { id: 12, name: 'Abuela', language: 'es', system: false }

let pinia: Pinia

async function mountPanel(s: NarrationState = state(), voices: Voice[] = [systemVoice, abuela]) {
  narration.getNarrationState.mockResolvedValue(s)
  voicesMock.listEligibleVoices.mockResolvedValue(voices)
  const wrapper = mount(StoryNarrationPanel, { props: { storyId: 7 }, global: { plugins: [pinia], stubs: { teleport: true } } })
  await flushPromises()
  return wrapper
}

function buttonByText(wrapper: ReturnType<typeof mount>, text: string) {
  return wrapper.findAll('button').find((b) => b.text() === text)
}

beforeEach(() => {
  pinia = createPinia()
  setActivePinia(pinia)
  vi.resetAllMocks()
})

describe('StoryNarrationPanel', () => {
  it('deshabilita narrar con páginas sin aceptar y muestra el motivo', async () => {
    const wrapper = await mountPanel(state({ canNarrate: false, disabledReason: 'PAGES_NOT_ACCEPTED' }))

    expect(buttonByText(wrapper, 'Narrar')!.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Faltan páginas interiores por aceptar')
  })

  it('muestra los motivos de narración en curso y sin escaleta', async () => {
    const inProgress = await mountPanel(state({ canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    expect(inProgress.text()).toContain('Ya hay una narración en curso')

    const noOutline = await mountPanel(state({ hasOutline: false, canNarrate: false, disabledReason: 'NO_OUTLINE' }))
    expect(noOutline.text()).toContain('no tiene escaleta')
  })

  it('al pulsar narrar muestra solo las elegibles con la del sistema seleccionada', async () => {
    const wrapper = await mountPanel()

    await buttonByText(wrapper, 'Narrar')!.trigger('click')
    await flushPromises()

    const radios = wrapper.findAll('input[type="radio"]')
    expect(radios).toHaveLength(2)
    expect((radios[0].element as HTMLInputElement).checked).toBe(true)
    expect((radios[1].element as HTMLInputElement).checked).toBe(false)
    expect(wrapper.text()).toContain('Narradora')
    expect(wrapper.text()).toContain('Abuela')

    await buttonByText(wrapper, 'Iniciar narración')!.trigger('click')
    expect(narration.startNarration).toHaveBeenCalledWith(7, 'system')
  })

  it('sin voz del sistema no hay preselección y se elige entre las subidas (ALT-002)', async () => {
    const wrapper = await mountPanel(state(), [abuela])

    await buttonByText(wrapper, 'Narrar')!.trigger('click')
    await flushPromises()

    const radio = wrapper.find('input[type="radio"]')
    expect((radio.element as HTMLInputElement).checked).toBe(false)
    expect(buttonByText(wrapper, 'Iniciar narración')!.attributes('disabled')).toBeDefined()

    await radio.setValue(true)
    narration.startNarration.mockResolvedValue(state({ narrating: true, canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    await buttonByText(wrapper, 'Iniciar narración')!.trigger('click')
    await flushPromises()

    expect(narration.startNarration).toHaveBeenCalledWith(7, 12)
    expect(wrapper.text()).toContain('Narrando el cuento')
  })

  it('sin voces elegibles avisa y remite a la sección Voces', async () => {
    const wrapper = await mountPanel(state(), [])

    await buttonByText(wrapper, 'Narrar')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No hay voces en español disponibles')
    expect(wrapper.text()).toContain('Voces')
    expect(wrapper.find('input[type="radio"]').exists()).toBe(false)
  })

  it('renarrar pide confirmación y, si se cancela, no inicia nada', async () => {
    const wrapper = await mountPanel(state({ hasAudio: true, voiceName: 'Narradora' }))

    await buttonByText(wrapper, 'Renarrar')!.trigger('click')
    await flushPromises()
    await buttonByText(wrapper, 'Iniciar narración')!.trigger('click')

    expect(wrapper.text()).toContain('Se sustituirá el audio actual')
    await buttonByText(wrapper, 'Cancelar')!.trigger('click')

    expect(narration.startNarration).not.toHaveBeenCalled()
  })

  it('renarrar confirmado inicia con la voz elegida', async () => {
    const wrapper = await mountPanel(state({ hasAudio: true, voiceName: 'Narradora' }))
    narration.startNarration.mockResolvedValue(state({ narrating: true, canNarrate: false, hasAudio: false }))

    await buttonByText(wrapper, 'Renarrar')!.trigger('click')
    await flushPromises()
    await wrapper.findAll('input[type="radio"]')[1].setValue(true)
    await buttonByText(wrapper, 'Iniciar narración')!.trigger('click')
    await wrapper.find('button.danger').trigger('click') // confirmar en el modal
    await flushPromises()

    expect(narration.startNarration).toHaveBeenCalledWith(7, 12)
  })

  it('durante la narración muestra el indicador y deshabilita narrar y renarrar', async () => {
    const narrating = await mountPanel(state({ narrating: true, canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    expect(narrating.text()).toContain('Narrando el cuento')
    expect(buttonByText(narrating, 'Narrar')!.attributes('disabled')).toBeDefined()

    const renarrating = await mountPanel(state({ narrating: true, hasAudio: true, canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    expect(buttonByText(renarrating, 'Renarrar')!.attributes('disabled')).toBeDefined()
  })

  it('tras el evento COMPLETED recarga y muestra «narrado con <voz>»', async () => {
    const wrapper = await mountPanel(state({ narrating: true, canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    narration.getNarrationState.mockResolvedValue(state({ hasAudio: true, voiceName: 'Abuela', lastResult: 'COMPLETED' }))

    useWebsocketStore().lastNarrationEvent = { storyId: 7, type: 'COMPLETED', voiceName: 'Abuela', id: 1 }
    await flushPromises()

    expect(wrapper.text()).toContain('Narrado con Abuela')
    expect(wrapper.text()).not.toContain('Narrando el cuento')
  })

  it('tras FAILED muestra el aviso de error y narrar habilitado', async () => {
    const wrapper = await mountPanel(state({ narrating: true, canNarrate: false, disabledReason: 'IN_PROGRESS' }))
    narration.getNarrationState.mockResolvedValue(state({ lastResult: 'ERROR' }))

    useWebsocketStore().lastNarrationEvent = { storyId: 7, type: 'FAILED', id: 1 }
    await flushPromises()

    expect(wrapper.find('.result-error').exists()).toBe(true)
    expect(buttonByText(wrapper, 'Narrar')!.attributes('disabled')).toBeUndefined()
  })

  it.each([
    ['GENERATION_IN_PROGRESS', 'otra generación en curso'],
    ['NO_VOICES_AVAILABLE', 'No hay voces en español disponibles'],
    ['VOICE_NOT_AVAILABLE', 'ya no está disponible'],
    ['PAGES_NOT_ACCEPTED', 'Faltan páginas interiores por aceptar'],
    ['IN_PROGRESS', 'ya se está narrando'],
    ['NO_OUTLINE', 'no tiene escaleta'],
  ])('un rechazo 409 %s muestra su aviso', async (code, text) => {
    const wrapper = await mountPanel()
    narration.startNarration.mockRejectedValue(new ApiError('mensaje del servidor', 409, code))

    await buttonByText(wrapper, 'Narrar')!.trigger('click')
    await flushPromises()
    await buttonByText(wrapper, 'Iniciar narración')!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.action-error').text()).toContain(text)
  })

  it('no solicita ni muestra el guion de audio', async () => {
    const wrapper = await mountPanel(state({ hasAudio: true, voiceName: 'Narradora' }))

    expect(wrapper.text().toLowerCase()).not.toContain('guion')
    expect(Object.keys(narration).filter((k) => /script|guion/i.test(k))).toEqual([])
  })

  it('ignora los eventos de otro cuento y recarga con la reconexión', async () => {
    await mountPanel()
    narration.getNarrationState.mockClear()
    const ws = useWebsocketStore()

    ws.lastNarrationEvent = { storyId: 99, type: 'COMPLETED', voiceName: 'X', id: 1 }
    await flushPromises()
    expect(narration.getNarrationState).not.toHaveBeenCalled()

    ws.connectionId += 1
    await flushPromises()
    expect(narration.getNarrationState).toHaveBeenCalledTimes(1)
  })
})
