<template>
  <view class="container" :style="themeVars">
    <view class="summary-card">
      <view>
        <text class="summary-title">资产快照</text>
        <text class="summary-subtitle">记录每日总资产、总负债和净资产</text>
      </view>
      <button class="primary-btn" :loading="creating" @click="createTodaySnapshot">生成今日</button>
    </view>

    <view class="list-card">
      <view v-if="snapshots.length === 0 && !loading" class="empty">暂无快照</view>
      <view v-for="item in snapshots" :key="item.id" class="snapshot-row">
        <view class="snapshot-main">
          <text class="snapshot-date">{{ item.snapshotDate }}</text>
          <text class="snapshot-sub">资产 {{ formatMoney(item.totalAsset) }} · 负债 {{ formatMoney(item.totalLiability) }}</text>
        </view>
        <text class="snapshot-value">{{ formatMoney(item.netWorth) }}</text>
      </view>
    </view>

    <button v-if="hasMore" class="load-btn" :loading="loading" @click="loadMore">加载更多</button>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { formatMoney } from '../../utils/money'
import { getThemeVars } from '../../utils/theme'

const PAGE_SIZE = 10

export default {
  data() {
    return {
      loading: false,
      creating: false,
      offset: 0,
      hasMore: true,
      snapshots: [],
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.reload()
  },
  methods: {
    formatMoney,
    async reload() {
      this.offset = 0
      this.hasMore = true
      this.snapshots = []
      await this.loadMore()
    },
    async loadMore() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const store = useAssetStore()
        const res = await store.fetchSnapshots({ limit: PAGE_SIZE, offset: this.offset, mode: 'replace' })
        const rows = res && Array.isArray(res.data) ? res.data : []
        this.snapshots = [...this.snapshots, ...rows]
        this.offset += rows.length
        this.hasMore = rows.length === PAGE_SIZE
      } finally {
        this.loading = false
      }
    },
    async createTodaySnapshot() {
      if (this.creating) return
      this.creating = true
      try {
        const store = useAssetStore()
        const res = await store.createSnapshot()
        if (res && res.code === 200) {
          uni.showToast({ title: '今日快照已更新', icon: 'success' })
          await this.reload()
        }
      } finally {
        this.creating = false
      }
    }
  }
}
</script>

<style scoped>
.container {
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.summary-card,
.list-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 18rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.summary-card {
  padding: 28rpx;
  margin-bottom: 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.summary-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 34rpx;
  line-height: 44rpx;
  font-weight: 850;
}

.summary-subtitle,
.snapshot-sub,
.empty {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.primary-btn,
.load-btn {
  margin: 0;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.primary-btn {
  width: 168rpx;
  height: 64rpx;
  line-height: 64rpx;
  flex-shrink: 0;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
}

.list-card {
  overflow: hidden;
}

.snapshot-row {
  min-height: 104rpx;
  padding: 18rpx 26rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.snapshot-row:last-child {
  border-bottom: none;
}

.snapshot-main {
  min-width: 0;
}

.snapshot-date {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 800;
}

.snapshot-value {
  color: var(--app-primary, #d3a414);
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 850;
  flex-shrink: 0;
}

.empty {
  padding: 48rpx 28rpx;
  text-align: center;
}

.load-btn {
  margin-top: 20rpx;
  height: 78rpx;
  line-height: 78rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  border: 1rpx solid var(--app-border, #edf1f4);
}
</style>
