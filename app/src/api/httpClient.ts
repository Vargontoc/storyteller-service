import { apiConfig } from './config'

export class ApiError extends Error {
  readonly status?: number
  // Motivo estable del servidor (errors[0], p. ej. NAME_DUPLICATED), si lo informa.
  readonly code?: string

  constructor(message: string, status?: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function extractError(response: Response): Promise<{ message: string; code?: string }> {
  let code: string | undefined
  try {
    const body = await response.clone().json()
    if (Array.isArray(body?.errors) && typeof body.errors[0] === 'string') {
      code = body.errors[0]
    }
    if (typeof body?.message === 'string' && body.message) {
      return { message: body.message, code }
    }
    if (Array.isArray(body?.errors) && body.errors.length > 0) {
      return { message: body.errors.join(', '), code }
    }
  } catch {
    // Body wasn't JSON (or was empty) — fall through to the generic message below.
  }

  return { message: `Request failed: ${response.status} ${response.statusText}`, code }
}

async function requestRaw(path: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), apiConfig.timeoutMs)

  try {
    const response = await fetch(`${apiConfig.baseUrl}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body && !(init.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
      signal: controller.signal,
    })

    if (!response.ok) {
      const { message, code } = await extractError(response)
      throw new ApiError(message, response.status, code)
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
  // Multipart: el navegador fija Content-Type con su boundary.
  postForm: <T>(path: string, body: FormData) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForBlob: (path: string, body?: unknown) =>
    requestBlob(path, {
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
      headers: { Accept: 'application/octet-stream' },
    }),
  getForBlob: (path: string) => requestBlob(path, { method: 'GET', headers: { Accept: 'application/octet-stream' } }),
}
