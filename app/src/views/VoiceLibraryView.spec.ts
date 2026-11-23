import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ConfirmModal from '../components/ConfirmModal.vue'
import { ApiError } from '../api/httpClient'
import VoiceLibraryView from './VoiceLibraryView.vue'
import * as voicesApi from '../api/voices'

vi.mock('../api/voices', () => ({
  listVoices: vi.fn(),
  uploadVoice: vi.fn(),
  deleteVoice: vi.fn(),
}))

const api = vi.mocked(voicesApi)

const system = { name: 'Narradora', language: 'es', system: true }
const abuela = { id: 3, name: 'Abuela', language: 'es', system: false }
const library = (overrides = {}) => ({
  voices: [system, abuela],
  systemVoiceName: 'Narradora',
  systemVoiceAvailable: true,
  systemVoiceUnavailableReason: null,
  systemVoiceNameCollision: false,
  ...overrides,
})

async function mountView() {
  const wrapper = mount(VoiceLibraryView)
  await flushPromises()
  return wrapper
}

async function fillAndSubmit(wrapper: Awaited<ReturnType<typeof mountView>>) {
  await wrapper.find('input[name="name"]').setValue('Abuelo')
  const input = wrapper.find('input[name="file"]')
  Object.defineProperty(input.element, 'files', { value: [new File(['x'], 'a.ogg')], configurable: true })
  await input.trigger('change')
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  vi.resetAllMocks()
  api.listVoices.mockResolvedValue(library())
})

describe('VoiceLibraryView', () => {
  it('el formulario no ofrece selector de idioma y la subida envía siempre español', async () => {
    api.uploadVoice.mockResolvedValue({ id: 4, name: 'Abuelo', language: 'es', system: false })
    const wrapper = await mountView()
    expect(wrapper.find('select[name="language"]').exists()).toBe(false)
    await fillAndSubmit(wrapper)
    expect(api.uploadVoice).toHaveBeenCalledWith(expect.objectContaining({ language: 'es' }))
  })

  it('muestra cada voz con nombre e idioma y la del sistema identificada', async () => {
    const wrapper = await mountView()
    const rows = wrapper.findAll('.voice-row')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('Narradora')
    expect(rows[0].text()).toContain('español')
    expect(rows[0].text()).toContain('Voz del sistema')
    expect(rows[1].text()).toContain('Abuela')
    expect(rows[1].text()).not.toContain('Voz del sistema')
  })

  it('ofrece borrar solo las voces subidas', async () => {
    const wrapper = await mountView()
    const rows = wrapper.findAll('.voice-row')
    expect(rows[0].find('button').exists()).toBe(false)
    expect(rows[1].find('button').exists()).toBe(true)
  })

  it('avisa con su motivo cuando la voz del sistema no está disponible', async () => {
    api.listVoices.mockResolvedValue(library({
      voices: [abuela], systemVoiceAvailable: false, systemVoiceUnavailableReason: 'Audio de referencia no encontrado',
    }))
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('La voz del sistema no está disponible')
    expect(wrapper.text()).toContain('Audio de referencia no encontrado')
  })

  it('avisa cuando el nombre de la voz del sistema coincide con una subida', async () => {
    api.listVoices.mockResolvedValue(library({ systemVoiceNameCollision: true }))
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('coincide con el de una voz subida')
  })

  it('un rechazo de subida muestra el aviso del motivo y no cambia la lista', async () => {
    api.uploadVoice.mockRejectedValue(new ApiError('Formato rechazado', 400, 'INVALID_FORMAT'))
    const wrapper = await mountView()
    await fillAndSubmit(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toContain('WAV o MP3')
    expect(wrapper.findAll('.voice-row')).toHaveLength(2)
    expect(api.listVoices).toHaveBeenCalledTimes(1)
  })

  it('una subida válida recarga la lista', async () => {
    api.uploadVoice.mockResolvedValue({ id: 4, name: 'Abuelo', language: 'es', system: false })
    api.listVoices.mockResolvedValueOnce(library()).mockResolvedValueOnce(
      library({ voices: [system, abuela, { id: 4, name: 'Abuelo', language: 'es', system: false }] }),
    )
    const wrapper = await mountView()
    await wrapper.find('input[name="permission"]').setValue(true)
    await fillAndSubmit(wrapper)
    expect(api.uploadVoice).toHaveBeenCalledWith(expect.objectContaining({ name: 'Abuelo', language: 'es', permission: true }))
    expect(wrapper.findAll('.voice-row')).toHaveLength(3)
  })

  it('tras confirmar el borrado la voz desaparece', async () => {
    api.deleteVoice.mockResolvedValue(undefined)
    api.listVoices.mockResolvedValueOnce(library()).mockResolvedValueOnce(library({ voices: [system] }))
    const wrapper = await mountView()
    await wrapper.findAll('.voice-row')[1].find('button').trigger('click')
    wrapper.findComponent(ConfirmModal).vm.$emit('confirm')
    await flushPromises()
    expect(api.deleteVoice).toHaveBeenCalledWith(3)
    expect(wrapper.text()).not.toContain('Abuela')
  })

  it('cancelar la confirmación no borra', async () => {
    const wrapper = await mountView()
    await wrapper.findAll('.voice-row')[1].find('button').trigger('click')
    wrapper.findComponent(ConfirmModal).vm.$emit('cancel')
    await flushPromises()
    expect(api.deleteVoice).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Abuela')
  })

  it('un fallo al borrar muestra el aviso y no altera la lista', async () => {
    api.deleteVoice.mockRejectedValue(new ApiError('No existe la voz', 404, 'VOICE_NOT_FOUND'))
    const wrapper = await mountView()
    await wrapper.findAll('.voice-row')[1].find('button').trigger('click')
    wrapper.findComponent(ConfirmModal).vm.$emit('confirm')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('No existe la voz')
    expect(wrapper.findAll('.voice-row')).toHaveLength(2)
  })
})
