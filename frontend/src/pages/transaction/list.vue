<template>
  <view class="container">
    <view class="ledger-toolbar">
      <view class="date-nav">
        <button class="date-nav-btn" @click="changePeriod(-1)">‹</button>
        <picker :range="periodOptions" range-key="label" :value="periodIndex" @change="selectPeriodMode">
          <view class="date-title">
            <text class="date-label">{{ periodLabel }}</text>
            <text class="period-mode">{{ periodTitle }} ▾</text>
          </view>
        </picker>
        <button class="date-nav-btn" @click="changePeriod(1)">›</button>
      </view>
      <view class="filter-row">
        <scroll-view class="type-scroll" scroll-x>
          <view class="type-chip-row">
            <view v-for="item in typeFilters" :key="item.value" :class="['type-chip', { active: filterType === item.value }]" @click="setType(item.value)">
              <text>{{ item.label }}</text>
            </view>
          </view>
        </scroll-view>
        <picker :disabled="isCategoryDisabled" :range="categoryOptions" range-key="name" :value="categoryIndex" @change="selectCategory">
          <view :class="['category-pill', { disabled: isCategoryDisabled }]">
            <text>{{ isCategoryDisabled ? '无分类' : selectedCategoryName }}</text>
            <text class="category-arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <view class="hero-card">
      <view class="hero-top">
        <view>
          <text class="hero-label">{{ periodTitle }}收支净额</text>
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

    <view class="budget-summary-card" :class="{ warning: budgetStatusLevel === 'warning', danger: budgetStatusLevel === 'danger' }" @click="goBudget">
      <view class="budget-summary-head">
        <view>
          <text class="budget-title">月度预算</text>
          <text class="budget-subtitle">{{ budgetStatusText }}</text>
        </view>
        <text class="budget-action">›</text>
      </view>
      <view class="budget-progress-track">
        <view class="budget-progress-fill" :style="{ width: budgetProgressPercent }"></view>
      </view>
      <view class="budget-summary-foot">
        <text>{{ budgetUsageText }}</text>
        <text>{{ budgetBalanceText }}</text>
      </view>
    </view>

    <view class="panel-card structure-card" v-if="hasStructureStats">
      <view class="section-header">
        <text class="section-title">收支结构</text>
        <view class="structure-switch">
          <text :class="['structure-switch-item', { active: structureType === 'EXPENSE' }]" @click="structureType = 'EXPENSE'">支出</text>
          <text :class="['structure-switch-item', { active: structureType === 'INCOME' }]" @click="structureType = 'INCOME'">收入</text>
        </view>
      </view>
      <view class="pie-layout">
        <view class="pie-ring" :style="{ background: buildPieStyle(activeStructureStats) }">
          <view class="pie-center">
            <text class="pie-label">{{ structureType === 'EXPENSE' ? '流出' : '流入' }}</text>
            <text class="pie-value">{{ formatMoney(activeStructureTotal) }}</text>
          </view>
        </view>
        <view class="pie-legend">
          <view class="legend-item" v-for="item in activeStructureStats.slice(0, 5)" :key="`${structureType}-${item.categoryName}`">
            <view class="legend-left">
              <text class="legend-dot" :style="{ backgroundColor: item.colorHex || defaultStructureColor }"></text>
              <text class="legend-name">{{ item.categoryName }}</text>
            </view>
            <text class="legend-percent">{{ formatPercent(item.percent) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="panel-card">
      <view class="section-header">
        <text class="section-title">收支明细</text>
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
        <text>当前筛选下暂无记录</text>
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
      report: { income: 0, expense: 0, net: 0, categoryStats: [], incomeCategoryStats: [], trend: [] },
      anchorDate: '',
      currentMonth: '',
      periodType: 'month',
      filterType: '',
      filterCategoryId: null,
      structureType: 'EXPENSE',
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
      return this.categories.filter(item => item.type !== 'TRANSFER')
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
    selectedTypeName() {
      const selected = this.typeFilters.find(item => item.value === this.filterType)
      return selected ? selected.label : '全部'
    },
    isCategoryDisabled() {
      return this.filterType === 'TRANSFER'
    },
    expenseStats() {
      return Array.isArray(this.report.categoryStats) ? this.report.categoryStats : []
    },
    incomeStats() {
      return Array.isArray(this.report.incomeCategoryStats) ? this.report.incomeCategoryStats : []
    },
    hasExpenseStats() {
      return this.expenseStats.length > 0
    },
    hasIncomeStats() {
      return this.incomeStats.length > 0
    },
    hasStructureStats() {
      return this.hasExpenseStats || this.hasIncomeStats
    },
    activeStructureStats() {
      if (this.structureType === 'INCOME') return this.incomeStats
      return this.expenseStats
    },
    activeStructureTotal() {
      return this.structureType === 'INCOME' ? this.report.income : this.report.expense
    },
    defaultStructureColor() {
      return this.structureType === 'INCOME' ? '#1f8f72' : '#d94a62'
    },
    periodTitle() {
      const current = this.periodOptions.find(item => item.value === this.periodType)
      return current ? current.label : '本月'
    },
    periodIndex() {
      const index = this.periodOptions.findIndex(item => item.value === this.periodType)
      return index < 0 ? 1 : index
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
    budgetTotals() {
      const used = this.budgets.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
      const total = this.budgets.reduce((sum, item) => sum + Number(item.amount || 0), 0)
      return {
        used,
        total,
        remaining: total - used,
        rate: total > 0 ? used / total : 0
      }
    },
    budgetStatusLevel() {
      if (!this.budgets.length || this.budgetTotals.total <= 0) return 'empty'
      if (this.budgetTotals.remaining < 0) return 'danger'
      if (this.budgets.some(item => item.warning) || this.budgetTotals.rate >= 0.8) return 'warning'
      return 'normal'
    },
    budgetStatusText() {
      if (!this.budgets.length || this.budgetTotals.total <= 0) return '本月未设置预算'
      if (this.budgetStatusLevel === 'danger') return '已超出预算'
      if (this.budgetStatusLevel === 'warning') return '接近预算上限'
      return '预算使用正常'
    },
    budgetUsageText() {
      if (!this.budgets.length || this.budgetTotals.total <= 0) return '去设置本月支出上限'
      return `已用 ${formatMoney(this.budgetTotals.used)} / ${formatMoney(this.budgetTotals.total)}`
    },
    budgetBalanceText() {
      if (!this.budgets.length || this.budgetTotals.total <= 0) return ''
      const amount = Math.abs(this.budgetTotals.remaining)
      return this.budgetTotals.remaining < 0 ? `超支 ${formatMoney(amount)}` : `剩余 ${formatMoney(amount)}`
    },
    budgetProgressPercent() {
      if (!this.budgets.length || this.budgetTotals.total <= 0) return '0%'
      return `${Math.min(100, Math.round(this.budgetTotals.rate * 100))}%`
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
            categoryId: this.isCategoryDisabled ? null : this.filterCategoryId,
            startDate,
            endDate
          })
        ])
        this.categories = Array.isArray(store.categories) ? [...store.categories] : []
        this.budgets = Array.isArray(store.budgets) ? [...store.budgets] : []
        this.records = Array.isArray(store.records) ? [...store.records] : []
        this.report = this.buildReport(this.records)
        if (this.structureType === 'EXPENSE' && !this.hasExpenseStats && this.hasIncomeStats) {
          this.structureType = 'INCOME'
        } else if (this.structureType === 'INCOME' && !this.hasIncomeStats && this.hasExpenseStats) {
          this.structureType = 'EXPENSE'
        }
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
      const categoryStats = this.buildCategoryStats(records, 'EXPENSE', expense)
      const incomeCategoryStats = this.buildCategoryStats(records, 'INCOME', income)
      return {
        ...fallback,
        income,
        expense,
        net: income - expense,
        categoryStats,
        incomeCategoryStats
      }
    },
    buildCategoryStats(records, type, total) {
      const grouped = records.filter(item => item.transactionType === type).reduce((map, item) => {
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
      return Object.values(grouped)
        .map(item => ({
          ...item,
          percent: total > 0 ? item.amount / total : 0
        }))
        .sort((a, b) => b.amount - a.amount)
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
    selectPeriodMode(event) {
      const index = Number(event.detail.value)
      const option = this.periodOptions[index] || this.periodOptions[1]
      this.setPeriod(option.value)
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
      this.refreshData()
    },
    setCategory(id) {
      this.filterCategoryId = id
      this.refreshData()
    },
    selectCategory(event) {
      if (this.isCategoryDisabled) return
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
    buildPieStyle(items) {
      if (!items.length) return '#edf2f5'
      let start = 0
      const segments = items.map((item, index) => {
        const percent = Math.max(0, Number(item.percent || 0) * 100)
        const end = index === items.length - 1 ? 100 : Math.min(100, start + percent)
        const color = item.colorHex || (index % 2 === 0 ? '#2EBD85' : '#5B8FF9')
        const segment = `${color} ${start}% ${end}%`
        start = end
        return segment
      })
      return `conic-gradient(${segments.join(', ')})`
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
        title: '删除记录',
        content: '删除后会自动撤销这笔记录对账户余额的影响。',
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

.ledger-toolbar {
  margin: 0 4rpx 14rpx;
}

.hero-top,
.hero-stats,
.section-header,
.record-item,
.budget-summary-head,
.budget-summary-foot,
.date-nav,
.filter-row,
.legend-item,
.legend-left {
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

.budget-summary-card {
  padding: 22rpx 24rpx;
  margin-bottom: 18rpx;
  border-radius: 18rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 22rpx rgba(26, 42, 58, 0.045);
  border: 1rpx solid #edf1f4;
}

.budget-summary-card.warning {
  border-color: #ffe0a8;
  background: #fffdf8;
}

.budget-summary-card.danger {
  border-color: #ffd4dc;
  background: #fff9fa;
}

.budget-title,
.budget-subtitle,
.budget-action {
  display: block;
}

.budget-title {
  color: #17202a;
  font-size: 26rpx;
  line-height: 34rpx;
  font-weight: 800;
}

.budget-subtitle {
  margin-top: 6rpx;
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
}

.budget-summary-card.warning .budget-subtitle {
  color: #b7791f;
}

.budget-summary-card.danger .budget-subtitle {
  color: #d94a62;
}

.budget-action {
  width: 42rpx;
  height: 42rpx;
  line-height: 38rpx;
  text-align: center;
  border-radius: 50%;
  background: #eef5f2;
  color: #226f63;
  font-size: 34rpx;
  font-weight: 500;
  flex-shrink: 0;
}

.budget-progress-track {
  height: 12rpx;
  margin: 18rpx 0 12rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
}

.budget-progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: #2ebd85;
}

.budget-summary-card.warning .budget-progress-fill {
  background: #f5a623;
}

.budget-summary-card.danger .budget-progress-fill {
  background: #d94a62;
}

.budget-summary-foot {
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
}

.budget-summary-card.danger .budget-summary-foot text:last-child {
  color: #d94a62;
  font-weight: 800;
}

.panel-card {
  padding: 24rpx;
  margin-bottom: 18rpx;
}

.date-nav {
  height: 54rpx;
  padding: 0;
}

.date-title {
  min-width: 260rpx;
  text-align: center;
}

.date-label {
  display: inline;
  color: #17202a;
  font-size: 30rpx;
  line-height: 38rpx;
  font-weight: 850;
  margin-right: 12rpx;
}

.period-mode {
  color: #7b8798;
  font-size: 22rpx;
  line-height: 30rpx;
  font-weight: 750;
}

.date-nav-btn {
  margin: 0;
  width: 42rpx;
  height: 42rpx;
  line-height: 42rpx;
  padding: 0;
  border-radius: 50%;
  background: #ffffff;
  color: #226f63;
  font-size: 32rpx;
  font-weight: 500;
  box-shadow: 0 6rpx 16rpx rgba(26, 42, 58, 0.045);
}

.filter-row {
  align-items: center;
  margin-top: 8rpx;
}

.type-scroll {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}

.type-chip-row {
  display: flex;
  gap: 28rpx;
  width: max-content;
  padding: 0 2rpx;
}

.type-chip {
  position: relative;
  height: 44rpx;
  line-height: 44rpx;
  padding: 0;
  background: transparent;
  color: #64748b;
  font-size: 25rpx;
  font-weight: 800;
}

.type-chip.active {
  background: transparent;
  color: #17202a;
  box-shadow: none;
}

.type-chip.active::after {
  content: '';
  position: absolute;
  left: 8rpx;
  right: 8rpx;
  bottom: 0;
  height: 5rpx;
  border-radius: 999rpx;
  background: #2ebd85;
}

.category-pill {
  height: 44rpx;
  max-width: 184rpx;
  padding: 0 2rpx 0 16rpx;
  border-left: 1rpx solid #dbe3ea;
  background: transparent;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 25rpx;
  font-weight: 800;
}

.category-pill.disabled {
  color: #a8b2bd;
  border-left-color: #e5e9ee;
}

.category-pill text:first-child {
  min-width: 0;
  max-width: 140rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-arrow {
  color: #226f63;
  font-size: 28rpx;
  line-height: 28rpx;
}

.category-pill.disabled .category-arrow {
  color: #c2cad3;
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

.structure-card {
  margin-bottom: 18rpx;
  padding-bottom: 22rpx;
}

.pie-layout {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.pie-ring {
  width: 168rpx;
  height: 168rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pie-center {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1rpx #edf1f4;
}

.pie-label {
  color: #7b8798;
  font-size: 20rpx;
  line-height: 26rpx;
}

.pie-value {
  max-width: 86rpx;
  color: #17202a;
  font-size: 19rpx;
  line-height: 26rpx;
  font-weight: 850;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pie-legend {
  flex: 1;
  min-width: 0;
}

.legend-item {
  padding: 6rpx 0;
}

.legend-left {
  justify-content: flex-start;
  min-width: 0;
  gap: 10rpx;
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name {
  color: #334155;
  font-size: 24rpx;
  line-height: 32rpx;
  font-weight: 750;
  max-width: 170rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-percent {
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
  flex-shrink: 0;
}

.structure-switch {
  display: flex;
  gap: 6rpx;
  padding: 4rpx;
  border-radius: 999rpx;
  background: #f3f6f8;
}

.structure-switch-item {
  min-width: 70rpx;
  height: 42rpx;
  line-height: 42rpx;
  border-radius: 999rpx;
  text-align: center;
  color: #7b8798;
  font-size: 23rpx;
  font-weight: 800;
}

.structure-switch-item.active {
  background: #17202a;
  color: #ffffff;
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
