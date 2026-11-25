import { apiConfig } from './config'

export class ApiError extends Error {
  readonly status?: number
  // Motivo estable del servidor (errors[0], p. ej. NAME_DUPLICATED), si lo informa.
  readonly code?: string
  // Carga útil (`data`) de la respuesta de error, si el servidor la envía (p. ej. la lista de faltas).
  readonly data?: unknown

  constructor(message: string, status?: number, code?: string, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.data = data
  }
}

async function extractError(
  response: Response,
): Promise<{ message: string; code?: string; data?: unknown }> {
  let code: string | undefined
  let data: unknown
  try {
    const body = await response.clone().json()
    data = body?.data ?? undefined
    if (Array.isArray(body?.errors) && typeof body.errors[0] === 'string') {
      code = body.errors[0]
    }
    if (typeof body?.message === 'string' && body.message) {
      return { message: body.message, code, data }
    }
    if (Array.isArray(body?.errors) && body.errors.length > 0) {
      return { message: body.errors.join(', '), code, data }
    }
  } catch {
    // Body wasn't JSON (or was empty) — fall through to the generic message below.
  }

  return { message: `Request failed: ${response.status} ${response.statusText}`, code, data }
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
      const { message, code, data } = await extractError(response)
      throw new ApiError(message, response.status, code, data)
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

// Nombre de fichero de Content-Disposition (`filename="x.zip"`), si viene.
function filenameOf(response: Response): string | undefined {
  const header = response.headers.get('Content-Disposition')
  return header?.match(/filename="?([^";]+)"?/i)?.[1]
}

async function requestFile(path: string): Promise<{ blob: Blob; filename?: string }> {
  const response = await requestRaw(path, { method: 'GET', headers: { Accept: 'application/octet-stream' } })
  return { blob: await response.blob(), filename: filenameOf(response) }
}

export const httpClient = {
  getForFile: (path: string) => requestFile(path),
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
