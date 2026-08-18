import { defineStore } from 'pinia'
import { ApiError } from '../api/httpClient'
import { getServicesStatus, type ApiStateResponse } from '../api/services'

interface ServerStatusState {
  data: ApiStateResponse | null
  error: string | null
  loading: boolean
}

export const useServerStatusStore = defineStore('serverStatus', {
  state: (): ServerStatusState => ({
    data: null,
    error: null,
    loading: false,
  }),
  actions: {
    async fetchStatus() {
      this.loading = true
      this.error = null

      try {
        this.data = await getServicesStatus()
      } catch (error) {
        this.data = null
        this.error = error instanceof ApiError ? error.message : 'Unexpected error'
      } finally {
        this.loading = false
      }
    },
  },
})
