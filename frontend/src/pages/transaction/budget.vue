<template>
  <view class="container">
    <!-- Period Switcher -->
    <view class="period-switcher">
      <text class="period-arrow" @click="shiftPeriod(-1)">‹</text>
      <view class="period-center">
        <text class="period-label">{{ periodTitle }}</text>
        <text class="period-sub">预算只统计支出记录</text>
      </view>
      <text class="period-arrow" @click="shiftPeriod(1)">›</text>
    </view>

    <!-- Tab Tabs -->
    <view class="tabs-row">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- Summary Card -->
    <view class="summary-card" :class="{ warning: summaryWarning, danger: summaryDanger }">
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
    </view>

    <!-- Warning Section -->
    <view v-if="warningItems.length" class="section-label warning-label">
      ⚠️ 超支预警 ({{ warningItems.length }})
    </view>
    <view v-if="warningItems.length" class="budget-list warning-list">
      <view
        class="budget-item danger"
        :class="{ subordinate: item.subordinate }"
        v-for="item in warningItems"
        :key="item.id"
        @click="editBudget(item)"
        @longpress="deleteBudget(item)"
      >
        <view class="budget-head">
          <view>
            <text class="budget-name">{{ item.categoryName }}</text>
            <text class="budget-state danger">超出预算</text>
            <text v-if="item.subordinate" class="sub-tag">已涵盖</text>
          </view>
          <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill danger" :style="{ width: percent(item) }"></view>
        </view>
        <text class="remaining danger">超支 {{ formatMoney(-item.remainingAmount) }}</text>
      </view>
    </view>

    <!-- Normal Section -->
    <view v-if="normalItems.length" class="section-label">
      正常预算 ({{ normalItems.length }})
      <text v-if="activeTab === 'all'" class="section-sort">按月份倒序</text>
    </view>
    <view v-if="activeTab === 'all'" class="all-list">
      <view v-for="group in groupedBudgets" :key="group.month" class="month-group">
        <view class="month-group-header">
          <text class="month-group-title">{{ group.month }}</text>
        </view>
        <view
          class="budget-item"
          :class="{ subordinate: item.subordinate }"
          v-for="item in group.items"
          :key="item.id"
          @click="editBudget(item)"
          @longpress="deleteBudget(item)"
        >
          <view class="budget-head">
            <view>
              <text class="budget-name">{{ item.categoryName }}</text>
              <text class="budget-state">{{ item.warning ? '接近预警线' : '使用正常' }}</text>
              <text v-if="item.subordinate" class="sub-tag">已涵盖</text>
            </view>
            <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
          </view>
          <view class="progress-track">
            <view class="progress-fill" :style="{ width: percent(item) }"></view>
          </view>
          <text class="remaining">剩余 {{ formatMoney(item.remainingAmount) }}</text>
        </view>
      </view>
    </view>
    <view v-else>
      <view
        class="budget-item"
        :class="{ subordinate: item.subordinate }"
        v-for="item in normalItems"
        :key="item.id"
        @click="editBudget(item)"
        @longpress="deleteBudget(item)"
      >
        <view class="budget-head">
          <view>
            <text class="budget-name">{{ item.categoryName }}</text>
            <text class="budget-state" :class="{ warning: item.warning }">{{ item.warning ? '接近预警线' : '使用正常' }}</text>
            <text v-if="item.subordinate" class="sub-tag">已涵盖</text>
          </view>
          <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :class="{ warning: item.warning }" :style="{ width: percent(item) }"></view>
        </view>
        <text class="remaining">剩余 {{ formatMoney(item.remainingAmount) }}</text>
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
      categories: []
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
    warningItems() {
      return this.budgets.filter(i => i.warning || i.remainingAmount < 0)
    },
    normalItems() {
      return this.budgets.filter(i => !i.warning && i.remainingAmount >= 0)
    },
    groupedBudgets() {
      const groups = {}
      this.budgets.forEach(item => {
        const month = (item.budgetMonth || '').substring(0, 7)
        if (!groups[month]) groups[month] = []
        groups[month].push(item)
      })
      return Object.entries(groups)
        .sort((a, b) => b[0].localeCompare(a[0]))
        .map(([month, items]) => ({ month, items }))
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
      // 'all' tab doesn't shift
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
}

/* Period Switcher */
.period-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
  padding: 0 28rpx;
  height: 120rpx;
  margin-bottom: 16rpx;
}

.period-arrow {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-primary-dark, #226f63);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
}

.period-center {
  text-align: center;
}

.period-label {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 34rpx;
  font-weight: 850;
}

.period-sub {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  margin-top: 4rpx;
}

/* Tabs */
.tabs-row {
  display: flex;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.tab {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-muted, #7b8798);
  font-size: 28rpx;
  font-weight: 650;
  transition: all 0.2s;
}

.tab.active {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-weight: 800;
}

/* Summary Card */
.summary-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 22rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.summary-card.warning {
  border-color: var(--app-warning, #f59e0b);
  background: var(--app-warning-bg, #fffbeb);
}

.summary-card.danger {
  border-color: var(--app-danger, #d94a62);
  background: var(--app-danger-bg, #fef2f2);
}

.summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
}

.summary-label {
  font-size: 26rpx;
  color: var(--app-muted, #7b8798);
  font-weight: 650;
}

.summary-card.warning .summary-label { color: var(--app-warning, #d97706); }
.summary-card.danger .summary-label { color: var(--app-danger, #d94a62); }

.summary-total {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  font-weight: 800;
}

.summary-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 14rpx;
  font-size: 26rpx;
  color: var(--app-muted, #7b8798);
}

.summary-card.danger .summary-foot text:last-child {
  color: var(--app-danger, #d94a62);
  font-weight: 800;
}

/* Section Labels */
.section-label {
  font-size: 26rpx;
  color: var(--app-muted, #7b8798);
  font-weight: 650;
  margin-bottom: 10rpx;
  padding: 0 4rpx;
}

.warning-label {
  color: var(--app-danger, #d94a62);
}

.section-sort {
  float: right;
  font-weight: 400;
  font-size: 24rpx;
}

/* Budget List */
.budget-list {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
  padding: 4rpx 28rpx;
  margin-bottom: 22rpx;
}

.budget-list.warning-list {
  border-color: var(--app-danger-border, #fecaca);
  background: var(--app-danger-bg, #fef2f2);
}

.budget-item {
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.budget-item:last-child {
  border-bottom: none;
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
  color: var(--app-warning, #d97706);
}

.budget-state.danger {
  color: var(--app-danger, #d94a62);
}

.budget-money {
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 850;
}

.progress-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #edf2f5);
  overflow: hidden;
  margin: 18rpx 0 10rpx;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: var(--app-positive-color, #2EBD85);
  transition: width 0.3s;
}

.progress-fill.warning {
  background: var(--app-warning, #f59e0b);
}

.progress-fill.danger {
  background: var(--app-danger, #d94a62);
}

.remaining {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
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
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-muted, #9ca3af);
  margin-left: 8rpx;
  vertical-align: middle;
}

.budget-item.subordinate {
  opacity: 0.65;
}

/* All list month groups */
.all-list {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
  padding: 4rpx 28rpx;
  margin-bottom: 22rpx;
}

.month-group-header {
  padding: 20rpx 0 6rpx;
}

.month-group-title {
  color: var(--app-primary-dark, #8f6b00);
  font-size: 28rpx;
  font-weight: 800;
}

/* Create Button */
.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 88rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
  box-shadow: 0 8rpx 24rpx rgba(211, 164, 20, 0.28);
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
