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
    accounts: [],
    snapshots: []
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
    async fetchAccount(id) {
      return request({ url: `/api/v1/asset/accounts/${id}`, method: 'GET' })
    },
    async fetchAccountHistory(id, limit = 50) {
      return request({ url: `/api/v1/asset/accounts/${id}/history?limit=${limit}`, method: 'GET' })
    },
    async fetchSnapshots(options = 30) {
      const params = typeof options === 'number'
        ? { limit: options, offset: 0, mode: 'replace' }
        : { limit: 30, offset: 0, mode: 'replace', ...options }
      const res = await request({
        url: `/api/v1/asset/snapshots?limit=${params.limit}&offset=${params.offset}`,
        method: 'GET'
      })
      if (res.code === 200) {
        const rows = Array.isArray(res.data) ? res.data : []
        if (params.mode === 'prepend') {
          this.snapshots = [...rows, ...this.snapshots]
        } else if (params.mode === 'append') {
          this.snapshots = [...this.snapshots, ...rows]
        } else {
          this.snapshots = rows
        }
      }
      return res
    },
    async createSnapshot() {
      const res = await request({ url: '/api/v1/asset/snapshots', method: 'POST' })
      if (res.code === 200) {
        await this.fetchSnapshots()
      }
      return res
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
    async archiveAccount(id, archived = true) {
      const res = await request({ url: `/api/v1/asset/accounts/${id}/archive`, method: 'PUT', data: { archived } })
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
