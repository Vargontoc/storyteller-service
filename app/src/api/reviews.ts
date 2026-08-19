import { ApiError, httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import type { Story } from './generations'

export type RevisionStatus = 'PENDING' | 'CONFIRMED' | 'DISCARDED'

export interface StoryReview {
  id: number
  previewStory: Story
  hint?: string
  hintAccepted?: boolean
  rejectedReason?: string | null
  status: RevisionStatus
}

export function confirmStoryReview(storyId: number, status: 'CONFIRMED' | 'DISCARDED') {
  return httpClient
    .post<ApiEnvelope<Story>>('/api/v1/reviews/story/confirm', { entityId: storyId, status })
    .then((envelope) => envelope.data)
}

export function reviewStory(storyId: number, hint: string) {
  return httpClient
    .post<ApiEnvelope<StoryReview>>('/api/v1/reviews/story', { storyId, hint })
    .then((envelope) => envelope.data)
}

export async function getPendingStoryReview(storyId: number): Promise<StoryReview | null> {
  try {
    const envelope = await httpClient.get<ApiEnvelope<StoryReview>>(`/api/v1/reviews/story/${storyId}`)
    return envelope.data
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null
    }
    throw err
  }
}
