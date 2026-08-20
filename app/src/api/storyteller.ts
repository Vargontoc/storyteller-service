import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

export interface StorySummary {
  id: number
  cover: string | null
  title: string
  synopsis: string
  actors: number
  size: number
  pages: number
}

export interface StorySummaryPage {
  items: StorySummary[]
  pageSize: number
  currentPage: number
  totalPages: number
  totalItems: number
}

export function getStories(page: number, pageSize: number) {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })

  return httpClient
    .get<ApiEnvelope<StorySummaryPage>>(`/api/v1/storyteller?${params.toString()}`)
    .then((envelope) => envelope.data)
}

export function getStory(id: number) {
  return httpClient.get<ApiEnvelope<StorySummary>>(`/api/v1/storyteller/${id}`).then((envelope) => envelope.data)
}

export interface ActorSummary {
  id: number,
  name: string,
  role: string,
  description: string,
  image: string | null
}

export function getStoryActors(storyId: number) {
  return httpClient
  .get<ApiEnvelope<ActorSummary[]>>(`/api/v1/storyteller/${storyId}/actors`)
  .then((envelope) => envelope.data)
}

export interface PageSummary {
  id: number
  page: number
  text: string
  scene: string
  image: string | null
  audio: string | null
}

export function getStoryPages(storyId: number) {
  return httpClient
    .get<ApiEnvelope<PageSummary[]>>(`/api/v1/storyteller/${storyId}/pages`)
    .then((envelope) => envelope.data)
}

export function getStorytellerAsset(path: string) {
  return httpClient.postForBlob('/api/v1/storyteller/asset', { path })
}

export function deleteStory(id: number) {
  return httpClient.delete<void>(`/api/v1/storyteller/${id}`)
}
