<template>
  <view class="container">
    <view class="period-panel">
      <view class="period-tabs">
        <view
          v-for="item in periodOptions"
          :key="item.value"
          :class="['period-tab', { active: periodType === item.value }]"
          @click="setPeriod(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
      <view class="period-nav">
        <text class="period-nav-btn" @click="changePeriod(-1)">‹</text>
        <text class="period-label">{{ periodLabel }}</text>
        <text class="period-nav-btn" @click="changePeriod(1)">›</text>
      </view>
    </view>

    <view class="hero-card">
      <view class="hero-top">
        <view>
          <text class="hero-label">{{ periodTitle }}流水净额</text>
          <text :class="['hero-value', { negative: Number(report.net || 0) < 0 }]">{{ formatSignedMoney(report.net) }}</text>
        </view>
        <button class="hero-action" @click.stop="goCreate">记一笔</button>
      </view>
      <view class="hero-stats">
        <view>
          <text class="stat-label">流入</text>
          <text class="stat-value income">{{ formatMoney(report.income) }}</text>
        </view>
        <view>
          <text class="stat-label">流出</text>
          <text class="stat-value expense">{{ formatMoney(report.expense) }}</text>
        </view>
      </view>
    </view>

    <view class="tool-strip">
      <view class="tool-item" @click="goBudget">
        <text class="tool-label">月度预算</text>
        <text class="tool-value">{{ budgetSummary }}</text>
      </view>
    </view>

    <view class="filter-card">
      <view class="segmented">
        <view v-for="item in typeFilters" :key="item.value" :class="['segment', { active: filterType === item.value }]" @click="setType(item.value)">
          <text>{{ item.label }}</text>
        </view>
      </view>
      <picker :range="categoryOptions" range-key="name" :value="categoryIndex" @change="selectCategory">
        <view class="filter-line">
          <text class="filter-label">分类</text>
          <text class="filter-value">{{ selectedCategoryName }}</text>
          <text class="filter-arrow">›</text>
        </view>
      </picker>
    </view>

    <view class="panel-card" v-if="report.categoryStats && report.categoryStats.length">
      <view class="section-header">
        <text class="section-title">支出结构</text>
        <text class="section-subtitle">按分类占比</text>
      </view>
      <view class="category-stat" v-for="item in report.categoryStats.slice(0, 5)" :key="item.categoryName">
        <view class="stat-line-head">
          <text>{{ item.categoryName }}</text>
          <text>{{ formatPercent(item.percent) }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: formatPercent(item.percent), background: item.colorHex || '#2EBD85' }"></view>
        </view>
      </view>
    </view>

    <view class="panel-card">
      <view class="section-header">
        <text class="section-title">流水明细</text>
        <text class="section-subtitle">{{ records.length }} 条</text>
      </view>

      <view v-if="records.length > 0" class="record-list">
        <view class="record-item" v-for="item in records" :key="item.id" @click="goEdit(item)" @longpress="confirmDelete(item)">
          <view class="record-icon" :class="item.transactionType.toLowerCase()">
            <text>{{ getTypeSymbol(item.transactionType) }}</text>
          </view>
          <view class="record-copy">
            <text class="record-title">{{ item.categoryName || getTypeName(item.transactionType) }}</text>
            <text class="record-sub">{{ getRecordSub(item) }}</text>
          </view>
          <text :class="['record-amount', getAmountClass(item.transactionType)]">{{ getRecordAmount(item) }}</text>
        </view>
      </view>

      <view v-else class="empty-card">
        <text>当前筛选下暂无流水</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useTransactionStore } from '../../store/transaction'
import { formatMoney } from '../../utils/money'

