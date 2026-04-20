import { defineStore } from 'pinia'
import { request } from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    username: uni.getStorageSync('username') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(username, password) {
      const res = await request({
        url: '/api/v1/auth/login',
        method: 'POST',
        data: { username, password }
      })
      if (res.code === 200) {
        this.token = res.data.token
        this.username = username
        uni.setStorageSync('token', this.token)
        uni.setStorageSync('username', username)
      }
      return res
    },
    async register(username, password, email) {
      const res = await request({
        url: '/api/v1/auth/register',
        method: 'POST',
        data: { username, password, email }
      })
      if (res.code === 200) {
        this.token = res.data.token
        this.username = username
        uni.setStorageSync('token', this.token)
        uni.setStorageSync('username', username)
      }
      return res
    },
    logout() {
      this.token = ''
      this.username = ''
      uni.removeStorageSync('token')
      uni.removeStorageSync('username')
    }
  }
})
