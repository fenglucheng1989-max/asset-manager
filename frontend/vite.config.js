import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: apiProxy
  },
  preview: {
    proxy: apiProxy
  }
})
