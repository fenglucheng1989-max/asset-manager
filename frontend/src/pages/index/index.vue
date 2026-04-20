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

    <view class="allocation-section" v-if="allocationItems.length > 0">
      <view class="section-title">资产配置</view>
      <view class="allocation-list">
        <view class="allocation-item" v-for="item in allocationItems" :key="item.name">
          <view class="allocation-row">
            <text class="allocation-name">{{ item.name }}</text>
            <text class="allocation-value">{{ item.percent }}%</text>
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

export default {
  setup() {
    const assetStore = useAssetStore()
    return { assetStore }
  },
  computed: {
    overview() {
      return this.assetStore.overview
    },
    accounts() {
      return this.assetStore.accounts
    },
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
    const token = uni.getStorageSync('token')
    if (token) {
      this.assetStore.fetchOverview()
      this.assetStore.fetchAccounts()
    }
  },
  methods: {
    formatMoney,
    getAccountTypeName,
    getIconText(name) {
      return name ? name.substring(0, 1) : '?'
    },
    goToAddAccount() {
      uni.navigateTo({ url: '/pages/account/form' })
    },
    goToEditAccount(id) {
      uni.navigateTo({ url: `/pages/account/form?id=${id}` })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20rpx;
  min-height: 100vh;
}

.net-worth-card {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 16rpx;
  padding: 40rpx;
  margin-bottom: 30rpx;
  color: #ffffff;
}

.card-title {
  font-size: 28rpx;
  opacity: 0.8;
  margin-bottom: 16rpx;
}

.net-worth-amount {
  font-size: 56rpx;
  font-weight: bold;
  color: #FFD700;
  margin-bottom: 30rpx;
}

.card-detail {
  display: flex;
  justify-content: space-between;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-label {
  font-size: 24rpx;
  opacity: 0.7;
  margin-bottom: 8rpx;
}

.detail-value {
  font-size: 32rpx;
  font-weight: 500;
}

.detail-value.liability {
  color: #FF6B6B;
}

.allocation-section,
.account-section {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
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
  margin-bottom: 12rpx;
}

.allocation-name,
.allocation-value {
  font-size: 26rpx;
  color: #555555;
}

.allocation-track {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.allocation-bar {
  height: 16rpx;
  border-radius: 8rpx;
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
  padding: 24rpx 0;
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
  font-size: 32rpx;
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
