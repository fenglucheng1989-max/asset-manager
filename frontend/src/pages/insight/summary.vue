<template>
  <view class="container" :style="themeVars">
    <view class="page-hero" v-if="current">
      <text class="hero-period">{{ periodText(current) }}</text>
      <text class="hero-desc">{{ current.description }}</text>
      <view class="hero-grid">
        <view>
          <text class="grid-label">期末净资产</text>
          <text class="grid-value">{{ formatMoney(current.endNetWorth) }}</text>
        </view>
        <view>
          <text class="grid-label">环比变化</text>
          <text :class="['grid-value', changeClass(current)]">
            {{ signedMoney(current) }}
          </text>
        </view>
      </view>
      <text class="hero-footnote">月报基于已记录流水生成，作为复盘参考</text>
    </view>

    <view class="section-card" v-if="current">
      <view class="section-head">
        <text class="section-title">收支概览</text>
        <text class="section-subtitle">本月收入、支出与预算执行</text>
      </view>
      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-label">收入</text>
          <text class="stat-value income">{{ formatMoney(current.income) }}</text>
        </view>
        <view class="stat-item">
          <text class="stat-label">支出</text>
          <text class="stat-value expense">{{ formatMoney(current.expense) }}</text>
        </view>
      </view>
      <view class="budget-block">
        <view class="budget-head">
          <text>预算使用</text>
          <text>{{ budgetText(current) }}</text>
        </view>
        <view class="budget-track">
          <view class="budget-bar" :style="{ width: budgetWidth(current) }"></view>
        </view>
      </view>
    </view>

    <view class="section-card" v-if="current">
      <view class="section-head">
        <text class="section-title">最大单笔</text>
        <text class="section-subtitle">帮助定位本月最显著的现金流</text>
      </view>
      <view class="record-line">
        <text>最大收入</text>
        <text>{{ recordText(current.largestIncome, '暂无收入') }}</text>
      </view>
      <view class="record-line">
        <text>最大支出</text>
        <text>{{ recordText(current.largestExpense, '暂无支出') }}</text>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head inline">
        <text class="section-title">历史月报</text>
        <text class="link" @click="goMilestones">里程碑 ›</text>
      </view>
      <view
        v-for="item in summaries"
        :key="item.id"
        :class="['summary-row', { selected: current && current.id === item.id }]"
        @click="selectSummary(item)"
      >
        <view class="summary-left">
          <view :class="['summary-dot', changeClass(item)]"></view>
          <view>
            <text class="row-title">{{ periodText(item) }}</text>
            <text class="row-subtitle">{{ item.description }}</text>
          </view>
        </view>
        <text :class="['row-amount', changeClass(item)]">
          {{ signedMoney(item) }}
        </text>
      </view>
      <view class="empty" v-if="loading">正在生成月报...</view>
      <view class="empty" v-if="!summaries.length && !loading">暂无月报</view>
    </view>
  </view>
</template>

