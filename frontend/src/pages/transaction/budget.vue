<template>
  <view class="container" :style="themeVars">
    <!-- Unified hero card: date + tabs on one row, then summary -->
    <view class="hero-card">
      <view class="hero-nav-row">
        <view class="date-nav-group">
          <button class="date-nav-btn" :disabled="activeTab === 'all'" @click="shiftPeriod(-1)">‹</button>
          <view class="date-pill">
            <view class="date-pill-icon"></view>
            <text class="date-pill-text">{{ periodTitle }}</text>
          </view>
          <button class="date-nav-btn" :disabled="activeTab === 'all'" @click="shiftPeriod(1)">›</button>
        </view>

        <view class="hero-tabs">
          <view
            v-for="tab in tabs"
            :key="tab.key"
            :class="['hero-tab', { active: activeTab === tab.key }]"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </view>
        </view>
      </view>

      <view class="hero-summary" :class="{ warning: summaryWarning, danger: summaryDanger }">
        <view class="summary-head">
          <text class="summary-label">{{ summaryLabel }}</text>
          <text class="summary-total">预算 {{ formatMoney(summaryTotal) }}</text>
        </view>
        <view class="progress-track">
          <view
            class="progress-fill"
            :class="{ warning: summaryWarning, danger: summaryDanger }"
            :style="{ width: summaryPercent }"
          ></view>
        </view>
        <view class="summary-foot">
          <text>已用 {{ formatMoney(summaryUsed) }}</text>
          <text>{{ summaryDanger ? '超支 ' + formatMoney(-summaryRemaining) : '剩余 ' + formatMoney(summaryRemaining) }}</text>
        </view>
        <text class="period-note">预算只统计支出记录</text>
      </view>
    </view>

    <!-- Unified Budget List -->
    <view v-if="activeTab === 'all'" class="budget-list">
      <view v-for="group in groupedBudgets" :key="group.month" class="month-group">
        <view class="month-group-header">
          <text class="month-group-title">{{ group.month }}</text>
        </view>
        <view
          class="budget-item"
          :class="[itemStateClass(item), { subordinate: item.subordinate }]"
          v-for="item in group.items"
          :key="item.id"
          @click="editBudget(item)"
          @longpress="deleteBudget(item)"
        >
          <view class="budget-head">
            <view>
              <text class="budget-name">{{ item.categoryName }}</text>
              <text :class="['budget-state', itemStateClass(item)]">{{ itemStateLabel(item) }}</text>
              <text v-if="item.subordinate" class="sub-tag">已涵盖</text>
            </view>
            <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
          </view>
          <view class="progress-track">
            <view class="progress-fill" :class="itemStateClass(item)" :style="{ width: percent(item) }"></view>
          </view>
          <text :class="['remaining', itemStateClass(item)]">{{ itemRemainingText(item) }}</text>
        </view>
      </view>
    </view>

    <view v-else-if="budgets.length" class="budget-list">
      <view
        class="budget-item"
        :class="[itemStateClass(item), { subordinate: item.subordinate }]"
        v-for="item in sortedBudgets"
        :key="item.id"
        @click="editBudget(item)"
        @longpress="deleteBudget(item)"
      >
        <view class="budget-head">
          <view>
            <text class="budget-name">{{ item.categoryName }}</text>
            <text :class="['budget-state', itemStateClass(item)]">{{ itemStateLabel(item) }}</text>
            <text v-if="item.subordinate" class="sub-tag">已涵盖</text>
          </view>
          <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :class="itemStateClass(item)" :style="{ width: percent(item) }"></view>
        </view>
        <text :class="['remaining', itemStateClass(item)]">{{ itemRemainingText(item) }}</text>
      </view>
    </view>

    <view v-if="!budgets.length && !loading" class="empty-card">
      <text>还没有预算，点击下方按钮新建。</text>
    </view>

    <!-- Create Button -->
    <view class="create-btn" @click="goCreate">
      <text class="create-btn-icon">+</text>
      <text class="create-btn-text">新建预算</text>
    </view>

    <custom-tab-bar />
  </view>
</template>