export default {
  data() {
    return {
      records: [],
      categories: [],
      budgets: [],
      report: { income: 0, expense: 0, net: 0, categoryStats: [], trend: [] },
      anchorDate: '',
      currentMonth: '',
      periodType: 'month',
      filterType: '',
      filterCategoryId: null,
      periodOptions: [
        { label: '本周', value: 'week' },
        { label: '本月', value: 'month' },
        { label: '今年', value: 'year' }
      ],
      typeFilters: [
        { label: '全部', value: '' },
        { label: '支出', value: 'EXPENSE' },
        { label: '收入', value: 'INCOME' },
        { label: '转账', value: 'TRANSFER' }
      ],
      isLoading: false
    }
  },
  computed: {
    visibleCategories() {
      if (!this.filterType) return this.categories
      return this.categories.filter(item => item.type === this.filterType)
    },
    categoryOptions() {
      return [{ id: null, name: '全部分类' }, ...this.visibleCategories]
    },
    categoryIndex() {
      const index = this.categoryOptions.findIndex(item => item.id === this.filterCategoryId)
      return index < 0 ? 0 : index
    },
    selectedCategoryName() {
      const selected = this.categoryOptions[this.categoryIndex]
      return selected ? selected.name : '全部分类'
    },
    periodTitle() {
      const current = this.periodOptions.find(item => item.value === this.periodType)
      return current ? current.label : '本月'
    },
    periodLabel() {
      const date = this.parseAnchorDate()
      if (this.periodType === 'week') {
        const [startDate, endDate] = this.periodRange()
        return `${startDate.slice(5).replace('-', '.')} - ${endDate.slice(5).replace('-', '.')}`
      }
      if (this.periodType === 'year') return `${date.getFullYear()} 年`
      return this.formatMonthText(date)
    },
    budgetSummary() {
      if (!this.budgets.length) return '本月未设置'
      const used = this.budgets.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
      const total = this.budgets.reduce((sum, item) => sum + Number(item.amount || 0), 0)
      return `${formatMoney(used)} / ${formatMoney(total)}`
    }
  },
  onLoad() {
    const today = new Date()
    this.anchorDate = this.formatDate(today)
    this.currentMonth = this.formatMonth(today)
  },
  onShow() {
    this.refreshData()
  },
  methods: {
    formatMoney,
    async refreshData() {
      if (this.isLoading) return
      this.isLoading = true
      try {
        const store = useTransactionStore()
        const [startDate, endDate] = this.periodRange()
        await Promise.all([
          store.fetchCategories(),
          store.fetchBudgets(this.currentMonth),
          store.fetchRecords({
            limit: 200,
            type: this.filterType,
            categoryId: this.filterCategoryId,
            startDate,
            endDate
          })
        ])
        this.categories = Array.isArray(store.categories) ? [...store.categories] : []
        this.budgets = Array.isArray(store.budgets) ? [...store.budgets] : []
        this.records = Array.isArray(store.records) ? [...store.records] : []
        this.report = this.buildReport(this.records)
      } finally {
        this.isLoading = false
      }
    },
    buildReport(records, fallback = {}) {
      const income = records
        .filter(item => item.transactionType === 'INCOME')
        .reduce((sum, item) => sum + Number(item.baseAmount || item.amount || 0), 0)
      const expense = records
        .filter(item => item.transactionType === 'EXPENSE')
        .reduce((sum, item) => sum + Number(item.baseAmount || item.amount || 0), 0)
      const expenseRecords = records.filter(item => item.transactionType === 'EXPENSE')
      const grouped = expenseRecords.reduce((map, item) => {
        const key = item.categoryId || 'none'
        const current = map[key] || {
          categoryName: item.categoryName || '未分类',
          colorHex: this.getCategoryColor(item.categoryId),
          amount: 0
        }
        current.amount += Number(item.baseAmount || item.amount || 0)
        map[key] = current
        return map
      }, {})
      const categoryStats = Object.values(grouped)
        .map(item => ({
          ...item,
          percent: expense > 0 ? item.amount / expense : 0
        }))
        .sort((a, b) => b.amount - a.amount)
      return {
        ...fallback,
        income,
        expense,
        net: income - expense,
        categoryStats
      }
    },
    getCategoryColor(categoryId) {
      const category = this.categories.find(item => item.id === categoryId)
      return category ? category.colorHex : '#64748B'
    },
    setPeriod(type) {
      this.periodType = type
      this.filterCategoryId = null
      this.refreshData()
    },
    changePeriod(delta) {
      const date = this.parseAnchorDate()
      if (this.periodType === 'week') {
        date.setDate(date.getDate() + delta * 7)
      } else if (this.periodType === 'year') {
        date.setFullYear(date.getFullYear() + delta)
      } else {
        date.setMonth(date.getMonth() + delta)
      }
      this.anchorDate = this.formatDate(date)
      this.currentMonth = this.formatMonth(date)
      this.refreshData()
    },
    setType(type) {
      this.filterType = type
      this.filterCategoryId = null
      this.refreshData()
    },
    setCategory(id) {
      this.filterCategoryId = id
      this.refreshData()
    },
    selectCategory(event) {
      const index = Number(event.detail.value)
      const selected = this.categoryOptions[index] || this.categoryOptions[0]
      this.setCategory(selected.id || null)
    },
    formatMonth(date) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    },
    formatMonthText(date) {
      return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`
    },
    parseAnchorDate() {
      if (!this.anchorDate) return new Date()
      const [year, month, day] = this.anchorDate.split('-').map(Number)
      return new Date(year, month - 1, day)
    },
    monthRange(month) {
      const [year, value] = month.split('-').map(Number)
      const start = new Date(year, value - 1, 1)
      const end = new Date(year, value, 0)
      return [this.formatDate(start), this.formatDate(end)]
    },
    periodRange() {
      const now = this.parseAnchorDate()
      if (this.periodType === 'week') {
        const day = now.getDay() || 7
        const start = new Date(now)
        start.setDate(now.getDate() - day + 1)
        const end = new Date(start)
        end.setDate(start.getDate() + 6)
        return [this.formatDate(start), this.formatDate(end)]
      }
      if (this.periodType === 'year') {
        return [`${now.getFullYear()}-01-01`, `${now.getFullYear()}-12-31`]
      }
      return this.monthRange(this.currentMonth)
    },
    formatDate(date) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    },
    formatSignedMoney(value) {
      const amount = Number(value || 0)
      if (amount === 0) return '¥0.00'
      return `${amount > 0 ? '+' : '-'}${formatMoney(Math.abs(amount))}`
    },
    formatPercent(value) {
      return `${Math.min(100, Math.round(Number(value || 0) * 100))}%`
    },
    getTypeName(type) {
      const map = { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账' }
      return map[type] || type
    },
    getTypeSymbol(type) {
      const map = { INCOME: '+', EXPENSE: '-', TRANSFER: '⇄' }
      return map[type] || '·'
    },
    getAmountClass(type) {
      if (type === 'INCOME') return 'income'
      if (type === 'EXPENSE') return 'expense'
      return ''
    },
    getRecordAmount(item) {
      const amount = formatMoney(item.baseAmount || item.amount)
      if (item.transactionType === 'INCOME') return `+${amount}`
      if (item.transactionType === 'EXPENSE') return `-${amount}`
      return amount
    },
    getRecordSub(item) {
      const date = this.formatDisplayDate(item.occurredAt)
      if (item.transactionType === 'TRANSFER') {
        return `${item.accountName || '-'} → ${item.targetAccountName || '-'} · ${date}`
      }
      const tag = item.tag ? ` · ${item.tag}` : ''
      return `${item.accountName || '-'}${tag} · ${date}`
    },
    formatDisplayDate(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      return `${date.getMonth() + 1}月${date.getDate()}日`
    },
    goCreate() {
      uni.navigateTo({ url: '/pages/transaction/form' })
    },
    goEdit(item) {
      uni.navigateTo({ url: `/pages/transaction/form?id=${item.id}` })
    },
    goBudget() {
      uni.navigateTo({ url: `/pages/transaction/budget?month=${this.currentMonth}` })
    },
    confirmDelete(item) {
      uni.showModal({
        title: '删除流水',
        content: '删除后会自动撤销这笔流水对账户余额的影响。',
        success: async (result) => {
          if (!result.confirm) return
          const store = useTransactionStore()
          const res = await store.deleteRecord(item.id)
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            this.refreshData()
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 20rpx 22rpx calc(144rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.hero-card {
  padding: 30rpx 28rpx 28rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #17202a 0%, #164d45 100%);
  color: #fff;
  margin-bottom: 18rpx;
}

.period-panel {
  padding: 10rpx;
  margin-bottom: 16rpx;
  border-radius: 20rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 22rpx rgba(26, 42, 58, 0.045);
}

.hero-top,
.hero-stats,
.section-header,
.record-item,
.stat-line-head,
.tool-strip,
.filter-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.hero-label,
.stat-label,
.section-subtitle,
.record-sub {
  display: block;
  color: #7f8ea3;
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-label {
  color: rgba(255, 255, 255, 0.76);
}

.hero-value {
  display: block;
  margin-top: 8rpx;
  color: #ffd166;
  font-size: 54rpx;
  line-height: 64rpx;
  font-weight: 850;
  word-break: break-all;
}

.hero-value.negative {
  color: #ff8fa3;
}

.hero-action {
  flex-shrink: 0;
  width: 148rpx;
  height: 64rpx;
  line-height: 64rpx;
  margin: 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  padding: 0;
}

.hero-stats {
  margin-top: 24rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.14);
}

.stat-value {
  display: block;
  margin-top: 8rpx;
  font-size: 31rpx;
  font-weight: 850;
}

.stat-value.income,
.record-amount.income {
  color: #1f8f72;
}

.stat-value.expense,
.record-amount.expense {
  color: #d94a62;
}

.filter-card,
.panel-card {
  background: #fff;
  border-radius: 18rpx;
  box-shadow: 0 8rpx 22rpx rgba(26, 42, 58, 0.045);
}

.swipe-hint {
  display: block;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.42);
  font-size: 22rpx;
  line-height: 30rpx;
  text-align: center;
}

.tool-strip {
  margin-bottom: 18rpx;
  gap: 16rpx;
}

.tool-item {
  flex: 1;
  min-width: 0;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 22rpx rgba(26, 42, 58, 0.045);
}

.tool-label,
.tool-value {
  display: block;
}

.tool-label {
  color: #17202a;
  font-size: 26rpx;
  line-height: 34rpx;
  font-weight: 800;
}

.tool-value {
  margin-top: 6rpx;
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.filter-card,
.panel-card {
  padding: 24rpx;
  margin-bottom: 18rpx;
}

.period-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4rpx;
  padding: 4rpx;
  border-radius: 16rpx;
  background: #f3f6f8;
}

.period-tab {
  height: 58rpx;
  border-radius: 13rpx;
  color: #7b8798;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 25rpx;
  font-weight: 800;
}

.period-tab.active {
  background: #17202a;
  color: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(23, 32, 42, 0.12);
}

.period-nav {
  height: 72rpx;
  padding: 0 8rpx;
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.period-label {
  color: #17202a;
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 850;
}

.period-nav-btn {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  background: #f2f6f4;
  color: #226f63;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42rpx;
  line-height: 50rpx;
}

.segmented {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4rpx;
  padding: 4rpx;
  border-radius: 14rpx;
  background: #f4f7f8;
  margin-bottom: 14rpx;
}

.segment {
  height: 58rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 26rpx;
  font-weight: 750;
}

.segment.active {
  background: #17202a;
  color: #fff;
  box-shadow: 0 8rpx 18rpx rgba(23, 32, 42, 0.12);
}

.filter-line {
  height: 72rpx;
  padding: 0 18rpx 0 22rpx;
  border-radius: 14rpx;
  background: #fafbfc;
}

.filter-label,
.filter-value {
  display: block;
}

.filter-label {
  color: #7b8798;
  font-size: 25rpx;
  line-height: 34rpx;
}

.filter-value {
  flex: 1;
  min-width: 0;
  text-align: right;
  color: #17202a;
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
  padding: 0 14rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.filter-arrow {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: #eef5f2;
  color: #226f63;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  line-height: 44rpx;
}

.section-header {
  align-items: flex-start;
  margin-bottom: 18rpx;
}

.section-title {
  color: #17202a;
  font-size: 31rpx;
  font-weight: 850;
}

.category-stat {
  margin-top: 18rpx;
}

.stat-line-head {
  color: #334155;
  font-size: 26rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.progress-track {
  height: 10rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
}

.record-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf1f4;
}

.record-item:last-child {
  border-bottom: none;
}

.record-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef5f2;
  color: #226f63;
  font-size: 28rpx;
  font-weight: 850;
  flex-shrink: 0;
}

.record-icon.expense {
  background: #fff1f3;
  color: #d94a62;
}

.record-icon.transfer {
  background: #f2f5f7;
  color: #64748b;
}

.record-copy {
  flex: 1;
  min-width: 0;
}

.record-title {
  display: block;
  color: #17202a;
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 800;
}

.record-amount {
  color: #17202a;
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 850;
  flex-shrink: 0;
}

.empty-card {
  padding: 50rpx 0;
  text-align: center;
  color: #7b8798;
  font-size: 26rpx;
}
</style>
