import { defineStore } from 'pinia'
import { request } from '../utils/request'

export const useAssetStore = defineStore('asset', {
  state: () => ({
    overview: {
      totalAsset: 0,
      totalLiability: 0,
      netWorth: 0,
      accountCount: 0,
      lastUpdateTime: null
    },
    accounts: []
  }),
  actions: {
    async fetchOverview() {
      const res = await request({ url: '/api/v1/asset/overview', method: 'GET' })
      if (res.code === 200) {
        this.overview = res.data
      }
    },
    async fetchAccounts() {
      const res = await request({ url: '/api/v1/asset/accounts', method: 'GET' })
      if (res.code === 200) {
        this.accounts = res.data
      }
    },
    async createAccount(data) {
      const res = await request({ url: '/api/v1/asset/accounts', method: 'POST', data })
      if (res.code === 200) {
        await this.fetchAccounts()
        await this.fetchOverview()
      }
      return res
    },
    async updateAccount(id, data) {
      const res = await request({ url: `/api/v1/asset/accounts/${id}`, method: 'PUT', data })
      if (res.code === 200) {
        await this.fetchAccounts()
        await this.fetchOverview()
      }
      return res
    },
    async deleteAccount(id) {
      const res = await request({ url: `/api/v1/asset/accounts/${id}`, method: 'DELETE' })
      if (res.code === 200) {
        await this.fetchAccounts()
        await this.fetchOverview()
      }
      return res
    },
    async updateSort(sortedIds) {
      const res = await request({ url: '/api/v1/asset/accounts/sort', method: 'PUT', data: { sortedIds } })
      if (res.code === 200) {
        await this.fetchAccounts()
      }
      return res
    }
  }
})
