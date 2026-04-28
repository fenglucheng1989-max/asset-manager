import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxy = {
    '/api': {
      target: env.VITE_DEV_API_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true
    }
  }

  return {
  plugins: [vue()],
  server: {
    proxy: apiProxy
  },
  preview: {
    proxy: apiProxy
  }
  }
})
