import { defineStore } from 'pinia'
import { login } from '../api/admin'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    username: localStorage.getItem('admin_username') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(username, password) {
      const res = await login({ username, password })
      this.token = res.token
      this.username = username
      localStorage.setItem('admin_token', this.token)
      localStorage.setItem('admin_username', username)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_username')
    }
  }
})
