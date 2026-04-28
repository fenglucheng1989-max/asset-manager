<template>
  <view class="container">
    <view class="net-worth-card" @click="goToTrend">
      <view class="card-title-row">
        <text class="card-title">净资产</text>
        <text class="card-badge">趋势 ›</text>
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
      <text class="login-tip-text">登录后可新增账户、查看净资产趋势和管理我的账户。</text>
      <button class="login-tip-btn" @click="goToMine">去登录</button>
    </view>

    <view class="error-tip" v-if="pageError">
      <text class="error-title">数据加载失败</text>
      <text class="error-text">{{ pageError }}</text>
      <button class="error-btn" @click="handleManualRefresh">重新加载</button>
    </view>

    <view class="account-section" v-if="hasToken">
      <view class="section-header">
        <view>
          <text class="section-title">我的账户</text>
        </view>
      </view>

      <view class="account-list" v-if="accounts.length > 0">
        <view
          class="account-item"
          :class="{ dragging: dragIndex === index, swiped: swipedAccountId === account.id }"
          v-for="(account, index) in accountRows"
          :key="account.id"
          @click="handleAccountClick(account.id)"
          @touchstart="handleAccountTouchStart(index, account, $event)"
          @touchmove="handleAccountTouchMove(index, account, $event)"
          @touchend="handleAccountTouchEnd(index, account)"
          @touchcancel="handleAccountTouchEnd(index, account)"
          @longpress="startDrag(index, account)"
        >
          <view class="delete-underlay" @click.stop="confirmDeleteAccount(account)">
            <text>删除</text>
          </view>
          <view class="account-surface" :style="getAccountSurfaceStyle(account, index)">
          <view class="account-main">
            <view class="account-left">
              <view class="account-icon" :style="{ backgroundColor: account.colorHex || '#2EBD85' }">
                <text class="icon-text">{{ getIconText(account.name) }}</text>
              </view>
              <view class="account-info">
                <text class="account-name">{{ account.name }}</text>
                <view class="account-meta">
                  <text>{{ getAccountTypeName(account.accountType) }}</text>
                  <text class="meta-dot">·</text>
                  <text>占比 {{ account.structurePercent }}%</text>
                </view>
              </view>
            </view>
            <view class="account-right">
              <text :class="['account-balance', { liability: account.isLiability }]">
                {{ account.isLiability ? '-' : '' }}{{ formatMoney(toBaseAmount(account)) }}
              </text>
              <text class="account-currency" v-if="account.currency && account.currency !== 'CNY'">
                {{ account.currency }} {{ Number(account.currentBalance || 0).toFixed(2) }}
              </text>
            </view>
          </view>
          <view class="account-rank-track">
            <view class="account-rank-bar" :style="{ width: account.structurePercent + '%', backgroundColor: account.colorHex || '#2EBD85' }"></view>
          </view>
          <view class="drag-hint" v-if="dragIndex === index">
            <text>拖动调整顺序</text>
          </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else-if="!isLoading">
        <text class="empty-text">暂无账户</text>
      </view>

      <button class="add-account-btn" @click="goToAddAccount">
        <text class="add-icon">+</text>
        <text>新增账户</text>
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
      pageError: '',
      isLoading: false,
      isSorting: false,
      isDeleting: false,
      pendingSortSave: false,
      isPressDragging: false,
      dragRects: [],
      touchStartX: 0,
      touchStartY: 0,
      touchAccountId: null,
      dragIndex: -1,
      dragAccountId: null,
      dragOffsetY: 0,
      swipedAccountId: null
    }
  },
  computed: {
    accountRows() {
      const rows = [...this.accounts]
        .map(account => ({ ...account, baseAmount: toBaseAmount(account) }))
      const total = rows.reduce((sum, item) => sum + Math.abs(item.baseAmount), 0)
      return rows.map(item => ({
        ...item,
        structurePercent: total > 0 ? Math.max(1, Math.round(Math.abs(item.baseAmount) / total * 100)) : 0
      }))
    }
  },
  onShow() {
    this.refreshData()
  },
  beforeUnmount() {
    this.endDrag()
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
        return
      }

      this.isLoading = true
      try {
        const assetStore = useAssetStore()
        await assetStore.fetchOverview()
        await assetStore.fetchAccounts()
        this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
        this.accounts = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : []
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
    getIconText(name) {
      return name ? name.substring(0, 1) : '?'
    },
    handleAccountClick(id) {
      if (this.dragIndex >= 0 || this.isPressDragging) return
      if (this.swipedAccountId) {
        this.swipedAccountId = null
        return
      }
      uni.navigateTo({ url: `/pages/account/detail?id=${id}` })
    },
    handleAccountTouchStart(index, account, event) {
      const touch = event.touches && event.touches[0]
      if (!touch) return
      this.touchStartX = touch.clientX
      this.touchStartY = touch.clientY
      this.touchAccountId = account.id
    },
    handleAccountTouchMove(index, account, event) {
      const touch = event.touches && event.touches[0]
      if (!touch || this.touchAccountId !== account.id) return
      const deltaX = touch.clientX - this.touchStartX
      const deltaY = touch.clientY - this.touchStartY

      if (this.dragIndex >= 0) {
        this.dragOffsetY = touch.clientY - this.touchStartY
        this.handleDragMove(event)
        return
      }

      if (Math.abs(deltaX) > 42 && Math.abs(deltaX) > Math.abs(deltaY) * 1.4) {
        this.swipedAccountId = deltaX < 0 ? account.id : null
      }
    },
    handleAccountTouchEnd() {
      if (this.dragIndex >= 0) {
        this.endDrag()
      }
      this.touchAccountId = null
    },
    startDrag(index, account) {
      if (this.accounts.length < 2) return
      if (this.swipedAccountId) this.swipedAccountId = null
      this.dragIndex = index
      this.dragAccountId = account.id
      this.dragOffsetY = 0
      this.touchAccountId = account.id
      this.isPressDragging = true
      this.cacheDragRects()
      uni.vibrateShort && uni.vibrateShort()
    },
    cacheDragRects() {
      this.$nextTick(() => {
        uni.createSelectorQuery()
          .in(this)
          .selectAll('.account-item')
          .boundingClientRect((rects) => {
            this.dragRects = Array.isArray(rects) ? rects : []
          })
          .exec()
      })
    },
    getPointerY(event) {
      const touch = event && event.touches && event.touches[0]
      return touch ? touch.clientY : event.clientY
    },
    handleDragMove(event) {
      if (this.dragIndex < 0) return
      if (event && event.preventDefault) {
        event.preventDefault()
      }
      const y = this.getPointerY(event)
      const targetIndex = this.dragRects.findIndex(rect => y >= rect.top && y <= rect.bottom)
      if (targetIndex < 0 || targetIndex === this.dragIndex) return

      const nextAccounts = [...this.accounts]
      const [item] = nextAccounts.splice(this.dragIndex, 1)
      nextAccounts.splice(targetIndex, 0, item)
      this.accounts = nextAccounts
      this.dragIndex = targetIndex
      this.touchStartY = y
      this.dragOffsetY = 0
      this.pendingSortSave = true
      this.cacheDragRects()
    },
    endDrag() {
      if (this.dragIndex < 0) return
      this.dragIndex = -1
      this.dragAccountId = null
      this.dragOffsetY = 0
      this.dragRects = []
      this.touchAccountId = null
      setTimeout(() => {
        this.isPressDragging = false
      }, 80)
      if (this.pendingSortSave) {
        this.saveSort()
      }
    },
    async saveSort() {
      if (this.isSorting) return
      this.isSorting = true
      try {
        const assetStore = useAssetStore()
        const sortedIds = this.accounts.map(item => item.id)
        const res = await assetStore.updateSort(sortedIds)
        if (res && res.code === 200) {
          this.accounts = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : this.accounts
          this.pendingSortSave = false
          uni.showToast({ title: '排序已保存', icon: 'success' })
        }
      } catch (error) {
        await this.refreshData()
      } finally {
        this.isSorting = false
      }
    },
    getAccountSurfaceStyle(account) {
      if (this.dragAccountId === account.id) {
        return {
          transform: `translateY(${this.dragOffsetY}px)`,
          transition: 'none',
          zIndex: 3
        }
      }
      return {
        transform: this.swipedAccountId === account.id ? 'translateX(-132rpx)' : 'translateX(0)',
        transition: 'transform 0.18s ease',
        zIndex: 1
      }
    },
    confirmDeleteAccount(account) {
      uni.showModal({
        title: '删除账户',
        content: `确认删除“${account.name}”？删除后账户和余额历史将不再显示。`,
        confirmColor: '#d94a62',
        success: async (result) => {
          if (!result.confirm) return
          await this.deleteAccount(account.id)
        }
      })
    },
    async deleteAccount(id) {
      if (this.isDeleting) return
      this.isDeleting = true
      try {
        const assetStore = useAssetStore()
        const res = await assetStore.deleteAccount(id)
        if (res && res.code === 200) {
          uni.showToast({ title: '账户已删除', icon: 'success' })
          await this.refreshData()
          this.swipedAccountId = null
        }
      } finally {
        this.isDeleting = false
      }
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

.net-worth-card:active {
  opacity: 0.94;
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
  gap: 18rpx;
}

.account-item {
  position: relative;
  border-radius: 18rpx;
  user-select: none;
  overflow: hidden;
  background: #fbfcfd;
}

.account-item.dragging {
  box-shadow: 0 16rpx 34rpx rgba(34, 111, 99, 0.12);
  transform: scale(1.01);
  touch-action: none;
}

.delete-underlay {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 132rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #d94a62;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 800;
}

.account-surface {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  padding: 24rpx;
  border-radius: 18rpx;
  background: #fbfcfd;
  border: 1rpx solid #edf1f4;
  transition: transform 0.18s ease;
}

.account-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  width: 100%;
}

.account-rank-track {
  width: 100%;
  height: 8rpx;
  margin-top: 20rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
}

.account-rank-bar {
  height: 100%;
  border-radius: 999rpx;
}

.account-left {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  flex: 1;
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
  padding-top: 2rpx;
}

.account-name {
  font-size: 30rpx;
  color: #17202a;
  font-weight: 650;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-meta {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: #7b8798;
  font-size: 23rpx;
  line-height: 32rpx;
  min-width: 0;
}

.meta-dot {
  color: #7b8798;
}

.account-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
  text-align: right;
  flex-shrink: 0;
}

.drag-hint {
  margin-top: 16rpx;
  color: #226f63;
  font-size: 22rpx;
  line-height: 30rpx;
  text-align: right;
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
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 18rpx;
  background: #eef8f4;
  color: #226f63;
  border: 1rpx dashed #a9d6ca;
  font-size: 28rpx;
  font-weight: 750;
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
