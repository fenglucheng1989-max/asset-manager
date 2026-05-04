<template>
  <view class="container" :style="themeVars">
    <view class="hero-card">
      <view class="hero-date-row">
        <button class="date-nav-btn" @click="changePeriod(-1)">‹</button>
        <picker :range="periodOptions" range-key="label" :value="periodIndex" @change="selectPeriodMode">
          <view class="date-title">
            <text class="date-label">{{ periodLabel }}</text>
            <text class="period-mode">{{ periodTitle }} ▾</text>
          </view>
        </picker>
        <button class="date-nav-btn" @click="changePeriod(1)">›</button>
      </view>
      <view class="hero-overview">
        <text class="hero-label">{{ periodTitle }}收支净额</text>
        <text :class="['hero-value', { negative: Number(report.net || 0) < 0 }]">{{ formatSignedMoney(report.net) }}</text>
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
    </view>

    <view class="filter-strip">
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
            <text class="pie-value">{{ formatCompactMoney(activeStructureTotal) }}</text>
          </view>
        </view>
        <view class="pie-legend">
          <view class="legend-item" v-for="(item, index) in activeStructureStats.slice(0, 5)" :key="`${structureType}-${item.categoryName}`">
            <view class="legend-left">
              <text class="legend-dot" :style="{ backgroundColor: getStructureColor(index) }"></text>
              <text class="legend-name">{{ item.categoryName }}</text>
            </view>
            <text class="legend-percent">{{ formatPercent(item.percent) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="panel-card">
      <view class="section-header">
        <view class="section-title-group">
          <text class="section-title">收支明细</text>
          <text class="section-subtitle">{{ records.length }} 条</text>
        </view>
        <button class="section-add-btn" @click="goCreate">
          <text>+</text>
        </button>
      </view>

      <view v-if="records.length > 0" class="record-list">
        <view
          class="record-row"
          :class="{ swiped: swipedRecordId === item.id }"
          v-for="item in records"
          :key="item.id"
        >
          <view class="record-delete-underlay" @click.stop="confirmDelete(item)">
            <text>删除</text>
          </view>
          <view
            class="record-item"
            :style="{ transform: swipedRecordId === item.id ? 'translateX(-140rpx)' : 'translateX(0)' }"
            @click="handleRecordClick(item)"
            @touchstart="handleRecordTouchStart(item, $event)"
            @touchmove="handleRecordTouchMove(item, $event)"
            @touchend="handleRecordTouchEnd(item)"
            @longpress="confirmDelete(item)"
          >
            <view class="record-icon" :class="recordIconClass(item)">
              <view class="record-symbol"></view>
            </view>
            <view class="record-copy">
              <text class="record-title">{{ getRecordTitle(item) }}</text>
              <text class="record-sub">{{ getRecordSub(item) }}</text>
            </view>
            <text :class="['record-amount', amountClass(item.transactionType)]">{{ getRecordAmount(item) }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-card">
        <text>当前筛选下暂无记录</text>
      </view>
    </view>

    <custom-tab-bar />
  </view>
</template>

<script>
import CustomTabBar from '../../custom-tab-bar/index.vue'
import { useTransactionStore } from '../../store/transaction'
import { formatCompactMoney, formatMoney } from '../../utils/money'
import { getThemeMode, getThemeVars } from '../../utils/theme'

export default {
  components: { CustomTabBar },
  data() {
    return {
      records: [],
      categories: [],
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
      isLoading: false,
      swipeTouchStartX: 0,
      swipeTouchStartY: 0,
      swipeTouchRecordId: null,
      swipedRecordId: null,
      themeVars: getThemeVars()
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
      return this.structureType === 'INCOME'
        ? 'var(--app-positive-color, #1f8f72)'
        : 'var(--app-liability-color, #d94a62)'
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
  },
  onLoad() {
    const today = new Date()
    this.anchorDate = this.formatDate(today)
    this.currentMonth = this.formatMonth(today)
  },
  onShow() {
    this.themeVars = getThemeVars(getThemeMode())
    this.refreshData()
  },
  methods: {
    formatCompactMoney,
    formatMoney,
    async refreshData() {
      if (this.isLoading) return
      this.isLoading = true
      try {
        const store = useTransactionStore()
        const [startDate, endDate] = this.periodRange()
        await Promise.all([
          store.fetchCategories(),
          store.fetchRecords({
            limit: 200,
            type: this.filterType,
            categoryId: this.isCategoryDisabled ? null : this.filterCategoryId,
            startDate,
            endDate
          })
        ])
        this.categories = Array.isArray(store.categories) ? [...store.categories] : []
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
          categoryName: item.categoryName || this.getFallbackCategoryName(type),
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
      if (!items.length) return 'var(--app-soft-bg, #edf2f5)'
      let start = 0
      const segments = items.map((item, index) => {
        const percent = Math.max(0, Number(item.percent || 0) * 100)
        const end = index === items.length - 1 ? 100 : Math.min(100, start + percent)
        const color = this.getStructureColor(index)
        const segment = `${color} ${start}% ${end}%`
        start = end
        return segment
      })
      return `conic-gradient(${segments.join(', ')})`
    },
    getStructureColor(index) {
      const expensePalette = [
        'var(--app-liability-color, #d94a62)',
        'var(--app-accent, #f4c95d)',
        'var(--app-primary, #d3a414)',
        'var(--app-muted, #64748b)',
        'var(--app-faint, #94a3b8)'
      ]
      const incomePalette = [
        'var(--app-positive-color, #1f8f72)',
        'var(--app-primary, #d3a414)',
        'var(--app-accent, #f4c95d)',
        'var(--app-muted, #64748b)',
        'var(--app-faint, #94a3b8)'
      ]
      const palette = this.structureType === 'INCOME' ? incomePalette : expensePalette
      return palette[index % palette.length]
    },
    getTypeName(type) {
      const map = { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账' }
      return map[type] || type
    },
    getFallbackCategoryName(type) {
      if (type === 'INCOME') return '其他收入'
      if (type === 'EXPENSE') return '其他支出'
      return '转账'
    },
    getRecordTitle(item) {
      return item.categoryName || this.getFallbackCategoryName(item.transactionType)
    },
    amountClass(type) {
      return {
        income: type === 'INCOME',
        expense: type === 'EXPENSE',
        transfer: type === 'TRANSFER'
      }
    },
    recordIconClass(item) {
      const kind = this.getRecordIconKind(item)
      return [
        item.transactionType === 'INCOME' ? 'income' : '',
        item.transactionType === 'EXPENSE' ? 'expense' : '',
        item.transactionType === 'TRANSFER' ? 'transfer' : '',
        `record-icon-${kind}`
      ]
    },
    getRecordIconKind(item) {
      if (item.transactionType === 'TRANSFER') return 'transfer'
      const name = this.getRecordTitle(item)
      if (/餐|饭|食|饮|咖|奶|茶|果/.test(name)) return 'food'
      if (/购|买|衣|日用|超市|数码|电商/.test(name)) return 'shopping'
      if (/交|车|油|地铁|公交|打车|出行|机票|火车/.test(name)) return 'travel'
      if (/房|租|水|电|物业|居家/.test(name)) return 'home'
      if (/医|药|诊|健康/.test(name)) return 'medical'
      if (/工资|薪|奖金|分红|理财|利息|收入/.test(name)) return 'salary'
      if (/娱乐|电影|游戏|会员/.test(name)) return 'fun'
      return item.transactionType === 'INCOME' ? 'other-income' : 'other-expense'
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
      return `${item.accountName || '-'} · ${date}`
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
    handleRecordClick(item) {
      if (this.swipedRecordId) {
        this.swipedRecordId = null
        return
      }
      this.goEdit(item)
    },
    handleRecordTouchStart(item, event) {
      const touch = event.touches && event.touches[0]
      if (!touch) return
      this.swipeTouchStartX = touch.clientX
      this.swipeTouchStartY = touch.clientY
      this.swipeTouchRecordId = item.id
    },
    handleRecordTouchMove(item, event) {
      const touch = event.touches && event.touches[0]
      if (!touch || this.swipeTouchRecordId !== item.id) return
      const deltaX = touch.clientX - this.swipeTouchStartX
      const deltaY = touch.clientY - this.swipeTouchStartY
      if (Math.abs(deltaX) > 42 && Math.abs(deltaX) > Math.abs(deltaY) * 1.4) {
        this.swipedRecordId = deltaX < 0 ? item.id : null
      }
    },
    handleRecordTouchEnd() {
      this.swipeTouchRecordId = null
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
  background: var(--app-page-bg, #f8f9fb);
}

/* ---- Hero Card ---- */
.hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 26rpx;
  background: var(--app-outfit-header-bg, var(--app-hero-gradient, linear-gradient(135deg, #17202a 0%, #164d45 100%)));
  color: var(--app-outfit-header-text, var(--app-hero-text, #ffffff));
  margin-bottom: 14rpx;
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.10);
}

.hero-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--app-outfit-header-pattern, none);
  opacity: 0.24;
  pointer-events: none;
}

.hero-date-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 24rpx 0;
}

.hero-date-row .date-nav-btn {
  width: 52rpx;
  height: 52rpx;
  line-height: 48rpx;
  margin: 0;
  padding: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--app-outfit-header-text, #ffffff);
  border: 1rpx solid var(--app-section-bg, rgba(255, 255, 255, 0.22));
  font-size: 30rpx;
  font-weight: 500;
}

.hero-date-row .date-title {
  text-align: center;
}

.hero-date-row .date-label {
  display: inline;
  color: var(--app-outfit-header-text, #ffffff);
  font-size: 30rpx;
  line-height: 38rpx;
  font-weight: 850;
  margin-right: 12rpx;
}

.hero-date-row .period-mode {
  color: var(--app-outfit-header-sub, rgba(255, 255, 255, 0.68));
  font-size: 24rpx;
  line-height: 32rpx;
  font-weight: 750;
}

.hero-overview {
  position: relative;
  z-index: 1;
  padding: 24rpx 32rpx 28rpx;
  text-align: center;
}

.hero-label {
  display: block;
  color: var(--app-outfit-header-sub, rgba(255, 255, 255, 0.76));
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-value {
  display: block;
  margin-top: 8rpx;
  color: var(--app-outfit-header-accent, var(--app-hero-accent, #ffd166));
  font-size: 44rpx;
  line-height: 54rpx;
  font-weight: 850;
  word-break: normal;
  overflow-wrap: break-word;
}

.hero-value.negative {
  color: var(--app-liability-color, #ff8fa3);
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 56rpx;
  margin-top: 14rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid var(--app-section-bg, rgba(255, 255, 255, 0.14));
}

.stat-label,
.section-subtitle,
.record-sub {
  display: block;
  color: var(--app-muted, #7f8ea3);
  font-size: 24rpx;
  line-height: 34rpx;
}

.stat-label {
  color: var(--app-outfit-header-sub, rgba(255, 255, 255, 0.68));
}

.stat-value {
  display: block;
  margin-top: 6rpx;
  font-size: 31rpx;
  font-weight: 850;
  text-align: center;
}

.stat-value.income {
  color: var(--app-positive-color, #1f8f72);
}

.stat-value.expense {
  color: var(--app-liability-color, #d94a62);
}

.filter-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 10rpx 4rpx 20rpx;
}

.type-scroll {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}

.type-chip-row {
  display: flex;
  gap: 10rpx;
  width: max-content;
  padding: 0 2rpx;
}

.type-chip {
  height: 58rpx;
  line-height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-muted, #64748b);
  font-size: 25rpx;
  font-weight: 800;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.type-chip.active {
  color: var(--app-primary, #d3a414);
  background: var(--app-tabbar-selected-bg, rgba(211, 164, 20, 0.12));
  border-color: var(--app-primary, #d3a414);
}

.category-pill {
  height: 58rpx;
  max-width: 204rpx;
  padding: 0 10rpx 0 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-muted, #64748b);
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 25rpx;
  font-weight: 800;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.category-pill.disabled {
  color: var(--app-faint, #94a3b8);
  background: var(--app-soft-bg, #f3f6f8);
}

.category-pill text:first-child {
  min-width: 0;
  max-width: 148rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-arrow {
  color: var(--app-faint, #94a3b8);
  font-size: 28rpx;
  line-height: 28rpx;
}

.category-pill.disabled .category-arrow {
  color: var(--app-faint, #94a3b8);
}

/* ---- Panels ---- */
.panel-card {
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
  padding: 24rpx;
  margin-bottom: 18rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.section-title-group {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  min-width: 0;
}

.section-title {
  color: var(--app-text, #17202a);
  font-size: 31rpx;
  font-weight: 850;
}

.section-add-btn {
  width: 56rpx;
  height: 56rpx;
  line-height: 52rpx;
  margin: 0;
  padding: 0;
  border-radius: 50%;
  background: var(--app-tabbar-selected-bg, rgba(211, 164, 20, 0.12));
  color: var(--app-primary, #d3a414);
  border: 1rpx solid var(--app-border, #edf1f4);
  font-size: 34rpx;
  font-weight: 500;
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
  width: 134rpx;
  height: 134rpx;
  border-radius: 50%;
  background: var(--app-card-bg, #ffffff);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1rpx var(--app-border, #edf1f4);
}

.pie-label {
  color: var(--app-muted, #7b8798);
  font-size: 22rpx;
  line-height: 28rpx;
}

.pie-value {
  color: var(--app-text, #17202a);
  font-size: 22rpx;
  line-height: 28rpx;
  font-weight: 850;
}

.pie-legend {
  flex: 1;
  min-width: 0;
}

.legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 6rpx 0;
}

.legend-left {
  display: flex;
  align-items: center;
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
  color: var(--app-muted, #334155);
  font-size: 24rpx;
  line-height: 32rpx;
  font-weight: 750;
  max-width: 170rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-percent {
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  line-height: 32rpx;
  flex-shrink: 0;
}

.structure-switch {
  display: flex;
  gap: 6rpx;
  padding: 4rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #f3f6f8);
}

.structure-switch-item {
  min-width: 70rpx;
  height: 42rpx;
  line-height: 42rpx;
  border-radius: 999rpx;
  text-align: center;
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  font-weight: 800;
}

.structure-switch-item.active {
  background: var(--app-primary, #e8c56d);
  color: #ffffff;
}

.record-row {
  position: relative;
  overflow: hidden;
}

.record-row.swiped {
  z-index: 1;
}

.record-delete-underlay {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 140rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-danger, #d94a62);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 800;
}

.record-item {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  transition: transform 0.18s ease;
}

.record-icon {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--app-muted, #64748b);
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
}

  .record-icon.income {
    color: var(--app-positive-color, #1f8f72);
    background: color-mix(in srgb, var(--app-positive-color, #1f8f72) 15%, var(--app-card-bg, #ffffff));
    border-color: color-mix(in srgb, var(--app-positive-color, #1f8f72) 30%, transparent);
  }

  .record-icon.expense {
    color: var(--app-liability-color, #d94a62);
    background: color-mix(in srgb, var(--app-liability-color, #d94a62) 15%, var(--app-card-bg, #ffffff));
    border-color: color-mix(in srgb, var(--app-liability-color, #d94a62) 30%, transparent);
  }
.record-icon.transfer {
  color: var(--app-muted, #64748b);
  background: var(--app-soft-bg, #f3f6f8);
}

.record-symbol {
  position: relative;
  width: 30rpx;
  height: 30rpx;
}

.record-symbol::before,
.record-symbol::after {
  content: '';
  position: absolute;
  box-sizing: border-box;
}

.record-icon-income .record-symbol::before,
.record-icon-expense .record-symbol::before {
  left: 50%;
  top: 5rpx;
  width: 2rpx;
  height: 20rpx;
  border-radius: 999rpx;
  background: currentColor;
  transform: translateX(-50%);
}

.record-icon-expense .record-symbol::after {
  left: 8rpx;
  bottom: 5rpx;
  width: 14rpx;
  height: 14rpx;
  border-right: 2rpx solid currentColor;
  border-bottom: 2rpx solid currentColor;
  transform: rotate(45deg);
}

.record-icon-income .record-symbol::after {
  left: 8rpx;
  top: 5rpx;
  width: 14rpx;
  height: 14rpx;
  border-left: 2rpx solid currentColor;
  border-top: 2rpx solid currentColor;
  transform: rotate(45deg);
}

.record-icon-transfer .record-symbol::before {
  left: 2rpx;
  top: 8rpx;
  width: 24rpx;
  height: 2rpx;
  border-radius: 999rpx;
  background: currentColor;
  box-shadow: 4rpx 12rpx 0 currentColor;
}

.record-icon-transfer .record-symbol::after {
  right: 1rpx;
  top: 4rpx;
  width: 10rpx;
  height: 10rpx;
  border-right: 2rpx solid currentColor;
  border-top: 2rpx solid currentColor;
  transform: rotate(45deg);
  box-shadow: -16rpx 16rpx 0 -2rpx var(--app-soft-bg, #f3f6f8), -16rpx 16rpx 0 0 currentColor;
}

.record-icon-food .record-symbol::before {
  left: 5rpx;
  top: 5rpx;
  width: 20rpx;
  height: 20rpx;
  border: 2rpx solid currentColor;
  border-radius: 50%;
}

.record-icon-food .record-symbol::after {
  left: 10rpx;
  top: 10rpx;
  width: 10rpx;
  height: 10rpx;
  border: 2rpx solid currentColor;
  border-radius: 50%;
}

.record-icon-shopping .record-symbol::before {
  left: 4rpx;
  top: 10rpx;
  width: 22rpx;
  height: 17rpx;
  border: 2rpx solid currentColor;
  border-radius: 5rpx;
}

.record-icon-shopping .record-symbol::after {
  left: 9rpx;
  top: 4rpx;
  width: 12rpx;
  height: 10rpx;
  border: 2rpx solid currentColor;
  border-bottom: 0;
  border-radius: 10rpx 10rpx 0 0;
}

.record-icon-travel .record-symbol::before {
  left: 4rpx;
  top: 10rpx;
  width: 22rpx;
  height: 12rpx;
  border: 2rpx solid currentColor;
  border-radius: 8rpx 8rpx 4rpx 4rpx;
}

.record-icon-travel .record-symbol::after {
  left: 8rpx;
  bottom: 4rpx;
  width: 5rpx;
  height: 5rpx;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 10rpx 0 0 currentColor;
}

.record-icon-home .record-symbol::before {
  left: 6rpx;
  top: 14rpx;
  width: 18rpx;
  height: 13rpx;
  border: 2rpx solid currentColor;
  border-top: 0;
  border-radius: 0 0 5rpx 5rpx;
}

.record-icon-home .record-symbol::after {
  left: 7rpx;
  top: 6rpx;
  width: 16rpx;
  height: 16rpx;
  border-left: 2rpx solid currentColor;
  border-top: 2rpx solid currentColor;
  transform: rotate(45deg);
}

.record-icon-medical .record-symbol::before,
.record-icon-medical .record-symbol::after {
  left: 50%;
  top: 50%;
  border-radius: 999rpx;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.record-icon-medical .record-symbol::before {
  width: 22rpx;
  height: 4rpx;
}

.record-icon-medical .record-symbol::after {
  width: 4rpx;
  height: 22rpx;
}

.record-icon-salary .record-symbol::before {
  left: 5rpx;
  top: 7rpx;
  width: 20rpx;
  height: 20rpx;
  border: 2rpx solid currentColor;
  border-radius: 50%;
}

.record-icon-salary .record-symbol::after {
  left: 14rpx;
  top: 4rpx;
  width: 2rpx;
  height: 25rpx;
  border-radius: 999rpx;
  background: currentColor;
}

.record-icon-fun .record-symbol::before {
  left: 4rpx;
  top: 10rpx;
  width: 22rpx;
  height: 16rpx;
  border: 2rpx solid currentColor;
  border-radius: 10rpx;
}

.record-icon-fun .record-symbol::after {
  left: 9rpx;
  top: 15rpx;
  width: 4rpx;
  height: 4rpx;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 9rpx 0 0 currentColor;
}

.record-icon-other-expense .record-symbol::before {
  left: 6rpx;
  top: 4rpx;
  width: 19rpx;
  height: 23rpx;
  border: 2rpx solid currentColor;
  border-radius: 5rpx;
}

.record-icon-other-expense .record-symbol::after {
  left: 11rpx;
  top: 10rpx;
  width: 9rpx;
  height: 2rpx;
  border-radius: 999rpx;
  background: currentColor;
  box-shadow: 0 6rpx 0 currentColor, 0 12rpx 0 currentColor;
}

.record-icon-other-income .record-symbol::before {
  left: 5rpx;
  top: 7rpx;
  width: 20rpx;
  height: 16rpx;
  border: 2rpx solid currentColor;
  border-radius: 9rpx;
}

.record-icon-other-income .record-symbol::after {
  left: 14rpx;
  top: 3rpx;
  width: 2rpx;
  height: 23rpx;
  border-radius: 999rpx;
  background: currentColor;
  box-shadow: -5rpx 5rpx 0 -1rpx currentColor, 5rpx 5rpx 0 -1rpx currentColor;
}

.record-item:last-child {
  border-bottom: none;
}

.record-copy {
  flex: 1;
  min-width: 0;
}

.record-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 800;
}

.record-amount {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 850;
  flex-shrink: 0;
}

.record-amount.income {
  color: var(--app-positive-color, #1f8f72);
}

.record-amount.expense {
  color: var(--app-liability-color, #d94a62);
}

.record-amount.transfer {
  color: var(--app-muted, #64748b);
}

.empty-card {
  padding: 50rpx 0;
  text-align: center;
  color: var(--app-muted, #7b8798);
  font-size: 26rpx;
}
</style>
