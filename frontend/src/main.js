import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = Pinia.createPinia()

  app.config.errorHandler = (err, instance, info) => {
    const message = err && err.message ? err.message : String(err)
    console.error('应用错误：', info, message)
    try {
      uni.setStorageSync('__last_app_error__', `${info || 'runtime'}: ${message}`)
    } catch (storageError) {
      console.error('保存应用错误失败：', storageError)
    }
  }

  app.use(pinia)
  return {
    app,
    Pinia
  }
}
