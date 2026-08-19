import { apiConfig } from './config'

export class ApiError extends Error {
  readonly status?: number

  constructor(message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.clone().json()
    if (typeof body?.message === 'string' && body.message) {
      return body.message
    }
    if (Array.isArray(body?.errors) && body.errors.length > 0) {
      return body.errors.join(', ')
    }
  } catch {
    // Body wasn't JSON (or was empty) — fall through to the generic message below.
  }

  return `Request failed: ${response.status} ${response.statusText}`
}

async function requestRaw(path: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), apiConfig.timeoutMs)

  try {
    const response = await fetch(`${apiConfig.baseUrl}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
      signal: controller.signal,
    })

    if (!response.ok) {
      throw new ApiError(await extractErrorMessage(response), response.status)
    }

    return response
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('Request timed out')
    }
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError(error instanceof Error ? error.message : 'Unknown network error')
  } finally {
    clearTimeout(timeout)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await requestRaw(path, init)

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

async function requestBlob(path: string, init?: RequestInit): Promise<Blob> {
  const response = await requestRaw(path, init)
  return response.blob()
}

export const httpClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForBlob: (path: string, body?: unknown) =>
    requestBlob(path, {
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
      headers: { Accept: 'application/octet-stream' },
    }),
}
