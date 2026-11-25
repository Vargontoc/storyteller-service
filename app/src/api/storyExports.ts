import { ApiError, httpClient } from './httpClient'

export type ExportMissing =
  | 'COVER_NOT_ACCEPTED'
  | 'ACCEPTED_TEXT'
  | 'IMAGE'
  | 'AUDIO'
  | 'SCRIPT_NOT_APPROVED'
  | 'BEAT_SETTING'
  | 'COVER_SETTING_TONE'
  | 'PROTAGONIST_MISSING'
  | 'PROTAGONIST_IMAGE'
  | 'CONTRACT_RULE'

export interface ExportFault {
  // 0 = portada, N = página interior, -1 = falta de todo el cuento.
  page: number
  missing: ExportMissing
  message: string
  // Código estable del contrato, o null (en Nubi Game siempre null).
  code: string | null
}

export type ExportResult =
  | { kind: 'download'; blob: Blob; filename: string }
  | { kind: 'rejected'; message: string; faults: ExportFault[] }

async function exportStory(storyId: number, target: 'nubi-game' | 'visual-agent'): Promise<ExportResult> {
  try {
    const { blob, filename } = await httpClient.getForFile(`/api/v1/storyteller/${storyId}/export/${target}`)
    return { kind: 'download', blob, filename: filename ?? `story-${storyId}.zip` }
  } catch (e) {
    // 409: rechazo sin zip con la lista completa de faltas. Otros errores (404...) se propagan.
    if (e instanceof ApiError && e.status === 409) {
      const faults = (e.data as { faults?: ExportFault[] } | undefined)?.faults ?? []
      return { kind: 'rejected', message: e.message, faults }
    }
    throw e
  }
}

export function exportNubiGame(storyId: number) {
  return exportStory(storyId, 'nubi-game')
}

export function exportVisualAgent(storyId: number) {
  return exportStory(storyId, 'visual-agent')
}
