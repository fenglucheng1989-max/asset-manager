<template>
  <view class="container" :style="themeVars">
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
          <view class="account-accent" :style="getAccountAccentStyle(account)"></view>
          <view class="account-main">
            <view class="account-left">
              <view class="account-icon" :class="getAccountIconClass(account)" :style="getAccountIconStyle(account)">
                <view class="account-line-mark"></view>
              </view>
              <view class="account-info">
                <text class="account-name">{{ account.name }}</text>
                <view class="account-meta">
                  <text>{{ getAccountTypeName(account.accountType) }}</text>
                  <template v-if="account.currency && account.currency !== 'CNY'">
                    <text class="meta-dot">·</text>
                    <text>{{ account.currency }}</text>
                  </template>
                </view>
              </view>
            </view>
            <view class="account-right">
              <text :class="['account-balance', { liability: account.isLiability }]">
                {{ account.isLiability ? '-' : '' }}{{ formatMoney(toBaseAmount(account)) }}
              </text>
            </view>
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

    <custom-tab-bar />
  </view>
</template>

<script>
import CustomTabBar from '../../custom-tab-bar/index.vue'
import { useAssetStore } from '../../store/asset'
import { formatMoney, getAccountTypeName, toBaseAmount } from '../../utils/money'
import { getThemeMode, getThemeVars } from '../../utils/theme'

const DEFAULT_OVERVIEW = {
  totalAsset: 0,
  totalLiability: 0,
  netWorth: 0,
  accountCount: 0,
  lastUpdateTime: null
}

export default {
  components: { CustomTabBar },
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
      swipedAccountId: null,
      themeVars: getThemeVars()
    }
  },
  computed: {
    accountRows() {
      return [...this.accounts].map(account => ({ ...account, baseAmount: toBaseAmount(account) }))
    }
  },
  onShow() {
    this.themeVars = getThemeVars(getThemeMode())
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
    getAccountIconClass(account) {
      const map = {
        BANK: 'account-icon-bank',
        CASH: 'account-icon-cash',
        E_WALLET: 'account-icon-wallet',
        INVESTMENT: 'account-icon-trend',
        REAL_ESTATE: 'account-icon-home',
        OTHER_ASSET: 'account-icon-box',
        CREDIT_CARD: 'account-icon-card',
        LOAN: 'account-icon-loan',
        OTHER_LIABILITY: 'account-icon-bill'
      }
      return map[account.accountType] || 'account-icon-box'
    },
    getAccountIconStyle(account) {
      return {
        color: account.isLiability ? 'var(--app-liability-color, #d94a62)' : 'var(--app-primary-dark, #8f6b00)'
      }
    },
    getAccountAccentStyle(account) {
      const color = this.normalizeColor(account.colorHex) || (account.isLiability ? '#d94a62' : '#d3a414')
      return {
        background: this.hexToRgba(color, 0.44)
      }
    },
    normalizeColor(value) {
      if (!value || typeof value !== 'string') return ''
      const color = value.trim()
      if (/^#[0-9a-fA-F]{6}$/.test(color)) return color
      if (/^#[0-9a-fA-F]{3}$/.test(color)) {
        return `#${color.slice(1).split('').map(char => char + char).join('')}`
      }
      return ''
    },
    hexToRgba(hex, alpha) {
      const color = this.normalizeColor(hex)
      if (!color) return `rgba(46, 189, 133, ${alpha})`
      const value = color.slice(1)
      const r = parseInt(value.slice(0, 2), 16)
      const g = parseInt(value.slice(2, 4), 16)
      const b = parseInt(value.slice(4, 6), 16)
      return `rgba(${r}, ${g}, ${b}, ${alpha})`
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
  position: relative;
  overflow: hidden;
  background: var(--app-outfit-header-bg, var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #20384a 58%, #22564d 100%)));
  border-radius: 22rpx;
  padding: 40rpx 34rpx 34rpx;
  margin-bottom: 26rpx;
  color: var(--app-outfit-header-text, var(--app-hero-text, #ffffff));
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.10);
}

.net-worth-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--app-outfit-header-pattern, none);
  opacity: 0.9;
  pointer-events: none;
}

.net-worth-card:active {
  opacity: 0.94;
}

.card-title-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.card-title {
  font-size: 28rpx;
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255, 255, 255, 0.78)));
}

