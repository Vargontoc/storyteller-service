export const apiConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeoutMs: 10_000,
} as const
