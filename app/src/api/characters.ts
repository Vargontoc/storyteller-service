import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

export interface CatalogCharacter {
  id: number
  name: string
  narrativeDescription: string
  visualDescription: string
  draft?: boolean | null
  portraitStatus?: CatalogPortraitStatus
  pendingReview: boolean
}

export type CatalogPortraitStatus = 'UNKNOWN' | 'DRAFT' | 'APPROVED'

export interface CatalogCharacterCreation {
  accepted: boolean
  rejectionReason: string | null
  name: string | null
  narrativeDescription: string | null
  visualDescription: string | null
  visualDescriptionEn: string | null
  visualAttributes: string[] | null
}

export function getCatalogCharacters() {
  return httpClient.get<ApiEnvelope<CatalogCharacter[]>>('/api/v1/catalog-characters').then((result) => result.data)
}

export function getCatalogCharacter(id: number) {
  return httpClient.get<ApiEnvelope<CatalogCharacter>>(`/api/v1/catalog-characters/${id}`).then((result) => result.data)
}

export function createCatalogCharacter(hint?: string) {
  return httpClient.post<ApiEnvelope<CatalogCharacterCreation>>('/api/v1/catalog-characters', hint === undefined ? undefined : { hint }).then((result) => result.data)
}

export function renameCatalogCharacter(id: number, name: string) {
  return httpClient.patch<ApiEnvelope<CatalogCharacter>>(`/api/v1/catalog-characters/${id}/name`, { name }).then((result) => result.data)
}