.card-badge {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: var(--app-hero-badge-bg, rgba(255, 255, 255, 0.12));
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255, 255, 255, 0.82)));
  font-size: 22rpx;
}

.net-worth-amount {
  position: relative;
  z-index: 1;
  font-size: 64rpx;
  line-height: 78rpx;
  font-weight: 800;
  color: var(--app-outfit-header-accent, var(--app-hero-accent, #f4c95d));
  margin-bottom: 34rpx;
  word-break: break-all;
}

.card-detail {
  position: relative;
  z-index: 1;
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
  background: var(--app-section-bg, rgba(255, 255, 255, 0.1));
}

.detail-label {
  font-size: 24rpx;
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255, 255, 255, 0.68)));
  margin-bottom: 8rpx;
}

.detail-value {
  font-size: 32rpx;
  font-weight: 500;
  word-break: break-all;
  color: var(--app-outfit-header-text, var(--app-hero-text, #ffffff));
}

.detail-value.liability {
  color: var(--app-liability-color, #ff9aa9);
}

.account-section,
.login-tip,
.error-tip,
.loading-tip {
  background: var(--app-card-bg, #ffffff);
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 26rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 12rpx 30rpx rgba(26, 42, 58, 0.06));
}

.loading-tip {
  color: var(--app-muted, #666666);
  font-size: 26rpx;
}

.login-tip {
  display: flex;
  flex-direction: column;
}

.error-tip {
  display: flex;
  flex-direction: column;
  border: 1rpx solid var(--app-danger, #ffd6d6);
}

.login-tip-title {
  font-size: 32rpx;
  color: var(--app-text, #17202a);
  font-weight: 700;
  margin-bottom: 12rpx;
}

.login-tip-text {
  font-size: 26rpx;
  color: var(--app-muted, #888888);
  line-height: 38rpx;
  margin-bottom: 24rpx;
}

.login-tip-btn {
  background: var(--app-primary, #2ebd85);
  color: #ffffff;
  border-radius: 12rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 30rpx;
}

.error-title {
  font-size: 30rpx;
  color: var(--app-danger, #d94a62);
  font-weight: 600;
  margin-bottom: 12rpx;
}

.error-text {
  font-size: 24rpx;
  color: var(--app-muted, #777777);
  line-height: 36rpx;
  margin-bottom: 20rpx;
  word-break: break-all;
}

.error-btn {
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary-dark, #226f63);
  border: 1rpx solid var(--app-border, #b9d7ce);
  border-radius: 12rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: var(--app-muted, #7b8798);
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
  background: var(--app-card-bg-alt, #fbfcfd);
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
  background: var(--app-danger, #d94a62);
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
  background: var(--app-card-bg-alt, #fbfcfd);
  border: 1rpx solid var(--app-border, #edf1f4);
  transition: transform 0.18s ease;
}

.account-accent {
  position: absolute;
  left: 0;
  top: 30rpx;
  bottom: 30rpx;
  width: 4rpx;
  border-radius: 0 999rpx 999rpx 0;
}

.account-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  width: 100%;
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
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  flex-shrink: 0;
  background: var(--app-soft-bg, #f2f6f4);
  border: 1rpx solid var(--app-border, #edf1f4);
  box-sizing: border-box;
}

.account-line-mark {
  position: relative;
  width: 40rpx;
  height: 40rpx;
  box-sizing: border-box;
}

.account-line-mark::before,
.account-line-mark::after,
.account-icon::before,
.account-icon::after {
  content: '';
  position: absolute;
  box-sizing: border-box;
}

.account-icon {
  position: relative;
}

.account-icon-bank .account-line-mark::before {
  left: 5rpx;
  top: 14rpx;
  width: 30rpx;
  height: 18rpx;
  border-left: 3rpx solid currentColor;
  border-right: 3rpx solid currentColor;
  box-shadow: inset 8rpx 0 0 -5rpx currentColor, inset -8rpx 0 0 -5rpx currentColor;
}

.account-icon-bank .account-line-mark::after {
  left: 2rpx;
  top: 5rpx;
  width: 36rpx;
  height: 11rpx;
  border-top: 3rpx solid currentColor;
  border-bottom: 3rpx solid currentColor;
  transform: perspective(30rpx) rotateX(28deg);
}

.account-icon-cash .account-line-mark {
  border: 3rpx solid currentColor;
  border-radius: 8rpx;
}

.account-icon-cash .account-line-mark::before {
  left: 50%;
  top: 50%;
  width: 13rpx;
  height: 13rpx;
  border: 3rpx solid currentColor;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.account-icon-wallet .account-line-mark {
  border: 3rpx solid currentColor;
  border-radius: 9rpx;
}

.account-icon-wallet .account-line-mark::before {
  right: -3rpx;
  top: 13rpx;
  width: 17rpx;
  height: 13rpx;
  border: 3rpx solid currentColor;
  border-right: 0;
  border-radius: 8rpx 0 0 8rpx;
  background: var(--app-soft-bg, #f2f6f4);
}

.account-icon-wallet .account-line-mark::after {
  right: 5rpx;
  top: 18rpx;
  width: 4rpx;
  height: 4rpx;
  border-radius: 50%;
  background: currentColor;
}

.account-icon-trend .account-line-mark::before {
  left: 5rpx;
  bottom: 7rpx;
  width: 30rpx;
  height: 24rpx;
  border-left: 3rpx solid currentColor;
  border-bottom: 3rpx solid currentColor;
}

.account-icon-trend .account-line-mark::after {
  left: 9rpx;
  top: 10rpx;
  width: 26rpx;
  height: 18rpx;
  border-top: 3rpx solid currentColor;
  border-right: 3rpx solid currentColor;
  transform: skew(-24deg) rotate(-10deg);
}

.account-icon-home .account-line-mark::before {
  left: 7rpx;
  top: 16rpx;
  width: 26rpx;
  height: 20rpx;
  border: 3rpx solid currentColor;
  border-top: 0;
  border-radius: 0 0 7rpx 7rpx;
}

.account-icon-home .account-line-mark::after {
  left: 8rpx;
  top: 5rpx;
  width: 24rpx;
  height: 24rpx;
  border-left: 3rpx solid currentColor;
  border-top: 3rpx solid currentColor;
  transform: rotate(45deg);
}

.account-icon-card .account-line-mark {
  border: 3rpx solid currentColor;
  border-radius: 8rpx;
}

.account-icon-card .account-line-mark::before {
  left: 0;
  right: 0;
  top: 11rpx;
  height: 3rpx;
  background: currentColor;
}

.account-icon-card .account-line-mark::after {
  right: 6rpx;
  bottom: 7rpx;
  width: 12rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: currentColor;
}

.account-icon-loan .account-line-mark::before,
.account-icon-bill .account-line-mark::before,
.account-icon-box .account-line-mark::before {
  inset: 4rpx;
  border: 3rpx solid currentColor;
  border-radius: 8rpx;
}

.account-icon-loan .account-line-mark::after {
  left: 9rpx;
  right: 9rpx;
  top: 12rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: currentColor;
  box-shadow: 0 8rpx 0 currentColor;
}

.account-icon-bill .account-line-mark::after {
  left: 12rpx;
  right: 9rpx;
  top: 12rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: currentColor;
  box-shadow: 0 8rpx 0 currentColor, 0 16rpx 0 currentColor;
}

.account-icon-box .account-line-mark::after {
  left: 8rpx;
  top: 12rpx;
  width: 24rpx;
  height: 17rpx;
  border-top: 3rpx solid currentColor;
  border-bottom: 3rpx solid currentColor;
}

.account-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-top: 2rpx;
}

.account-name {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
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
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  line-height: 32rpx;
  min-width: 0;
}

.meta-dot {
  color: var(--app-muted, #7b8798);
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
  color: var(--app-primary-dark, #226f63);
  font-size: 22rpx;
  line-height: 30rpx;
  text-align: right;
}

.account-balance {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--app-text, #17202a);
}

.account-balance.liability {
  color: var(--app-liability-color, #d94a62);
}

.add-account-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 24rpx 0 0;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 18rpx;
  background: var(--app-soft-bg, #eef8f4);
  color: var(--app-primary-dark, #226f63);
  border: 1rpx dashed var(--app-border, #a9d6ca);
  font-size: 28rpx;
  font-weight: 750;
}

.empty-state {
  padding: 60rpx 0;
  text-align: center;
}

.empty-text {
  color: var(--app-muted, #999999);
  font-size: 28rpx;
}
</style>
