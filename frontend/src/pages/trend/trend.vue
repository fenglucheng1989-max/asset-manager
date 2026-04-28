<template>
  <view class="container">
    <view class="trend-hero">
      <view class="hero-row">
        <view>
          <text class="hero-label">当前净资产</text>
          <text class="hero-value">{{ formatMoney(overview.netWorth) }}</text>
        </view>
        <button class="snapshot-btn" :disabled="isSnapshotSaving" @click="handleCreateSnapshot">
          {{ isSnapshotSaving ? '记录中' : snapshotButtonText }}
        </button>
      </view>
      <view class="hero-meta">
        <view>
          <text class="meta-label">总资产</text>
          <text class="meta-value">{{ formatMoney(overview.totalAsset) }}</text>
        </view>
        <view>
          <text class="meta-label">总负债</text>
          <text class="meta-value liability">{{ formatMoney(overview.totalLiability) }}</text>
        </view>
      </view>
    </view>

    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">资产走势</text>
        <text class="section-subtitle">{{ chartSubtitle }}</text>
      </view>

      <view class="range-tabs">
        <view
          v-for="item in rangeOptions"
          :key="item.value"
          class="range-tab"
          :class="{ active: chartRange === item.value }"
          @click="chartRange = item.value"
        >
          <text>{{ item.label }}</text>
        </view>
      </view>

      <view class="trend-chart" v-if="chartHasData">
        <view class="line-chart">
          <view class="chart-grid-line top"></view>
          <view class="chart-grid-line mid"></view>
          <view class="chart-grid-line bottom"></view>
          <view
            v-for="segment in lineSegments"
            :key="segment.key"
            class="line-segment"
            :style="segment.style"
          ></view>
          <view
            v-for="point in linePoints"
            :key="point.key"
            class="line-point"
            :style="{ left: point.x + '%', bottom: point.y + '%' }"
            @click="selectChartPoint(point)"
          ></view>
        </view>
        <view class="chart-tooltip" v-if="selectedPoint">
          <text class="tooltip-title">{{ selectedPoint.label || '当前点' }}</text>
          <text class="tooltip-value">{{ formatMoney(selectedPoint.value) }}</text>
          <text class="tooltip-sub">{{ selectedPointChangeText }}</text>
        </view>
        <view class="chart-axis">
          <view class="axis-label" v-for="item in chartItems" :key="item.key">
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="empty-card" v-else>
        <text>记录资产快照后，将在这里显示全年净资产走势。</text>
      </view>
    </view>

    <view class="ranking-section">
      <view class="section-header">
        <text class="section-title">账户金额排行</text>
        <text class="section-subtitle">按折合金额从高到低</text>
      </view>

      <view class="ranking-list" v-if="rankedAccounts.length > 0">
        <view class="ranking-item" v-for="(account, index) in rankedAccounts" :key="account.id">
          <view class="rank-badge">
            <text>{{ index + 1 }}</text>
          </view>
          <view class="rank-main">
            <view class="rank-row">
              <text class="rank-name">{{ account.name }}</text>
              <text :class="['rank-amount', { liability: account.isLiability }]">
                {{ account.isLiability ? '-' : '' }}{{ formatMoney(account.baseAmount) }}
              </text>
            </view>
            <view class="rank-row sub">
              <text>{{ getAccountTypeName(account.accountType) }}</text>
              <text v-if="account.currency && account.currency !== 'CNY'">
                {{ account.currency }} {{ Number(account.currentBalance || 0).toFixed(2) }}
              </text>
            </view>
            <view class="rank-track">
              <view class="rank-bar" :style="{ width: account.rankPercent + '%', backgroundColor: account.colorHex || '#2EBD85' }"></view>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-card" v-else>
        <text>暂无账户金额排行。</text>
      </view>
    </view>

    <view class="history-section">
      <view class="section-header">
        <text class="section-title">快照记录</text>
        <text class="section-subtitle">已加载 {{ snapshots.length }} 条</text>
      </view>

      <view class="history-list" v-if="snapshots.length > 0">
        <view class="history-item" v-for="item in visibleSnapshots" :key="item.id || item.snapshotDate">
          <view>
            <text class="history-date">{{ formatSnapshotDate(item.snapshotDate) }}</text>
            <text class="history-sub">资产 {{ formatMoney(item.totalAsset) }} / 负债 {{ formatMoney(item.totalLiability) }}</text>
          </view>
          <text class="history-value">{{ formatMoney(item.netWorth) }}</text>
        </view>
        <button class="load-more-btn" v-if="hasMoreSnapshots" :disabled="isLoadingMoreSnapshots" @click="loadMoreSnapshots">
          {{ isLoadingMoreSnapshots ? '加载中' : '查看更多' }}
        </button>
      </view>

      <view class="empty-card" v-else>
        <text>暂无历史记录。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { formatMoney, getAccountTypeName, toBaseAmount } from '../../utils/money'

