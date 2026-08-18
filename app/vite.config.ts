import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.APP_PORT) || 80

  return {
    plugins: [vue()],
    server: {
      port,
      strictPort: true,
    },
    preview: {
      port,
      strictPort: true,
    },
  }
})
