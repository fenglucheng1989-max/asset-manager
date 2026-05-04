<template>
  <view class="container" :style="themeVars">
    <!-- Compact Hero -->
    <view class="trend-hero">
      <view class="hero-row">
        <view class="hero-left">
          <text class="hero-label">净资产</text>
          <text class="hero-value">{{ formatMoney(overview.netWorth) }}</text>
          <view class="hero-change-row" v-if="periodChangeText">
            <text :class="['hero-change-tag', periodChange >= 0 ? 'up' : 'down']">
              {{ periodChange >= 0 ? '↑' : '↓' }} {{ formatMoney(Math.abs(periodChange)) }}
            </text>
            <text class="hero-change-period">较上期</text>
          </view>
        </view>
        <button class="snapshot-btn" :disabled="isSnapshotSaving" @click="handleCreateSnapshot">
          {{ isSnapshotSaving ? '记录中' : snapshotButtonText }}
        </button>
      </view>
    </view>

    <!-- Chart Section -->
    <view class="chart-section">
      <view class="chart-header">
        <text class="section-title">资产走势</text>
        <view class="range-tabs">
          <view
            v-for="item in rangeOptions"
            :key="item.value"
            :class="['range-tab', { active: chartRange === item.value }]"
            @click="chartRange = item.value"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="trend-chart" v-if="chartHasData">
        <view class="chart-body">
          <view class="chart-y-axis">
            <text class="y-label">{{ formatCompact(chartMax) }}</text>
            <text class="y-label">{{ formatCompact(chartMedian) }}</text>
            <text class="y-label">{{ formatCompact(chartMin) }}</text>
          </view>
          <view class="chart-plot" @touchend.stop>
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
              class="line-point-hit"
              :style="{ left: point.x + '%', bottom: point.y + '%' }"
              @click="selectChartPoint(point)"
            >
              <view class="line-point" :class="{ active: selectedPoint && selectedPoint.key === point.key }"></view>
            </view>
          </view>
        </view>
        <view class="chart-x-axis">
          <view class="axis-label" v-for="item in chartAxisLabels" :key="item.key">
            <text>{{ item.label }}</text>
          </view>
        </view>
        <view class="chart-info-bar" v-if="selectedPoint">
          <view class="info-bar-left">
            <text class="info-bar-dot"></text>
            <text class="info-bar-label">{{ selectedPoint.label }}</text>
          </view>
          <view class="info-bar-right">
            <text class="info-bar-value">{{ formatMoney(selectedPoint.value) }}</text>
            <text v-if="selectedPointChangeText" :class="['info-bar-delta', selectedPointDelta > 0 ? 'up' : 'down']">{{ selectedPointChangeText }}</text>
          </view>
        </view>
      </view>

      <view class="empty-card" v-else>
        <text>记录资产快照后，将在这里显示净资产走势。</text>
      </view>
    </view>

    <!-- Stats Summary -->
    <view class="stats-row" v-if="chartHasData">
      <view class="stat-card">
        <text class="stat-label">期间最高</text>
        <text class="stat-value up">{{ formatMoney(periodPeak) }}</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">期间最低</text>
        <text class="stat-value muted">{{ formatMoney(periodValley) }}</text>
      </view>
    </view>

    <!-- Snapshot History -->
    <view class="history-section">
      <view class="section-header">
        <text class="section-title">快照记录</text>
        <text class="section-subtitle">共 {{ snapshots.length }} 条</text>
      </view>

      <view class="timeline" v-if="snapshots.length > 0">
        <template v-for="group in groupedSnapshots" :key="group.key">
          <view class="tl-month">
            <text class="tl-month-label">{{ group.label }}</text>
          </view>
          <view class="tl-item" v-for="item in group.items" :key="item.id || item.snapshotDate">
            <view class="tl-item-body">
              <text class="tl-item-date">{{ formatSnapshotDay(item.snapshotDate) }}</text>
              <view class="tl-item-values">
                <text class="tl-item-value">{{ formatMoney(item.netWorth) }}</text>
                <text
                  v-if="item.change !== null"
                  :class="['tl-item-change', item.change >= 0 ? 'up' : 'down']"
                >{{ formatSignedMoney(item.change) }}</text>
              </view>
            </view>
          </view>
        </template>
        <button
          class="load-more-btn"
          v-if="hasMoreSnapshots"
          :disabled="isLoadingMoreSnapshots"
          @click="loadMoreSnapshots"
        >
          {{ isLoadingMoreSnapshots ? '加载中' : '加载更早记录' }}
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
import { formatMoney } from '../../utils/money'
import { getThemeMode, getThemeVars } from '../../utils/theme'

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
      themeVars: getThemeVars(),
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
    currentYear() {
      return new Date().getFullYear()
    },
    currentMonth() {
      return new Date().getMonth() + 1
    },
    chartItems() {
      if (this.chartRange === 'day') return this.buildDayTrend()
      if (this.chartRange === 'year') return this.buildYearTrend()
      return this.buildMonthTrend()
    },
    chartHasData() {
      return this.chartItems.some(item => item.value !== undefined)
    },
    chartValues() {
      return this.chartItems.map(item => item.value).filter(v => v !== undefined)
    },
    chartMax() {
      return this.chartValues.length ? Math.max(...this.chartValues) : 0
    },
    chartMin() {
      return this.chartValues.length ? Math.min(...this.chartValues) : 0
    },
    chartMedian() {
      return Math.round((this.chartMax + this.chartMin) / 2)
    },
    linePoints() {
      const items = this.chartItems
      const count = items.length
      return items
        .map((item, index) => ({
          ...item,
          x: count <= 1 ? 50 : 4 + (index / (count - 1)) * 92,
          y: Math.max(10, Math.min(86, item.height))
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
    selectedPointDelta() {
      if (!this.selectedPoint) return 0
      const points = this.linePoints
      const index = points.findIndex(item => item.key === this.selectedPoint.key)
      if (index <= 0) return 0
      const previous = points[index - 1]
      return Number(this.selectedPoint.value || 0) - Number(previous.value || 0)
    },
    selectedPointChangeText() {
      if (!this.selectedPoint) return ''
      const delta = this.selectedPointDelta
      if (delta === 0) return ''
      return `${delta > 0 ? '+' : ''}${this.formatCompact(delta)}`
    },
    periodChange() {
      const values = this.chartValues
      if (values.length < 2) return 0
      return values[values.length - 1] - values[0]
    },
    periodChangeText() {
      return this.chartValues.length >= 2
    },
    periodPeak() {
      return this.chartMax
    },
    periodValley() {
      return this.chartMin
    },
    chartAxisLabels() {
      const items = this.chartItems
      return items.filter(item => item.label)
    },
    groupedSnapshots() {
      const groups = []
      const reversed = [...this.snapshots].reverse()
      let currentMonth = ''
      let currentGroup = null
      reversed.forEach((item, index) => {
        const prev = index > 0 ? reversed[index - 1] : null
        const dateStr = String(item.snapshotDate || '')
        const parts = dateStr.split('-')
        const monthKey = parts.length >= 2 ? `${parts[0]}-${parts[1]}` : dateStr
        const monthLabel = parts.length >= 2 ? `${Number(parts[0])}年${Number(parts[1])}月` : dateStr
        const change = prev ? Number(item.netWorth || 0) - Number(prev.netWorth || 0) : null

        if (monthKey !== currentMonth) {
          currentMonth = monthKey
          currentGroup = { key: monthKey, label: monthLabel, items: [] }
          groups.push(currentGroup)
        }
        currentGroup.items.push({
          ...item,
          change: change !== null ? Math.round(change * 100) / 100 : null
        })
      })
      return groups
    }
  },
  watch: {
    chartRange() {
      this.selectedPoint = null
    }
  },
  onShow() {
    this.themeVars = getThemeVars(getThemeMode())
    this.refreshData()
  },
  methods: {
    formatMoney,
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
    formatSignedMoney(value) {
      const amount = Number(value || 0)
      if (amount === 0) return '¥0.00'
      return `${amount > 0 ? '+' : '-'}${formatMoney(Math.abs(amount))}`
    },
    formatCompact(value) {
      const num = Math.abs(Number(value || 0))
      if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
      return num.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    },
    formatSnapshotDay(value) {
      if (!value) return '--'
      const parts = String(value).split('-')
      if (parts.length !== 3) return value
      return `${Number(parts[1])}月${Number(parts[2])}日`
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
      this.selectedPoint = this.selectedPoint && this.selectedPoint.key === point.key ? null : point
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
  background: var(--app-page-bg, #f5f6f8);
  padding: 28rpx 24rpx calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* ---- Hero ---- */
.trend-hero {
  position: relative;
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  padding: 34rpx 30rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.hero-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
}

.hero-left {
  flex: 1;
  min-width: 0;
}

.hero-label {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-value {
  display: block;
  margin-top: 8rpx;
  color: var(--app-primary, #d3a414);
  font-size: 40rpx;
  line-height: 52rpx;
  font-weight: 800;
  word-break: break-all;
}

.hero-change-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 14rpx;
}

.hero-change-tag {
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  font-size: 23rpx;
  line-height: 32rpx;
  font-weight: 700;
}

.hero-change-tag.up {
  background: rgba(211, 164, 20, 0.12);
  color: var(--app-primary, #d3a414);
}

.hero-change-tag.down {
  background: rgba(217, 74, 98, 0.12);
  color: var(--app-liability-color, #d94a62);
}

.hero-change-period {
  color: var(--app-text, #17202a);
  opacity: 0.48;
  font-size: 22rpx;
}

.snapshot-btn {
  flex-shrink: 0;
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: rgba(211, 164, 20, 0.10);
  color: var(--app-primary, #d3a414);
  font-size: 25rpx;
  font-weight: 700;
  border: 1rpx solid rgba(211, 164, 20, 0.18);
}

/* ---- Chart Card ---- */
.chart-section {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 18rpx;
  padding: 30rpx 26rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 22rpx;
}

.section-title {
  color: var(--app-text, #17202a);
  font-size: 31rpx;
  font-weight: 750;
  flex-shrink: 0;
}

.range-tabs {
  display: flex;
  gap: 4rpx;
  padding: 4rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #f2f5f7);
}

.range-tab {
  height: 50rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  font-weight: 650;
}

.range-tab.active {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(15, 23, 42, 0.10);
}

/* ---- Chart Body ---- */
.trend-chart {
  padding: 0 4rpx;
}

.chart-body {
  display: flex;
  gap: 12rpx;
  align-items: stretch;
}

.chart-y-axis {
  width: 70rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0 0 10rpx;
  flex-shrink: 0;
}

.y-label {
  color: var(--app-faint, #94a3b8);
  font-size: 19rpx;
  line-height: 28rpx;
  text-align: right;
}

.chart-plot {
  flex: 1;
  position: relative;
  height: 280rpx;
  overflow: visible;
  box-sizing: border-box;
}

.chart-grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  background: var(--app-border, #e2e8ef);
}

.chart-grid-line.top {
  top: 14rpx;
}

.chart-grid-line.mid {
  top: 50%;
}

.chart-grid-line.bottom {
  bottom: 12rpx;
}

.line-segment {
  position: absolute;
  height: 5rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  transform-origin: 0 50%;
  box-shadow: 0 0 12rpx rgba(211, 164, 20, 0.18);
}

.line-point-hit {
  position: absolute;
  margin-left: -22rpx;
  margin-bottom: -22rpx;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.line-point {
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  background: var(--app-primary, #d3a414);
  border: 4rpx solid var(--app-card-bg, #ffffff);
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.16);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.line-point.active {
  transform: scale(1.35);
  box-shadow: 0 0 16rpx rgba(211, 164, 20, 0.25);
}

/* ---- Chart Info Bar ---- */
.chart-info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background: var(--app-soft-bg, #f3f6f8);
}

.info-bar-left {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
  flex: 1;
}

.info-bar-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--app-primary, #d3a414);
  flex-shrink: 0;
}

.info-bar-label {
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  font-weight: 650;
}

.info-bar-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.info-bar-value {
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 800;
}

.info-bar-delta {
  font-size: 23rpx;
  font-weight: 700;
  min-width: 80rpx;
  text-align: right;
}

.info-bar-delta.up {
  color: var(--app-positive-color, #1f8f72);
}

.info-bar-delta.down {
  color: var(--app-liability-color, #d94a62);
}

/* ---- X Axis ---- */
.chart-x-axis {
  display: flex;
  justify-content: space-between;
  gap: 4rpx;
  padding: 12rpx 0 0;
  margin-left: 82rpx;
}

.axis-label {
  flex: 1;
  min-width: 0;
  text-align: center;
}

.axis-label text {
  color: var(--app-muted, #7b8798);
  font-size: 20rpx;
  line-height: 28rpx;
}

/* ---- Stats Row ---- */
.stats-row {
  display: flex;
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.stat-card {
  flex: 1;
  min-width: 0;
  padding: 22rpx 18rpx;
  border-radius: 16rpx;
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.stat-label {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 22rpx;
  line-height: 30rpx;
  margin-bottom: 8rpx;
}

.stat-value {
  display: block;
  font-size: 27rpx;
  line-height: 38rpx;
  font-weight: 800;
  word-break: break-all;
}

.stat-value.up {
  color: var(--app-positive-color, #1f8f72);
}

.stat-value.down {
  color: var(--app-liability-color, #d94a62);
}

.stat-value.muted {
  color: var(--app-muted, #64748b);
  font-weight: 700;
}

/* ---- History ---- */
.history-section {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 18rpx;
  padding: 30rpx 26rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 10rpx;
}

.section-subtitle {
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  line-height: 32rpx;
}

/* ---- Timeline ---- */
.timeline {
  position: relative;
  padding-left: 48rpx;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 15rpx;
  top: 6rpx;
  bottom: 6rpx;
  width: 2rpx;
  border-radius: 1rpx;
  background: var(--app-border, #e2e8ef);
}

/* Month marker */
.tl-month {
  position: relative;
  padding: 20rpx 0 6rpx;
}

.tl-month:first-child {
  padding-top: 0;
}

.tl-month::before {
  content: '';
  position: absolute;
  left: -33rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: var(--app-primary, #d3a414);
  border: 4rpx solid var(--app-card-bg, #ffffff);
  box-shadow: 0 0 0 5rpx rgba(211, 164, 20, 0.18);
  z-index: 1;
}

.tl-month-label {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  font-weight: 700;
}

/* Item */
.tl-item {
  position: relative;
  padding: 12rpx 0;
}

.tl-item::before {
  content: '';
  position: absolute;
  left: -36rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--app-bg, #f5f6f8);
  border: 3rpx solid var(--app-border, #dde3ea);
  z-index: 1;
}

.tl-item-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  min-width: 0;
}

.tl-item-date {
  color: var(--app-text, #17202a);
  font-size: 26rpx;
  font-weight: 650;
}

.tl-item-values {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.tl-item-value {
  color: var(--app-text, #17202a);
  font-size: 27rpx;
  font-weight: 800;
}

.tl-item-change {
  font-size: 23rpx;
  font-weight: 700;
  min-width: 80rpx;
  text-align: right;
}

.tl-item-change.up {
  color: var(--app-positive-color, #1f8f72);
}

.tl-item-change.down {
  color: var(--app-liability-color, #d94a62);
}

.load-more-btn {
  margin: 22rpx 0 0;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  font-size: 26rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
}

/* ---- Empty ---- */
.empty-card {
  padding: 32rpx 24rpx;
  border-radius: 14rpx;
  background: var(--app-input-bg, #f6f8fb);
  color: var(--app-muted, #7b8798);
  font-size: 26rpx;
  line-height: 38rpx;
}
</style>
