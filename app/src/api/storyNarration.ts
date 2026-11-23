import { ApiError, httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

// Cliente REST de la narración de un cuento (base /api/v1/storyteller/{storyId}).
// Los rechazos llegan como ApiError: message con el texto y code con el motivo estable del servidor
// (NO_OUTLINE, PAGES_NOT_ACCEPTED, IN_PROGRESS, VOICE_NOT_AVAILABLE, NO_VOICES_AVAILABLE,
// GENERATION_IN_PROGRESS, AUDIO_NOT_FOUND).
//
//   POST /narration              -> startNarration (202, asíncrono; el resultado llega por WebSocket)
//   GET  /narration              -> getNarrationState
//   GET  /pages/{number}/audio   -> getPageAudio (solo páginas interiores, number >= 1)

export type NarrationDisabledReason = 'NO_OUTLINE' | 'IN_PROGRESS' | 'PAGES_NOT_ACCEPTED'

export type NarrationResult = 'IN_PROGRESS' | 'COMPLETED' | 'ERROR'

// Estado autoritativo del servidor; nunca incluye el guion de audio.
export interface NarrationState {
  storyId: number
  hasOutline: boolean
  canNarrate: boolean
  disabledReason: NarrationDisabledReason | null
  narrating: boolean
  hasAudio: boolean
  // Solo si hay audio.
  voiceName: string | null
  lastResult: NarrationResult | null
}

function base(storyId: number) {
  return `/api/v1/storyteller/${storyId}`
}

// Inicia o renarra con una voz elegible (id numérico o 'system'). No modifica estado local.
export async function startNarration(storyId: number, voiceId: number | 'system'): Promise<NarrationState> {
  const envelope = await httpClient.post<ApiEnvelope<NarrationState>>(`${base(storyId)}/narration`, {
    voiceId: String(voiceId),
  })
  return envelope.data
}

export async function getNarrationState(storyId: number): Promise<NarrationState> {
  return (await httpClient.get<ApiEnvelope<NarrationState>>(`${base(storyId)}/narration`)).data
}

// Audio WAV de una página interior. La portada (0) no tiene audio: se rechaza sin llamar al servidor.
export async function getPageAudio(storyId: number, pageNumber: number): Promise<Blob> {
  if (pageNumber < 1) {
    throw new ApiError('La portada no tiene audio', 404, 'AUDIO_NOT_FOUND')
  }
  return httpClient.getForBlob(`${base(storyId)}/pages/${pageNumber}/audio`)
}
