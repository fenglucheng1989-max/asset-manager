import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = Pinia.createPinia()

  app.config.errorHandler = (err, instance, info) => {
    const message = err && err.message ? err.message : String(err)
    console.error('App error:', info, message)
    try {
      uni.setStorageSync('__last_app_error__', `${info || 'runtime'}: ${message}`)
    } catch (storageError) {
      console.error('Save app error failed:', storageError)
    }
  }

  app.use(pinia)
  return {
    app,
    Pinia
  }
}
