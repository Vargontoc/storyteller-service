import { httpClient } from './httpClient'

export interface OllamaApiResponse {
  topic: boolean
  director: boolean
  scriptwriter: boolean
}

export interface ApiStateResponse {
  ollama: OllamaApiResponse
  chatterbox: boolean
  comfy: boolean
}

export function getServicesStatus() {
  return httpClient.get<ApiStateResponse>('/api/v1/services')
}
