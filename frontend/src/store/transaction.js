import { defineStore } from 'pinia'
import { request } from '../utils/request'

export const useTransactionStore = defineStore('transaction', {
  state: () => ({
    records: [],
    categories: [],
    budgets: [],
    report: {
      income: 0,
      expense: 0,
      net: 0,
      categoryStats: [],
      trend: []
    }
  }),
  actions: {
    async fetchRecords(params = {}) {
      const normalized = typeof params === 'number' ? { limit: params } : params
      const query = buildQuery({ limit: 100, ...normalized })
      const res = await request({ url: `/api/v1/transactions${query}`, method: 'GET' })
      if (res.code === 200) {
        this.records = Array.isArray(res.data) ? res.data : []
      }
      return res
    },
    async fetchCategories(type = '') {
      const query = type ? `?type=${type}` : ''
      const res = await request({ url: `/api/v1/transactions/categories${query}`, method: 'GET' })
      if (res.code === 200) {
        this.categories = Array.isArray(res.data) ? res.data : []
      }
      return res
    },
    async createCategory(data) {
      const res = await request({ url: '/api/v1/transactions/categories', method: 'POST', data })
      if (res.code === 200) await this.fetchCategories(data.type || '')
      return res
    },
    async updateCategory(id, data) {
      const res = await request({ url: `/api/v1/transactions/categories/${id}`, method: 'PUT', data })
      if (res.code === 200) await this.fetchCategories(data.type || '')
      return res
    },
    async deleteCategory(id) {
      const res = await request({ url: `/api/v1/transactions/categories/${id}`, method: 'DELETE' })
      if (res.code === 200) await this.fetchCategories()
      return res
    },
    async createRecord(data) {
      const res = await request({ url: '/api/v1/transactions', method: 'POST', data })
      if (res.code === 200) {
        await this.fetchRecords()
      }
      return res
    },
    async updateRecord(id, data) {
      const res = await request({ url: `/api/v1/transactions/${id}`, method: 'PUT', data })
      if (res.code === 200) {
        await this.fetchRecords()
      }
      return res
    },
    async deleteRecord(id) {
      const res = await request({ url: `/api/v1/transactions/${id}`, method: 'DELETE' })
      if (res.code === 200) {
        await this.fetchRecords()
      }
      return res
    },
    async fetchBudgets(month) {
      const query = buildQuery({ month })
      const res = await request({ url: `/api/v1/transactions/budgets${query}`, method: 'GET' })
      if (res.code === 200) {
        this.budgets = Array.isArray(res.data) ? res.data : []
      }
      return res
    },
    async saveBudget(data) {
      const res = await request({ url: '/api/v1/transactions/budgets', method: 'POST', data })
      if (res.code === 200) await this.fetchBudgets(data.budgetMonth)
      return res
    },
    async deleteBudget(id, month) {
      const res = await request({ url: `/api/v1/transactions/budgets/${id}`, method: 'DELETE' })
      if (res.code === 200) await this.fetchBudgets(month)
      return res
    },
    async fetchReport(month) {
      const query = buildQuery({ month })
      const res = await request({ url: `/api/v1/transactions/report${query}`, method: 'GET' })
      if (res.code === 200) {
        this.report = res.data || this.report
      }
      return res
    }
  }
})

function buildQuery(params) {
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  if (entries.length === 0) return ''
  return `?${entries.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')}`
}
