import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import { apiConfig } from './config'

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
export interface PortraitCandidate { id: number; catalogCharacterId: number; generationJobId: number; assetPath: string }

export interface CatalogCharacterCreation {
  accepted: boolean
  rejectionReason: string | null
  name: string | null
  narrativeDescription: string | null
  visualDescription: string | null
  visualDescriptionEn: string | null
  visualAttributes: string[] | null
}

export type CharacterReviewTarget = 'NARRATIVE' | 'VISUAL' | 'BOTH'
export type CharacterReviewResolution = 'CONFIRMED' | 'DISCARDED'
export interface CatalogCharacterReview { id: number; target: CharacterReviewTarget; hint: string; name: string; narrativeDescription: string; visualDescription: string; forkConfirmationRequired: boolean; forkCatalogCharacterId: number | null }
export interface CatalogCharacterReviewProposal { accepted: boolean; rejectionReason: string | null; name: string | null; narrativeDescription: string | null; visualDescription: string | null }
export interface CatalogCharacterReviewRequestResult { replacementConfirmationRequired: boolean; visualConsistencyWarning: boolean; proposal: CatalogCharacterReviewProposal | null }

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

export function generateCatalogCharacterPortraits(id: number) {
  return httpClient.post<ApiEnvelope<void>>(`/api/v1/catalog-characters/${id}/portraits/generate`).then(() => undefined)
}

export function getPortraitCandidates(id: number) {
  return httpClient.get<ApiEnvelope<PortraitCandidate[]>>(`/api/v1/catalog-characters/${id}/portraits/candidates`).then((result) => result.data)
}

export function selectPortraitCandidate(id: number, candidateId: number) {
  return httpClient.post<ApiEnvelope<void>>(`/api/v1/catalog-characters/${id}/portraits/candidates/${candidateId}/select`).then(() => undefined)
}

export function isPortraitGenerationRunning(id: number) {
  return httpClient.get<ApiEnvelope<boolean>>(`/api/v1/catalog-characters/${id}/portraits/generating`).then((result) => result.data)
}

export function portraitCandidateAssetUrl(characterId: number, candidateId: number) {
  return `${apiConfig.baseUrl}/api/v1/catalog-characters/${characterId}/portraits/candidates/${candidateId}/asset`
}

export function approvedPortraitAssetUrl(characterId: number, version: number) {
  return `${apiConfig.baseUrl}/api/v1/catalog-characters/${characterId}/portraits/approved/asset?v=${version}`
}

export function getPendingCatalogCharacterReview(characterId: number) { return httpClient.get<ApiEnvelope<CatalogCharacterReview>>(`/api/v1/catalog-characters/${characterId}/reviews/pending`).then((result) => result.data) }
export function requestCatalogCharacterReview(characterId: number, target: CharacterReviewTarget, hint: string, replacePending: boolean) { return httpClient.post<ApiEnvelope<CatalogCharacterReviewRequestResult>>(`/api/v1/catalog-characters/${characterId}/reviews`, { target, hint, replacePending }).then((result) => result.data) }
export function resolveCatalogCharacterReview(characterId: number, reviewId: number, status: CharacterReviewResolution, confirmFork = false) { return httpClient.post<ApiEnvelope<CatalogCharacterReview>>(`/api/v1/catalog-characters/${characterId}/reviews/${reviewId}/resolution`, { status, confirmFork }).then((result) => result.data) }
