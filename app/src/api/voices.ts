import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

// Cliente REST de la biblioteca de voces (base /api/v1/voices).
// Los rechazos llegan como ApiError: message con el texto y code con el motivo estable del servidor
// (NAME_REQUIRED, LANGUAGE_REQUIRED, PERMISSION_REQUIRED, INVALID_FORMAT, INVALID_DURATION,
// NAME_DUPLICATED, SYSTEM_VOICE_NOT_DELETABLE, VOICE_NOT_FOUND).
//
//   GET    /voices            -> listVoices
//   GET    /voices/languages  -> getLanguages
//   GET    /voices/eligible   -> listEligibleVoices
//   POST   /voices            -> uploadVoice (multipart)
//   DELETE /voices/{id}       -> deleteVoice ('system' = voz del sistema, siempre rechazada)

const BASE = '/api/v1/voices'

// La voz del sistema va primera, con system true y sin id.
export interface Voice {
  id?: number
  name: string
  language: string
  system: boolean
}

export interface VoiceLibrary {
  voices: Voice[]
  systemVoiceName: string
  systemVoiceAvailable: boolean
  systemVoiceUnavailableReason: string | null
  systemVoiceNameCollision: boolean
}

export interface Language {
  code: string
  name: string
}

export interface LanguageCatalog {
  languages: Language[]
  defaultLanguage: Language
}

export interface VoiceUpload {
  name: string
  language: string
  permission: boolean
  file: File | Blob
}

export async function listVoices(): Promise<VoiceLibrary> {
  return (await httpClient.get<ApiEnvelope<VoiceLibrary>>(BASE)).data
}

export async function getLanguages(): Promise<LanguageCatalog> {
  return (await httpClient.get<ApiEnvelope<LanguageCatalog>>(`${BASE}/languages`)).data
}

export async function listEligibleVoices(): Promise<Voice[]> {
  return (await httpClient.get<ApiEnvelope<Voice[]>>(`${BASE}/eligible`)).data
}

export async function uploadVoice(upload: VoiceUpload): Promise<Voice> {
  const form = new FormData()
  form.append('file', upload.file)
  form.append('name', upload.name)
  form.append('language', upload.language)
  form.append('permission', String(upload.permission))
  return (await httpClient.postForm<ApiEnvelope<Voice>>(BASE, form)).data
}

export async function deleteVoice(id: number | 'system'): Promise<void> {
  await httpClient.delete<ApiEnvelope<null>>(`${BASE}/${id}`)
}
