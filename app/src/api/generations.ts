import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'
import type { Topic } from './topics'

export type StorySize = 'S' | 'M' | 'L'
export type KindImage = 'ACTOR' | 'PAGE' | 'COVER'
export type PresetVoice = 'CALM_STORYTELLER' | 'ADVENTURE_ENERGIC' | 'SLEEP_SOFT' | 'NEUTRO' | 'CUSTOM'
export interface Story {
  id: number
  title: string
  summary: string
  size: StorySize
  characters: Actor[]
  pages: Page[]
}

export interface Actor {
  id: number
  name: string
  visual: string
  description: string
}

export interface Page {
  id: number
  page: number
  text: string
  scene: string
}

export interface AudioParams {
  exageration: number
  cfgWeight: number
  temperatue: number
}

export interface AudioRequest {
  pageId: number
  voiceName?: string
  preset: PresetVoice
  customParams?: AudioParams
}

export function generateTopic() {
  return httpClient.post<ApiEnvelope<Topic>>('/api/v1/generations/topic').then((envelope) => envelope.data)
}

export function generateStory(topicId: number, size: StorySize) {
  return httpClient
    .post<ApiEnvelope<Story>>('/api/v1/generations/story', { topicId, size })
    .then((envelope) => envelope.data)
}

export function generatePage(idStory: number) {
  return httpClient
    .post<ApiEnvelope<Page>>('/api/v1/generations/page', { idStory })
    .then((envelope) => envelope.data)
}

export function generateImage(image: KindImage, id: number) {
  return httpClient
    .post<ApiEnvelope<string>>('/api/v1/assets/images/generate', { image, id })
    .then((envelope) => envelope.data)
}

export function generateAudio(request: AudioRequest) {
  return httpClient
    .post<ApiEnvelope<string>>('/api/v1/assets/audios/generate', request)
    .then((envelope) => envelope.data)
}