<script>
import { useInsightStore } from '../../store/insight'
import { formatMoney } from '../../utils/money'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      loading: false,
      current: null,
      summaries: [],
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.fetchSummaries()
  },
  methods: {
    formatMoney,
    async fetchSummaries() {
      this.loading = true
      try {
        const store = useInsightStore()
        await store.fetchSummaries(12)
        this.summaries = Array.isArray(store.summaries) ? [...store.summaries] : []
        this.current = this.summaries[0] || null
      } catch (error) {
        uni.showToast({ title: error && error.message ? error.message : '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    selectSummary(item) {
      this.current = item
      uni.pageScrollTo({ scrollTop: 0, duration: 180 })
    },
    periodText(item) {
      if (!item || !item.periodStart) return ''
      const [year, month] = String(item.periodStart).split('-')
      return `${year} 年 ${Number(month)} 月`
    },
    signedMoney(item) {
      if (!item || item.comparisonAvailable === false) return '暂无对比'
      const num = Number(item.netWorthChange || 0)
      return `${num >= 0 ? '+' : '-'}${formatMoney(Math.abs(num))}`
    },
    changeClass(item) {
      if (!item || item.comparisonAvailable === false) return 'neutral'
      return Number(item.netWorthChange || 0) < 0 ? 'down' : 'up'
    },
    budgetText(item) {
      const amount = Number(item.budgetAmount || 0)
      if (amount <= 0) return '未设置预算'
      return `${formatMoney(item.budgetUsed)} / ${formatMoney(item.budgetAmount)}`
    },
    budgetWidth(item) {
      const rate = Math.min(1, Math.max(0, Number(item.budgetUsageRate || 0)))
      return `${Math.round(rate * 100)}%`
    },
    recordText(record, fallback) {
      if (!record) return fallback
      return `${record.name || '未分类'} ${formatMoney(record.amount)}`
    },
    goMilestones() {
      uni.navigateTo({ url: '/pages/insight/milestones' })
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

/* ========== Hero ========== */
.page-hero {
  padding: 34rpx;
  margin-bottom: 18rpx;
  border-radius: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.hero-period {
  display: block;
  font-size: 42rpx;
  line-height: 52rpx;
  font-weight: 900;
  color: var(--app-primary, #d3a414);
}

.hero-desc {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 28rpx;
}

.hero-grid > view {
  padding: 20rpx;
  border-radius: 16rpx;
  background: var(--app-soft-bg, #eff1f5);
}

.grid-label {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.grid-value {
  display: block;
  margin-top: 6rpx;
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  line-height: 38rpx;
  font-weight: 850;
  word-break: break-all;
}

.up {
  color: var(--app-primary, #d3a414);
}

.down {
  color: var(--app-danger, #d94a62);
}

.neutral {
  color: var(--app-muted, #7b8798);
}

.hero-footnote {
  display: block;
  margin-top: 24rpx;
  color: var(--app-faint, #9ba3b0);
  font-size: 22rpx;
  line-height: 32rpx;
}

/* ========== Section Cards ========== */
.section-card {
  margin-bottom: 18rpx;
  overflow: hidden;
  border-radius: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.section-head {
  padding: 26rpx 28rpx 18rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.section-head.inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 31rpx;
  line-height: 40rpx;
  font-weight: 850;
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.link {
  color: var(--app-primary, #d3a414);
  font-size: 25rpx;
  font-weight: 800;
}

/* ========== Stats Row ========== */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 24rpx 28rpx;
}

.stat-item {
  padding: 20rpx;
  border-radius: 16rpx;
  background: var(--app-soft-bg, #eff1f5);
}

.stat-label {
  display: block;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
}

.stat-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  line-height: 42rpx;
  font-weight: 900;
}

.income {
  color: var(--app-primary, #d3a414);
}

.expense {
  color: var(--app-danger, #d94a62);
}

/* ========== Budget ========== */
.budget-block {
  padding: 0 28rpx 26rpx;
}

.budget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  margin-bottom: 12rpx;
}

.budget-track {
  height: 12rpx;
  background: var(--app-soft-bg, #edf2f5);
  border-radius: 999rpx;
  overflow: hidden;
}

.budget-bar {
  height: 100%;
  background: var(--app-primary, #d3a414);
  border-radius: 999rpx;
}

/* ========== Record Lines ========== */
.record-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  min-height: 92rpx;
  padding: 0 28rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 700;
}

.record-line:last-child {
  border-bottom: none;
}

/* ========== History List ========== */
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  min-height: 92rpx;
  padding: 0 28rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  transition: background 0.18s;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-row.selected {
  background: var(--app-soft-bg, #eff1f5);
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
  flex: 1;
}

.summary-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--app-muted, #9ca3af);
}

.summary-dot.up {
  background: var(--app-primary, #d3a414);
}

.summary-dot.down {
  background: var(--app-danger, #d94a62);
}

.summary-dot.neutral {
  background: var(--app-muted, #9ca3af);
}

.row-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 800;
}

.row-subtitle {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.row-amount {
  font-size: 27rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.empty {
  padding: 38rpx;
  text-align: center;
  color: var(--app-faint, #94a3b8);
  font-size: 26rpx;
}
</style>
