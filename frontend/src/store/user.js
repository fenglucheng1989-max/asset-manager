import { defineStore } from 'pinia'
import { request, requestRaw } from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    username: uni.getStorageSync('username') || '',
    email: uni.getStorageSync('email') || '',
    profile: uni.getStorageSync('userProfile') || null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(email, password) {
      const res = await request({
        url: '/api/v1/auth/login',
        method: 'POST',
        data: { email, password }
      })
      if (res.code === 200) {
        this.token = res.data.token
        this.username = res.data.username
        this.email = res.data.email
        uni.setStorageSync('token', this.token)
        uni.setStorageSync('username', this.username)
        uni.setStorageSync('email', this.email)
      }
      return res
    },
    async register(email, password, username, legal = {}) {
      const res = await request({
        url: '/api/v1/auth/register',
        method: 'POST',
        data: {
          email,
          password,
          username: username || undefined,
          acceptLegal: legal.acceptLegal,
          acceptedTermsVersion: legal.acceptedTermsVersion,
          acceptedPrivacyVersion: legal.acceptedPrivacyVersion
        }
      })
      if (res.code === 200) {
        this.token = res.data.token
        this.username = res.data.username
        this.email = res.data.email
        uni.setStorageSync('token', this.token)
        uni.setStorageSync('username', this.username)
        uni.setStorageSync('email', this.email)
      }
      return res
    },
    async fetchProfile() {
      const res = await request({ url: '/api/v1/user/profile', method: 'GET' })
      if (res.code === 200) {
        this.profile = res.data
        uni.setStorageSync('userProfile', res.data)
      }
      return res
    },
    async fetchLatestLegalDocuments() {
      return request({ url: '/api/v1/legal-documents/latest', method: 'GET' })
    },
    async fetchLatestLegalDocument(type) {
      return request({ url: `/api/v1/legal-documents/latest/${type}`, method: 'GET' })
    },
    async updateBaseCurrency(baseCurrency) {
      const res = await request({
        url: '/api/v1/user/profile/base-currency',
        method: 'PUT',
        data: { baseCurrency }
      })
      if (res.code === 200) {
        this.profile = res.data
        uni.setStorageSync('userProfile', res.data)
      }
      return res
    },
    async exportCsv(type) {
      return requestRaw({
        url: `/api/v1/user/data/export/${type}`,
        method: 'GET',
        header: { Accept: 'text/csv' },
        responseType: 'text'
      })
    },
    async exportBackup() {
      return request({
        url: '/api/v1/user/data/backup',
        method: 'GET'
      })
    },
    async restoreBackup(data) {
      return request({
        url: '/api/v1/user/data/restore',
        method: 'POST',
        data
      })
    },
    async clearData() {
      return request({
        url: '/api/v1/user/data',
        method: 'DELETE'
      })
    },
    async updateProfile(data) {
      const res = await request({
        url: '/api/v1/user/profile',
        method: 'PUT',
        data
      })
      if (res.code === 200) {
        this.profile = res.data
        uni.setStorageSync('userProfile', res.data)
      }
      return res
    },
    async changePassword(currentPassword, newPassword) {
      return request({
        url: '/api/v1/user/security/password',
        method: 'PUT',
        data: { currentPassword, newPassword }
      })
    },
    async deleteAccount(currentPassword) {
      return request({
        url: '/api/v1/user/account',
        method: 'DELETE',
        data: { currentPassword }
      })
    },
    async submitFeedback(data) {
      return request({
        url: '/api/v1/user/feedback',
        method: 'POST',
        data
      })
    },
    logout() {
      this.token = ''
      this.username = ''
      this.email = ''
      this.profile = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('username')
      uni.removeStorageSync('email')
      uni.removeStorageSync('userProfile')
    }
  }
})
