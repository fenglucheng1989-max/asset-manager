<template>
  <view class="container">
    <view class="net-worth-card">
      <view class="card-title">净资产</view>
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

    <view class="login-tip" v-if="!hasToken">
      <text class="login-tip-title">先登录后开始记录资产</text>
      <text class="login-tip-text">登录后可新增账户、查看净资产和资产配置。</text>
      <button class="login-tip-btn" @click="goToMine">去登录</button>
    </view>

    <view class="error-tip" v-if="pageError">
      <text class="error-title">页面已进入保护模式</text>
      <text class="error-text">{{ pageError }}</text>
      <button class="error-btn" @click="refreshData">重新加载</button>
    </view>

    <view class="allocation-section" v-if="allocationItems.length > 0">
      <view class="section-title">资产配置</view>
      <view class="allocation-list">
        <view class="allocation-item" v-for="item in allocationItems" :key="item.name">
          <view class="allocation-row">
            <text class="allocation-name">{{ item.name }}</text>
            <view class="allocation-meta">
              <text class="allocation-amount">{{ formatMoney(item.value) }}</text>
              <text class="allocation-value">{{ item.percent }}%</text>
            </view>
          </view>
          <view class="allocation-track">
            <view class="allocation-bar" :style="{ width: item.percent + '%', backgroundColor: item.color }" />
          </view>
        </view>
      </view>
    </view>

    <view class="account-section">
      <view class="section-header">
        <text class="section-title">账户列表</text>
        <view class="add-btn" @click="goToAddAccount">
          <text class="add-icon">+</text>
          <text>添加</text>
        </view>
      </view>

      <view class="account-list" v-if="accounts.length > 0">
        <view
          class="account-item"
          v-for="account in accounts"
          :key="account.id"
          @click="goToEditAccount(account.id)"
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
              {{ account.isLiability ? '-' : '' }}{{ formatMoney(account.currentBalance) }}
            </text>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else>
        <text class="empty-text">暂无账户，点击右上角添加</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { formatMoney, getAccountTypeName } from '../../utils/money'

const COLORS = ['#2EBD85', '#5B8FF9', '#FF6B6B', '#FFC107', '#00BCD4', '#607D8B']
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
      pageError: ''
    }
  },
  computed: {
    allocationItems() {
      const groups = {}
      this.accounts
        .filter(account => !account.isLiability && account.includeInTotal)
        .forEach(account => {
          const typeName = getAccountTypeName(account.accountType)
          groups[typeName] = (groups[typeName] || 0) + Number(account.currentBalance || 0)
        })

      const total = Object.values(groups).reduce((sum, value) => sum + value, 0)
      if (total <= 0) return []

      return Object.keys(groups).map((name, index) => ({
        name,
        value: groups[name],
        percent: Math.round((groups[name] / total) * 100),
        color: COLORS[index % COLORS.length]
      }))
    }
  },
  onShow() {
    this.refreshData()
  },
  methods: {
    formatMoney,
    getAccountTypeName,
    async refreshData() {
      this.hasToken = !!uni.getStorageSync('token')
      this.pageError = uni.getStorageSync('__last_app_error__') || ''

      if (!this.hasToken) {
        this.overview = { ...DEFAULT_OVERVIEW }
        this.accounts = []
        return
      }

      try {
        const assetStore = useAssetStore()
        await assetStore.fetchOverview()
        await assetStore.fetchAccounts()
        this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
        this.accounts = Array.isArray(assetStore.accounts) ? assetStore.accounts : []
        this.pageError = ''
        uni.removeStorageSync('__last_app_error__')
      } catch (error) {
        const message = error && error.message ? error.message : String(error)
        this.pageError = `数据加载失败：${message}`
        this.overview = { ...DEFAULT_OVERVIEW }
        this.accounts = []
      }
    },
    getIconText(name) {
      return name ? name.substring(0, 1) : '?'
    },
    goToAddAccount() {
      uni.navigateTo({ url: '/pages/account/form' })
    },
    goToEditAccount(id) {
      uni.navigateTo({ url: `/pages/account/form?id=${id}` })
    },
    goToMine() {
      uni.switchTab({ url: '/pages/mine/mine' })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20rpx 20rpx calc(140rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.net-worth-card {
  background: linear-gradient(135deg, #111827 0%, #172033 100%);
  border-radius: 16rpx;
  padding: 40rpx;
  margin-bottom: 28rpx;
  color: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(17, 24, 39, 0.14);
}

.card-title {
  font-size: 28rpx;
  opacity: 0.8;
  margin-bottom: 16rpx;
}

.net-worth-amount {
  font-size: 58rpx;
  font-weight: bold;
  color: #FFD700;
  margin-bottom: 30rpx;
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
  min-width: 0;
}

.detail-label {
  font-size: 24rpx;
  opacity: 0.7;
  margin-bottom: 8rpx;
}

.detail-value {
  font-size: 32rpx;
  font-weight: 500;
  word-break: break-all;
}

.detail-value.liability {
  color: #FF6B6B;
}

.allocation-section,
.account-section,
.login-tip,
.error-tip {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
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
  color: #333333;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.login-tip-text {
  font-size: 26rpx;
  color: #888888;
  line-height: 38rpx;
  margin-bottom: 24rpx;
}

.login-tip-btn {
  background: #2EBD85;
  color: #ffffff;
  border-radius: 12rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 30rpx;
}

.error-title {
  font-size: 30rpx;
  color: #FF6B6B;
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
  color: #2EBD85;
  border: 1rpx solid #2EBD85;
  border-radius: 12rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333333;
}

.allocation-list {
  margin-top: 20rpx;
}

.allocation-item {
  margin-bottom: 24rpx;
}

.allocation-item:last-child {
  margin-bottom: 0;
}

.allocation-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 12rpx;
}

.allocation-name {
  font-size: 26rpx;
  color: #555555;
  flex: 1;
  min-width: 0;
}

.allocation-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.allocation-amount {
  font-size: 26rpx;
  color: #333333;
  font-weight: 600;
  margin-bottom: 4rpx;
}

.allocation-value {
  font-size: 24rpx;
  color: #888888;
}

.allocation-track {
  height: 18rpx;
  background: #f0f0f0;
  border-radius: 9rpx;
  overflow: hidden;
}

.allocation-bar {
  min-width: 8rpx;
  height: 18rpx;
  border-radius: 9rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.add-btn {
  display: flex;
  align-items: center;
  color: #2EBD85;
  font-size: 28rpx;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
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
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
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
  color: #333333;
  font-weight: 500;
  margin-bottom: 8rpx;
  max-width: 340rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-type {
  font-size: 24rpx;
  color: #999999;
}

.account-right {
  text-align: right;
  flex-shrink: 0;
  margin-left: 20rpx;
}

.account-balance {
  font-size: 30rpx;
  font-weight: 600;
  color: #333333;
}

.account-balance.liability {
  color: #FF6B6B;
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