const DEFAULT_OVERVIEW = {
  totalAsset: 0,
  totalLiability: 0,
  netWorth: 0,
  accountCount: 0,
  lastUpdateTime: null
}

export default {
  data() {
    return {
      overview: { ...DEFAULT_OVERVIEW },
      snapshots: [],
      accounts: [],
      isLoading: false,
      isSnapshotSaving: false,
      snapshotPageSize: 30,
      hasMoreSnapshots: true,
      isLoadingMoreSnapshots: false,
      selectedPoint: null,
      chartRange: 'month',
      rangeOptions: [
        { label: '日', value: 'day' },
        { label: '月', value: 'month' },
        { label: '年', value: 'year' }
      ]
    }
  },
  computed: {
    latestSnapshot() {
      if (!this.snapshots.length) return null
      return this.snapshots[this.snapshots.length - 1]
    },
    hasTodaySnapshot() {
      return this.latestSnapshot && this.latestSnapshot.snapshotDate === this.getTodayText()
    },
    snapshotButtonText() {
      return this.hasTodaySnapshot ? '更新今日' : '记录今日'
    },
    reversedSnapshots() {
      return [...this.snapshots].reverse()
    },
    visibleSnapshots() {
      return this.reversedSnapshots
    },
    selectedPointChangeText() {
      if (!this.selectedPoint) return ''
      const points = this.linePoints
      const index = points.findIndex(item => item.key === this.selectedPoint.key)
      if (index <= 0) return '暂无环比'
      const previous = points[index - 1]
      const change = Number(this.selectedPoint.value || 0) - Number(previous.value || 0)
      return `环比 ${this.formatSignedMoney(change)}`
    },
    currentYear() {
      return new Date().getFullYear()
    },
    currentMonth() {
      return new Date().getMonth() + 1
    },
    chartSubtitle() {
      if (this.chartRange === 'day') return `${this.currentMonth} 月每日快照`
      if (this.chartRange === 'month') return `${this.currentYear} 年每月快照`
      return '按年份汇总快照'
    },
    chartItems() {
      if (this.chartRange === 'day') return this.buildDayTrend()
      if (this.chartRange === 'year') return this.buildYearTrend()
      return this.buildMonthTrend()
    },
    chartHasData() {
      return this.chartItems.some(item => item.value !== undefined)
    },
    linePoints() {
      const items = this.chartItems
      const count = items.length
      return items
        .map((item, index) => ({
          ...item,
          x: count <= 1 ? 50 : 4 + (index / (count - 1)) * 92,
          y: Math.max(8, Math.min(88, item.height))
        }))
        .filter(item => item.value !== undefined)
    },
    lineSegments() {
      const points = this.linePoints
      const chartAspectRatio = 0.45
      return points.slice(1).map((point, index) => {
        const previous = points[index]
        const dx = point.x - previous.x
        const dy = point.y - previous.y
        const adjustedDy = dy * chartAspectRatio
        const width = Math.sqrt(dx * dx + adjustedDy * adjustedDy)
        const angle = Math.atan2(-adjustedDy, dx) * 180 / Math.PI
        return {
          key: `${previous.key}-${point.key}`,
          style: {
            left: previous.x + '%',
            bottom: previous.y + '%',
            width: width + '%',
            transform: `rotate(${angle}deg)`
          }
        }
      })
    },
    rankedAccounts() {
      const rows = this.accounts
        .map(account => ({
          ...account,
          baseAmount: toBaseAmount(account)
        }))
        .sort((a, b) => b.baseAmount - a.baseAmount)
      const max = rows.length ? Math.max(...rows.map(item => item.baseAmount)) : 0
      return rows.map(item => ({
        ...item,
        rankPercent: max > 0 ? Math.max(8, Math.round((item.baseAmount / max) * 100)) : 0
      }))
    }
  },
  onShow() {
    this.refreshData()
  },
  methods: {
    formatMoney,
    getAccountTypeName,
    buildMonthTrend() {
      const byMonth = new Map()
      this.snapshots.forEach(item => {
        const parts = String(item.snapshotDate || '').split('-')
        if (parts.length !== 3 || Number(parts[0]) !== this.currentYear) return
        byMonth.set(Number(parts[1]), Number(item.netWorth || 0))
      })
      return this.buildScaledItems(Array.from({ length: 12 }, (_, index) => {
        const month = index + 1
        return { key: `m-${month}`, label: `${month}月`, value: byMonth.get(month) }
      }))
    },
    buildDayTrend() {
      const now = new Date()
      const days = new Date(this.currentYear, this.currentMonth, 0).getDate()
      const byDay = new Map()
      this.snapshots.forEach(item => {
        const parts = String(item.snapshotDate || '').split('-')
        if (parts.length !== 3) return
        if (Number(parts[0]) !== this.currentYear || Number(parts[1]) !== this.currentMonth) return
        byDay.set(Number(parts[2]), Number(item.netWorth || 0))
      })
      return this.buildScaledItems(Array.from({ length: days }, (_, index) => {
        const day = index + 1
        const showLabel = day === 1 || day === days || day % 5 === 0
        return { key: `d-${day}`, label: showLabel ? `${day}` : '', value: byDay.get(day) }
      }))
    },
    buildYearTrend() {
      const byYear = new Map()
      this.snapshots.forEach(item => {
        const parts = String(item.snapshotDate || '').split('-')
        if (parts.length !== 3) return
        byYear.set(Number(parts[0]), Number(item.netWorth || 0))
      })
      const years = Array.from(byYear.keys()).sort()
      const visibleYears = years.length ? years : [this.currentYear]
      return this.buildScaledItems(visibleYears.map(year => ({
        key: `y-${year}`,
        label: String(year).slice(2),
        value: byYear.get(year)
      })))
    },
    buildScaledItems(items) {
      const values = items.map(item => item.value).filter(value => value !== undefined)
      const min = values.length ? Math.min(...values) : 0
      const max = values.length ? Math.max(...values) : 0
      const range = max - min
      return items.map(item => ({
        ...item,
        height: item.value === undefined ? 0 : range === 0 ? 54 : 18 + ((item.value - min) / range) * 72
      }))
    },
    async refreshData() {
      if (this.isLoading) return
      this.isLoading = true
      try {
        const assetStore = useAssetStore()
        await assetStore.fetchOverview()
        await assetStore.fetchAccounts()
        const snapshotRes = await assetStore.fetchSnapshots({ limit: this.snapshotPageSize, offset: 0 })
        this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
        this.accounts = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : []
        this.snapshots = Array.isArray(assetStore.snapshots) ? [...assetStore.snapshots] : []
        this.hasMoreSnapshots = Array.isArray(snapshotRes.data) && snapshotRes.data.length === this.snapshotPageSize
        this.selectedPoint = null
      } finally {
        this.isLoading = false
      }
    },
    selectChartPoint(point) {
      this.selectedPoint = point
    },
    async loadMoreSnapshots() {
      if (this.isLoadingMoreSnapshots || !this.hasMoreSnapshots) return
      this.isLoadingMoreSnapshots = true
      try {
        const assetStore = useAssetStore()
        const res = await assetStore.fetchSnapshots({
          limit: this.snapshotPageSize,
          offset: this.snapshots.length,
          mode: 'prepend'
        })
        this.snapshots = Array.isArray(assetStore.snapshots) ? [...assetStore.snapshots] : []
        this.hasMoreSnapshots = Array.isArray(res.data) && res.data.length === this.snapshotPageSize
      } finally {
        this.isLoadingMoreSnapshots = false
      }
    },
    async handleCreateSnapshot() {
      if (this.isSnapshotSaving) return
      this.isSnapshotSaving = true
      try {
        const assetStore = useAssetStore()
        const res = await assetStore.createSnapshot()
        if (res && res.code === 200) {
          await assetStore.fetchOverview()
          const snapshotRes = await assetStore.fetchSnapshots({ limit: this.snapshotPageSize, offset: 0 })
          this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
          this.snapshots = Array.isArray(assetStore.snapshots) ? [...assetStore.snapshots] : []
          this.hasMoreSnapshots = Array.isArray(snapshotRes.data) && snapshotRes.data.length === this.snapshotPageSize
          uni.showToast({ title: '快照已记录', icon: 'success' })
        }
      } finally {
        this.isSnapshotSaving = false
      }
    },
    formatSignedMoney(value) {
      const amount = Number(value || 0)
      if (amount === 0) return '¥0.00'
      return `${amount > 0 ? '+' : '-'}${formatMoney(Math.abs(amount))}`
    },
    formatSnapshotDate(value) {
      if (!value) return '--'
      const parts = String(value).split('-')
      if (parts.length !== 3) return value
      return `${Number(parts[1])}月${Number(parts[2])}日`
    },
    getTodayText() {
      const now = new Date()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      return `${now.getFullYear()}-${month}-${day}`
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 28rpx 24rpx calc(96rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.trend-hero,
.chart-section,
.ranking-section,
.history-section {
  background: #ffffff;
  border: 1rpx solid #edf1f4;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 26rpx;
  box-shadow: 0 12rpx 30rpx rgba(26, 42, 58, 0.06);
}

.trend-chart {
  padding: 16rpx 8rpx 4rpx;
}

.range-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6rpx;
  padding: 6rpx;
  border-radius: 14rpx;
  background: #f2f5f7;
  margin-bottom: 18rpx;
}

.range-tab {
  height: 58rpx;
  border-radius: 11rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 26rpx;
  font-weight: 650;
}

.range-tab.active {
  background: #17202a;
  color: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(23, 32, 42, 0.16);
}

.line-chart {
  position: relative;
  height: 260rpx;
  margin: 8rpx 8rpx 0;
  overflow: hidden;
  box-sizing: border-box;
}

.chart-grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  background: #e2e8ef;
}

.chart-grid-line.top {
  top: 12%;
}

.chart-grid-line.mid {
  top: 50%;
}

.chart-grid-line.bottom {
  bottom: 0;
}

.line-segment {
  position: absolute;
  height: 4rpx;
  border-radius: 999rpx;
  background: #2ebd85;
  transform-origin: 0 50%;
  box-shadow: 0 4rpx 10rpx rgba(46, 189, 133, 0.2);
}

.line-point {
  position: absolute;
  width: 18rpx;
  height: 18rpx;
  margin-left: -9rpx;
  margin-bottom: -9rpx;
  border-radius: 50%;
  background: #f4c95d;
  border: 4rpx solid #ffffff;
  box-shadow: 0 2rpx 10rpx rgba(23, 32, 42, 0.18);
}

.chart-axis {
  display: flex;
  justify-content: space-between;
  gap: 8rpx;
  padding: 14rpx 0 0;
}

.chart-tooltip {
  margin: 10rpx 8rpx 0;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: #f6f8fb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.tooltip-title,
.tooltip-sub {
  color: #7b8798;
  font-size: 23rpx;
}

.tooltip-value {
  flex: 1;
  color: #17202a;
  font-size: 28rpx;
  font-weight: 800;
}

.axis-label {
  flex: 1;
  min-width: 0;
  text-align: center;
}

.chart-month {
  color: #7b8798;
  font-size: 20rpx;
  line-height: 28rpx;
}

.axis-label text {
  color: #7b8798;
  font-size: 20rpx;
  line-height: 28rpx;
}

.ranking-list {
  display: flex;
  flex-direction: column;
}

.ranking-item {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf1f4;
}

.ranking-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rank-badge {
  width: 48rpx;
  height: 48rpx;
  border-radius: 14rpx;
  background: #f2f6f4;
  color: #226f63;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
}

.rank-row.sub {
  margin-top: 6rpx;
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
}

.rank-name {
  color: #17202a;
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 750;
}

.rank-amount {
  color: #17202a;
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.rank-amount.liability {
  color: #d94a62;
}

.rank-track {
  height: 12rpx;
  margin-top: 16rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
}

.rank-bar {
  height: 12rpx;
  border-radius: 999rpx;
}

.trend-hero {
  background: linear-gradient(135deg, #14202d 0%, #20384a 58%, #22564d 100%);
  color: #ffffff;
}

.hero-row,
.hero-meta,
.section-header,
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
}

.hero-label,
.meta-label {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-value {
  display: block;
  margin-top: 14rpx;
  color: #f4c95d;
  font-size: 58rpx;
  line-height: 72rpx;
  font-weight: 800;
  word-break: break-all;
}

.snapshot-btn {
  flex-shrink: 0;
  margin: 0;
  height: 68rpx;
  line-height: 68rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  font-size: 26rpx;
}

.hero-meta {
  margin-top: 30rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.14);
}

.meta-value {
  display: block;
  margin-top: 6rpx;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
}

.meta-value.liability {
  color: #ff9aa9;
}

.section-header {
  align-items: flex-start;
  margin-bottom: 24rpx;
}

.section-title {
  color: #17202a;
  font-size: 32rpx;
  font-weight: 700;
}

.section-subtitle {
  color: #7b8798;
  font-size: 24rpx;
  line-height: 34rpx;
}

.empty-card {
  padding: 24rpx;
  border-radius: 14rpx;
  background: #f6f8fb;
}

.empty-card {
  color: #7b8798;
  font-size: 26rpx;
  line-height: 38rpx;
}

.history-list {
  display: flex;
  flex-direction: column;
}

.history-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf1f4;
}

.history-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.history-date {
  display: block;
  color: #17202a;
  font-size: 28rpx;
  font-weight: 700;
}

.history-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b8798;
  font-size: 23rpx;
  line-height: 34rpx;
}

.history-value {
  flex-shrink: 0;
  color: #17202a;
  font-size: 30rpx;
  font-weight: 800;
}

.load-more-btn {
  margin: 22rpx 0 0;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 999rpx;
  background: #f6f8fb;
  color: #226f63;
  font-size: 28rpx;
}
</style>
