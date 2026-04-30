import { defineStore } from 'pinia'
import { request } from '../utils/request'

export const useInsightStore = defineStore('insight', {
  state: () => ({
    healthScore: null,
    summaries: [],
    milestones: []
  }),
  actions: {
    async fetchHealthScore() {
      const res = await request({ url: '/api/v1/insight/health-score', method: 'GET' })
      if (res.code === 200) {
        this.healthScore = res.data
      }
      return res
    },
    async fetchSummaries(limit = 12) {
      const res = await request({ url: `/api/v1/insight/summaries?limit=${limit}`, method: 'GET' })
      if (res.code === 200) {
        this.summaries = Array.isArray(res.data) ? res.data : []
      }
      return res
    },
    async fetchMilestones() {
      const res = await request({ url: '/api/v1/insight/milestones', method: 'GET' })
      if (res.code === 200) {
        this.milestones = Array.isArray(res.data) ? res.data : []
      }
      return res
    },
    async createMilestone(data) {
      const res = await request({ url: '/api/v1/insight/milestones', method: 'POST', data })
      if (res.code === 200) {
        await this.fetchMilestones()
      }
      return res
    }
  }
})
