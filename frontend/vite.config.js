import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devApiProxyTarget = process.env.VITE_DEV_API_PROXY_TARGET || env.VITE_DEV_API_PROXY_TARGET || 'http://localhost:8080'
  const apiProxy = {
    '/api': {
      target: devApiProxyTarget,
      changeOrigin: true
    }
  }

  return {
    plugins: [uni()],
    server: {
      proxy: apiProxy
    },
    preview: {
      proxy: apiProxy
    }
  }
})
