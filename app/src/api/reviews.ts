import { ApiError, httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import type { Actor, Story } from './generations'


export type RevisionStatus = 'PENDING' | 'CONFIRMED' | 'DISCARDED'
export type ReviewType = 'SCRIPT' | 'ACTOR' | 'COVER' | 'PAGE'
export type ReviewActorTarget = 'VISUAL' | 'ROLE' | 'BOTH'
export type ReviewPageTarget = 'TEXT' | 'SCENE' | 'BOTH'

export interface StoryReview {
  id: number
  previewStory: Story
  hint?: string
  hintAccepted?: boolean
  rejectedReason?: string | null
  status: RevisionStatus
}

export function confirmStoryReview(storyId: number, status: RevisionStatus) {
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

export interface ActorReview {
  characterId: number
  previewActo: Actor
  visualDescription?: string
  narrativeDescription?: string
  hint?: string
  hintAccepted?: boolean
  rejectedReason?: string | null
  status: RevisionStatus
}

export function reviewActor(storyId: number, id: number,  target: ReviewActorTarget, hint: string) {
  return httpClient
    .post<ApiEnvelope<StoryReview>>('/api/v1/reviews/actor', { storyId, id, target, hint })
    .then((envelope) => envelope.data)
}


export async function getActorReview(actorId: number): Promise<ActorReview | null> {
  try {
    const envelope = await httpClient.get<ApiEnvelope<ActorReview>>(`/api/v1/reviews/actor/${actorId}`)
    return envelope.data
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null
    }
    throw err
  }
}

export function confirmActorReview(actorId: number, status: RevisionStatus) {
  return httpClient
    .post<ApiEnvelope<Story>>('/api/v1/reviews/actor/confirm', { entityId: actorId, status })
    .then((envelope) => envelope.data)
}

export function isStoryReview(review: StoryReview | ActorReview): review is StoryReview {
  return 'previewStory' in review
}

export function isActorReview(review: StoryReview | ActorReview): review is ActorReview {
  return 'characterId' in review
}

export interface PageReview {
  pageId: number
  text?: string
  scene?: string
  hint?: string
  hintAccepted?: boolean
  rejectedReason?: string | null
  status: RevisionStatus
}


export function reviewPage(storyId: number, id: number,  target: ReviewPageTarget, hint: string) {
  return httpClient
    .post<ApiEnvelope<StoryReview>>('/api/v1/reviews/page', { storyId, id, target, hint })
    .then((envelope) => envelope.data)
}


export async function getPageReview(id: number): Promise<PageReview | null> {
  try {
    const envelope = await httpClient.get<ApiEnvelope<PageReview>>(`/api/v1/reviews/page/${id}`)
    return envelope.data
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null
    }
    throw err
  }
}

export function confirmPageReview(id: number, status: RevisionStatus) {
  return httpClient
    .post<ApiEnvelope<Story>>('/api/v1/reviews/page/confirm', { entityId: id, status })
    .then((envelope) => envelope.data)
}



