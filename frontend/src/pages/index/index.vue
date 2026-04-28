<template>
  <view class="container">
    <view class="net-worth-card">
      <view class="card-title-row">
        <text class="card-title">净资产</text>
        <text class="card-badge">{{ overview.accountCount || accounts.length }} 个账户</text>
      </view>
      <view class="net-worth-amount">{{ formatMoney(overview.netWorth) }}</view>
      <view class="card-detail">
        <view class="detail-item">
          <text class="detail-label">总资产</text>
          <text class="detail-value">{{ formatMoney(overview.totalAsset) }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">总负债</text>
          <text class="detail-value liability">{{ formatMoney(overview.totalLiability) }}</text>
        </view>
      </view>
    </view>

    <view class="loading-tip" v-if="isLoading">
      <text>正在同步资产数据...</text>
    </view>

    <view class="login-tip" v-if="!hasToken">
      <text class="login-tip-title">先登录后开始记录资产</text>
      <text class="login-tip-text">登录后可新增账户、查看净资产趋势和账户金额排行。</text>
      <button class="login-tip-btn" @click="goToMine">去登录</button>
    </view>

    <view class="error-tip" v-if="pageError">
      <text class="error-title">数据加载失败</text>
      <text class="error-text">{{ pageError }}</text>
      <button class="error-btn" @click="handleManualRefresh">重新加载</button>
    </view>

    <view class="trend-section" v-if="hasToken" @click="goToTrend">
      <view class="section-header">
        <view>
          <text class="section-title">资产趋势</text>
          <text class="section-subtitle">{{ latestSnapshot ? `最近快照 ${formatSnapshotDate(latestSnapshot.snapshotDate)}` : '查看净资产走势和快照记录' }}</text>
        </view>
        <text class="section-arrow">›</text>
      </view>
    </view>

    <view class="account-section" v-if="hasToken">
      <view class="section-header">
        <text class="section-title">账户金额排行</text>
      </view>

      <view class="account-list" v-if="accounts.length > 0">
        <view
          class="account-item"
          v-for="(account, index) in rankedAccounts"
          :key="account.id"
          @click="handleAccountClick(account.id)"
        >
          <view class="account-left">
            <view class="account-icon" :style="{ backgroundColor: account.colorHex || '#2EBD85' }">
              <text class="icon-text">{{ getIconText(account.name) }}</text>
            </view>
            <view class="account-info">
              <text class="account-name">{{ account.name }}</text>
              <text class="account-type">{{ getAccountTypeName(account.accountType) }}</text>
            </view>
          </view>
          <view class="account-right">
            <text :class="['account-balance', { liability: account.isLiability }]">
              {{ account.isLiability ? '-' : '' }}{{ formatMoney(toBaseAmount(account)) }}
            </text>
            <text class="account-currency">{{ account.rankPercent }}%</text>
          </view>
          <view class="account-rank-track">
            <view class="account-rank-bar" :style="{ width: account.rankPercent + '%', backgroundColor: account.colorHex || '#2EBD85' }"></view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else-if="!isLoading">
        <text class="empty-text">暂无账户</text>
      </view>

      <button class="add-account-btn" @click="goToAddAccount">
        <text class="add-icon">+</text>
        <text>添加账户</text>
      </button>
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
      hasToken: false,
      overview: { ...DEFAULT_OVERVIEW },
      accounts: [],
      snapshots: [],
      pageError: '',
      isLoading: false
    }
  },
  computed: {
    rankedAccounts() {
      const rows = [...this.accounts]
        .map(account => ({ ...account, baseAmount: toBaseAmount(account) }))
        .sort((a, b) => b.baseAmount - a.baseAmount)
      const total = rows.reduce((sum, item) => sum + Math.abs(item.baseAmount), 0)
      return rows.map(item => ({
        ...item,
        rankPercent: total > 0 ? Math.max(1, Math.round(Math.abs(item.baseAmount) / total * 100)) : 0
      }))
    },
    latestSnapshot() {
      if (!this.snapshots.length) return null
      return this.snapshots[this.snapshots.length - 1]
    }
  },
  onShow() {
    this.refreshData()
  },
  methods: {
    formatMoney,
    getAccountTypeName,
    toBaseAmount,
    async handleManualRefresh() {
      await this.refreshData({ showToast: true })
    },
    async refreshData(options = {}) {
      if (this.isLoading) return
      this.hasToken = !!uni.getStorageSync('token')
      this.pageError = uni.getStorageSync('__last_app_error__') || ''

      if (!this.hasToken) {
        this.overview = { ...DEFAULT_OVERVIEW }
        this.accounts = []
        this.snapshots = []
        return
      }

      this.isLoading = true
      try {
        const assetStore = useAssetStore()
        await assetStore.fetchOverview()
        await assetStore.fetchAccounts()
        await assetStore.fetchSnapshots()
        this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
        this.accounts = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : []
        this.snapshots = Array.isArray(assetStore.snapshots) ? [...assetStore.snapshots] : []
        this.pageError = ''
        uni.removeStorageSync('__last_app_error__')
        if (options.showToast) {
          uni.showToast({ title: '刷新成功', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : String(error)
        this.pageError = `数据加载失败：${message}`
        this.overview = { ...DEFAULT_OVERVIEW }
        this.accounts = []
      } finally {
        this.isLoading = false
      }
    },
    formatSnapshotDate(value) {
      if (!value) return '--'
      const parts = String(value).split('-')
      if (parts.length !== 3) return value
      return `${Number(parts[1])}月${Number(parts[2])}日`
    },
    getIconText(name) {
      return name ? name.substring(0, 1) : '?'
    },
    handleAccountClick(id) {
      uni.navigateTo({ url: `/pages/account/detail?id=${id}` })
    },
    goToAddAccount() {
      uni.navigateTo({ url: '/pages/account/form' })
    },
    goToEditAccount(id) {
      uni.navigateTo({ url: `/pages/account/form?id=${id}` })
    },
    goToTrend() {
      uni.navigateTo({ url: '/pages/trend/trend' })
    },
    goToMine() {
      uni.switchTab({ url: '/pages/mine/mine' })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 28rpx 24rpx calc(144rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.net-worth-card {
  background: linear-gradient(135deg, #14202d 0%, #20384a 58%, #22564d 100%);
  border-radius: 16rpx;
  padding: 40rpx 34rpx 34rpx;
  margin-bottom: 26rpx;
  color: #ffffff;
  box-shadow: 0 18rpx 40rpx rgba(17, 32, 45, 0.2);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.card-title {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.78);
}

.card-badge {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.82);
  font-size: 22rpx;
}

.net-worth-amount {
  font-size: 64rpx;
  line-height: 78rpx;
  font-weight: 800;
  color: #f4c95d;
  margin-bottom: 34rpx;
  word-break: break-all;
}

.card-detail {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
}

.detail-item {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  padding: 22rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.1);
}

.detail-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.68);
  margin-bottom: 8rpx;
}

.detail-value {
  font-size: 32rpx;
  font-weight: 500;
  word-break: break-all;
}

.detail-value.liability {
  color: #ff9aa9;
}

.account-section,
.trend-section,
.login-tip,
.error-tip,
.loading-tip {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 26rpx;
  border: 1rpx solid #edf1f4;
  box-shadow: 0 12rpx 30rpx rgba(26, 42, 58, 0.06);
}

.loading-tip {
  color: #666666;
  font-size: 26rpx;
}

.login-tip {
  display: flex;
  flex-direction: column;
}

.error-tip {
  display: flex;
  flex-direction: column;
  border: 1rpx solid #ffd6d6;
}

.login-tip-title {
  font-size: 32rpx;
  color: #17202a;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.login-tip-text {
  font-size: 26rpx;
  color: #888888;
  line-height: 38rpx;
  margin-bottom: 24rpx;
}

.login-tip-btn {
  background: linear-gradient(135deg, #2ebd85, #239a88);
  color: #ffffff;
  border-radius: 12rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 30rpx;
}

.error-title {
  font-size: 30rpx;
  color: #d94a62;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.error-text {
  font-size: 24rpx;
  color: #777777;
  line-height: 36rpx;
  margin-bottom: 20rpx;
  word-break: break-all;
}

.error-btn {
  background: #ffffff;
  color: #226f63;
  border: 1rpx solid #b9d7ce;
  border-radius: 12rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #17202a;
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: #7b8798;
  font-size: 24rpx;
  line-height: 34rpx;
}

.section-arrow {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f2f6f4;
  color: #226f63;
  font-size: 44rpx;
  line-height: 44rpx;
  font-weight: 300;
}

.trend-section {
  position: relative;
  overflow: hidden;
}

.trend-section:active {
  opacity: 0.88;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header {
  margin-bottom: 20rpx;
}

.add-icon {
  font-size: 36rpx;
  margin-right: 8rpx;
}

.account-list {
  display: flex;
  flex-direction: column;
}

.account-item {
  position: relative;
  flex-wrap: wrap;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #edf1f4;
  user-select: none;
}

.account-rank-track {
  width: 100%;
  height: 10rpx;
  margin-left: 100rpx;
  margin-top: 14rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
}

.account-rank-bar {
  height: 100%;
  border-radius: 999rpx;
}

.account-item.dragging {
  opacity: 0.72;
  background: #f8fafc;
}

.account-item:last-child {
  border-bottom: none;
}

.account-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.account-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  flex-shrink: 0;
}

.icon-text {
  color: #ffffff;
  font-size: 32rpx;
  font-weight: bold;
}

.account-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.account-name {
  font-size: 30rpx;
  color: #17202a;
  font-weight: 650;
  margin-bottom: 8rpx;
  max-width: 340rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-type {
  font-size: 24rpx;
  color: #7b8798;
}

.account-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 18rpx;
  text-align: right;
  flex-shrink: 0;
  margin-left: 20rpx;
}

.account-balance {
  font-size: 30rpx;
  font-weight: 600;
  color: #17202a;
}

.account-balance.liability {
  color: #d94a62;
}

.account-currency {
  color: #7b8798;
  font-size: 22rpx;
  line-height: 30rpx;
}

.add-account-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 24rpx 0 0;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 14rpx;
  background: #f6f8fb;
  color: #226f63;
  border: 1rpx solid #dbe7e3;
  font-size: 28rpx;
}

.empty-state {
  padding: 60rpx 0;
  text-align: center;
}

.empty-text {
  color: #999999;
  font-size: 28rpx;
}
</style>
