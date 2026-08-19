import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

export interface Topic {
  id: number
  type: string
  description: string
}

export function getTopics() {
  return httpClient.get<ApiEnvelope<Topic[]>>('/api/v1/topic').then((envelope) => envelope.data)
}
