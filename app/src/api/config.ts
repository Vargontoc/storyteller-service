export const apiConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeoutMs: Number(import.meta.env.VITE_API_TIMEOUT_MS) || 180_000,
} as const
