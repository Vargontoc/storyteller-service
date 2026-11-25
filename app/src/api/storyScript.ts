import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import type { Story } from './generations'

export type StoryStatus = 'SCRIPT_PENDING' | 'SCRIPT_ACCEPTED'
export type BeatEditStatus = 'APPLIED' | 'REJECTED' | 'CONFIRMATION_REQUIRED' | 'CANCELLED'

export interface CastMember {
  name: string
  role: string
}

// Los campos del beat. setting (lugar de la escena) puede faltar o ser null en cuentos anteriores;
// el servidor lo exige no vacío al editar un beat.
export interface StoryBeatFields {
  event: string
  narrativeFunction: string
  participants: string[]
  tone: string
  visualNote: string
  setting?: string | null
}

// Planteamiento de la portada; independiente del beat 1.
export interface CoverPlan {
  setting: string
  tone: string
}

// Beat del guion vigente; page empieza en 1.
export interface ScriptBeat extends StoryBeatFields {
  page: number
}

// scriptVersion nulo = cuento antiguo (cast y beats vacíos).
export interface StoryScript {
  id: number
  title: string
  synopsis: string
  status: StoryStatus
  scriptVersion: number | null
  cast: CastMember[]
  beats: ScriptBeat[]
  // Ausente o null en cuentos anteriores.
  cover?: CoverPlan | null
}

// Resultado de aprobar el guion; coverStarted indica si la portada empezó a generarse.
export interface ApproveResult {
  story: Story
  coverStarted: boolean
}

export interface BeatEditResult {
  status: BeatEditStatus
  reason: string | null
  invalidatedPages: number
  // Solo true en CONFIRMATION_REQUIRED de un cuento con audio; puede faltar.
  audioWillBeDeleted?: boolean
  story: Story | null
}

export function getStoryScript(storyId: number) {
  return httpClient
    .get<ApiEnvelope<StoryScript>>(`/api/v1/generations/story/${storyId}/script`)
    .then((envelope) => envelope.data)
}

export function approveStoryScript(storyId: number) {
  return httpClient
    .post<ApiEnvelope<ApproveResult>>(`/api/v1/generations/story/${storyId}/approve`)
    .then((envelope) => envelope.data)
}

// Sin confirmInvalidation es la primera petición; con true confirma el aviso de invalidación y con false lo cancela.
export function editStoryBeat(storyId: number, page: number, beat: StoryBeatFields, confirmInvalidation?: boolean) {
  return httpClient
    .put<ApiEnvelope<BeatEditResult>>(`/api/v1/generations/story/${storyId}/beat/${page}`, {
      ...beat,
      confirmInvalidation,
    })
    .then((envelope) => envelope.data)
}