<script>
import CustomTabBar from '../../custom-tab-bar/index.vue'
import { useTransactionStore } from '../../store/transaction'
import { formatMoney } from '../../utils/money'
import { getThemeVars } from '../../utils/theme'

export default {
  components: { CustomTabBar },
  data() {
    return {
      loading: false,
      activeTab: 'monthly',
      tabs: [
        { key: 'monthly', label: '本月' },
        { key: 'yearly', label: '年度' },
        { key: 'all', label: '全部' }
      ],
      currentYear: new Date().getFullYear(),
      currentMonthKey: '',
      budgets: [],
      categories: [],
      themeVars: getThemeVars()
    }
  },
  computed: {
    periodType() {
      if (this.activeTab === 'yearly') return 'YEARLY'
      if (this.activeTab === 'all') return null
      return 'MONTHLY'
    },
    periodKey() {
      if (this.activeTab === 'yearly') return String(this.currentYear)
      if (this.activeTab === 'all') return null
      return this.currentMonthKey
    },
    periodTitle() {
      if (this.activeTab === 'yearly') return `${this.currentYear}年`
      if (this.activeTab === 'all') return '全部预算'
      const [y, m] = this.currentMonthKey.split('-')
      return `${y}年${parseInt(m)}月`
    },
    summaryBudgets() {
      return this.budgets.filter(i => !i.subordinate)
    },
    summaryUsed() {
      return this.summaryBudgets.reduce((s, i) => s + Number(i.usedAmount || 0), 0)
    },
    summaryTotal() {
      return this.summaryBudgets.reduce((s, i) => s + Number(i.amount || 0), 0)
    },
    summaryRemaining() {
      return this.summaryTotal - this.summaryUsed
    },
    summaryRate() {
      return this.summaryTotal > 0 ? this.summaryUsed / this.summaryTotal : 0
    },
    summaryPercent() {
      return `${Math.min(100, Math.round(this.summaryRate * 100))}%`
    },
    summaryWarning() {
      return this.summaryRate >= 0.8 && this.summaryRate < 1
    },
    summaryDanger() {
      return this.summaryRate >= 1
    },
    summaryLabel() {
      if (this.summaryTotal <= 0) return '暂无预算'
      if (this.summaryDanger) return '已超预算'
      if (this.summaryWarning) return '接近预算上限'
      return '预算使用情况'
    },
    sortedBudgets() {
      const copy = [...this.budgets]
      return copy.sort((a, b) => {
        const sa = a.remainingAmount < 0 ? 2 : (a.warning ? 1 : 0)
        const sb = b.remainingAmount < 0 ? 2 : (b.warning ? 1 : 0)
        return sb - sa
      })
    },
    groupedBudgets() {
      const groups = {}
      this.budgets.forEach(item => {
        const month = (item.budgetMonth || '').substring(0, 7)
        if (!groups[month]) groups[month] = []
        groups[month].push(item)
      })
      const severity = (i) => i.remainingAmount < 0 ? 2 : (i.warning ? 1 : 0)
      return Object.entries(groups)
        .sort((a, b) => b[0].localeCompare(a[0]))
        .map(([month, items]) => ({ month, items: items.sort((a, b) => severity(b) - severity(a)) }))
    }
  },
  onLoad(options = {}) {
    if (options.month) {
      this.currentMonthKey = options.month
      this.activeTab = 'monthly'
    } else {
      this.currentMonthKey = this.formatMonth(new Date())
    }
    if (options.tab) {
      this.activeTab = options.tab
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.loadData()
  },
  methods: {
    formatMoney,
    async loadData() {
      this.loading = true
      const store = useTransactionStore()
      const tasks = [store.fetchCategories('EXPENSE')]
      if (this.activeTab === 'all') {
        tasks.push(store.fetchBudgets({}))
      } else {
        tasks.push(store.fetchBudgets({ periodType: this.periodType, periodKey: this.periodKey }))
      }
      await Promise.all(tasks)
      this.categories = Array.isArray(store.categories) ? [...store.categories] : []
      this.budgets = Array.isArray(store.budgets) ? [...store.budgets] : []
      this.loading = false
    },
    switchTab(key) {
      this.activeTab = key
      if (key === 'yearly') {
        this.currentYear = new Date().getFullYear()
      } else if (key === 'monthly' && !this.currentMonthKey) {
        this.currentMonthKey = this.formatMonth(new Date())
      }
      this.loadData()
    },
    shiftPeriod(delta) {
      if (this.activeTab === 'yearly') {
        this.currentYear += delta
      } else if (this.activeTab === 'monthly') {
        const [y, m] = this.currentMonthKey.split('-').map(Number)
        this.currentMonthKey = this.formatMonth(new Date(y, m - 1 + delta, 1))
      }
      if (this.activeTab !== 'all') {
        this.loadData()
      }
    },
    formatMonth(date) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    },
    percent(item) {
      return `${Math.min(100, Math.round(Number(item.usageRate || 0) * 100))}%`
    },
    itemStateClass(item) {
      if (item.remainingAmount < 0) return 'danger'
      if (item.warning) return 'warning'
      return ''
    },
    itemStateLabel(item) {
      if (item.remainingAmount < 0) return '超出预算'
      if (item.warning) return '接近预警线'
      return '使用正常'
    },
    itemRemainingText(item) {
      if (item.remainingAmount < 0) return `超支 ${this.formatMoney(-item.remainingAmount)}`
      return `剩余 ${this.formatMoney(item.remainingAmount)}`
    },
    goCreate() {
      const params = []
      if (this.activeTab === 'monthly') {
        params.push(`periodType=MONTHLY`)
        params.push(`periodKey=${this.currentMonthKey}`)
      } else if (this.activeTab === 'yearly') {
        params.push(`periodType=YEARLY`)
        params.push(`periodKey=${this.currentYear}`)
      }
      uni.navigateTo({ url: `/pages/transaction/budget-form${params.length ? '?' + params.join('&') : ''}` })
    },
    editBudget(item) {
      uni.navigateTo({ url: `/pages/transaction/budget-form?id=${item.id}&periodType=${item.periodType || 'MONTHLY'}&periodKey=${item.budgetMonth}` })
    },
    deleteBudget(item) {
      uni.showModal({
        title: '删除预算',
        content: `确认删除"${item.categoryName}"预算？`,
        success: async (result) => {
          if (!result.confirm) return
          const store = useTransactionStore()
          const res = await store.deleteBudget(item.id, { periodType: item.periodType, periodKey: item.budgetMonth })
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            this.loadData()
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
  padding: 24rpx 24rpx calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

/* ========== Hero Card (period nav + tabs + summary) ========== */
.hero-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
  margin-bottom: 22rpx;
  overflow: hidden;
}

/* Nav row: date + tabs on one line */
.hero-nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 18rpx 14rpx;
  gap: 10rpx;
}

/* Date navigation group: ‹ pill › */
.date-nav-group {
  display: flex;
  align-items: center;
  gap: 4rpx;
  flex-shrink: 0;
}

.date-nav-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: transparent;
  color: var(--app-primary, #d3a414);
  border: 1rpx solid var(--app-border, #edf1f4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  line-height: 1;
  padding: 0;
  margin: 0;
  transition: opacity 0.2s;
}

.date-nav-btn[disabled] {
  opacity: 0.22;
  pointer-events: none;
}

/* Compact date pill */
.date-pill {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #eff1f5);
  flex-shrink: 0;
}

.date-pill-icon {
  width: 24rpx;
  height: 22rpx;
  border: 2rpx solid var(--app-primary, #d3a414);
  border-radius: 3rpx;
  position: relative;
  flex-shrink: 0;
  margin-top: -2rpx;
}

.date-pill-icon::before {
  content: '';
  position: absolute;
  top: -3rpx;
  left: -2rpx;
  right: -2rpx;
  height: 5rpx;
  background: var(--app-primary, #d3a414);
  border-radius: 3rpx 3rpx 0 0;
}

.date-pill-text {
  color: var(--app-text, #17202a);
  font-size: 26rpx;
  font-weight: 800;
  white-space: nowrap;
}

/* Tab pills — compact, right side */
.hero-tabs {
  display: flex;
  gap: 4rpx;
  flex-shrink: 0;
}

.hero-tab {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 650;
  background: transparent;
  color: var(--app-muted, #7b8798);
  transition: all 0.18s;
  white-space: nowrap;
}

.hero-tab.active {
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  font-weight: 700;
}

/* Summary section */
.hero-summary {
  padding: 18rpx 26rpx 26rpx;
  border-top: 1rpx solid var(--app-border, #edf1f4);
  transition: background 0.25s, border-color 0.25s;
}

.hero-summary.warning {
  background: var(--app-warning-bg, #fffbeb);
  border-top-color: var(--app-warning-border, #f4dfaa);
}

.hero-summary.danger {
  background: var(--app-danger-bg, #fef2f2);
  border-top-color: var(--app-danger-border, #fecaca);
}

.summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}

.summary-label {
  font-size: 26rpx;
  color: var(--app-muted, #7b8798);
  font-weight: 650;
}

.hero-summary.warning .summary-label { color: var(--app-warning-text, #8a5a13); }
.hero-summary.danger .summary-label { color: var(--app-danger, #d94a62); }

.summary-total {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  font-weight: 800;
}

.summary-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--app-muted, #7b8798);
}

.hero-summary.danger .summary-foot text:last-child {
  color: var(--app-danger, #d94a62);
  font-weight: 800;
}

.period-note {
  display: block;
  margin-top: 10rpx;
  color: var(--app-faint, #94a3b8);
  font-size: 22rpx;
  text-align: center;
}

/* ========== Progress ========== */
.progress-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #edf2f5);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  transition: width 0.3s;
}

.progress-fill.warning {
  background: var(--app-warning, #f59e0b);
}

.progress-fill.danger {
  background: var(--app-danger, #d94a62);
}

/* ========== Unified Budget List ========== */
.budget-list {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
  padding: 4rpx 28rpx;
  margin-bottom: 22rpx;
  overflow: hidden;
}

.budget-item {
  position: relative;
  padding: 26rpx 0 26rpx 18rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.budget-item:last-child {
  border-bottom: none;
}

/* Inline state accent strip */
.budget-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20rpx;
  bottom: 20rpx;
  width: 4rpx;
  border-radius: 999rpx;
  background: transparent;
  transition: background 0.25s;
}

.budget-item.danger::before {
  background: var(--app-danger, #d94a62);
}

.budget-item.warning::before {
  background: var(--app-warning, #f59e0b);
}

.budget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.budget-name {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 800;
}

.budget-state {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
}

.budget-state.warning {
  color: var(--app-warning-text, #8a5a13);
}

.budget-state.danger {
  color: var(--app-danger, #d94a62);
}

.budget-money {
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 850;
}

.remaining {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  margin-top: 8rpx;
  display: block;
}

.remaining.warning {
  color: var(--app-warning-text, #8a5a13);
  font-weight: 650;
}

.remaining.danger {
  color: var(--app-danger, #d94a62);
  font-weight: 700;
}

/* Subordinate budget */
.sub-tag {
  display: inline-block;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  background: var(--app-soft-bg, #eff1f5);
  color: var(--app-muted, #9ca3af);
  margin-left: 8rpx;
  vertical-align: middle;
}

.budget-item.subordinate {
  opacity: 0.65;
}

/* Month groups in all tab */
.month-group-header {
  padding: 20rpx 0 6rpx;
}

.month-group-title {
  color: var(--app-primary, #d3a414);
  font-size: 28rpx;
  font-weight: 800;
}

/* ========== Create Button ========== */
.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 80rpx;
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  font-size: 30rpx;
  font-weight: 700;
  border: 2rpx solid var(--app-primary, #d3a414);
}

.create-btn-icon {
  font-size: 36rpx;
  font-weight: 300;
}

.create-btn-text {
  font-size: 30rpx;
}

.empty-card {
  padding: 80rpx 0;
  text-align: center;
  color: var(--app-muted, #7b8798);
  font-size: 26rpx;
}
</style>
