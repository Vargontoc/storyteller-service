import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import type { Topic } from './topics'

export type StorySize = 'S' | 'M' | 'L'

export interface Story {
  id: number
  title: string
  summary: string
  size: StorySize
  characters: unknown[]
  pages: unknown[]
}

export function generateTopic() {
  return httpClient.post<ApiEnvelope<Topic>>('/api/v1/generations/topic').then((envelope) => envelope.data)
}

export function generateStory(topicId: number, size: StorySize) {
  return httpClient
    .post<ApiEnvelope<Story>>('/api/v1/generations/story', { topicId, size })
    .then((envelope) => envelope.data)
}
