<template>
  <view class="container">
    <view class="section-group">
      <view class="section-item" @click="goTheme">
        <view class="theme-mini-preview" :style="{ background: currentTheme.heroGradient }">
          <view class="mini-bar" :style="{ background: currentTheme.accent }"></view>
          <view class="mini-bar short" :style="{ background: currentTheme.accent, opacity: 0.5 }"></view>
        </view>
        <view class="item-copy">
          <text class="item-title">视觉装扮</text>
          <text class="item-desc">{{ currentPreset.label }}</text>
        </view>
        <text class="item-arrow">›</text>
      </view>

      <view class="section-item">
        <view class="item-copy">
          <text class="item-title">默认货币</text>
        </view>
        <picker :range="currencyOptions" range-key="label" :value="currencyIndex" @change="handleCurrencyChange">
          <view class="inline-picker">
            <text>{{ baseCurrency }}</text>
          </view>
        </picker>
        <text class="item-arrow">›</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item" @click="goDataManage">
        <view class="item-copy">
          <text class="item-title">数据管理</text>
          <text class="item-desc">导出、备份、恢复与清空数据</text>
        </view>
        <text class="item-arrow">›</text>
      </view>
      <view class="section-item" @click="goAccountSettings">
        <view class="item-copy">
          <text class="item-title">账号设置</text>
          <text class="item-desc">修改个人信息、密码和注销账号</text>
        </view>
        <text class="item-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'
import { getThemeMode, getThemeOptions, getResolvedTheme } from '../../utils/theme'

export default {
  data() {
    return {
      baseCurrency: 'CNY',
      themeMode: 'warm-gold',
      themeOptions: getThemeOptions(),
      currencyOptions: [
        { label: '人民币 CNY', value: 'CNY' },
        { label: '美元 USD', value: 'USD' },
        { label: '港币 HKD', value: 'HKD' },
        { label: '欧元 EUR', value: 'EUR' },
        { label: '日元 JPY', value: 'JPY' },
        { label: '英镑 GBP', value: 'GBP' }
      ]
    }
  },
  computed: {
    currencyIndex() {
      const index = this.currencyOptions.findIndex(item => item.value === this.baseCurrency)
      return index < 0 ? 0 : index
    },
    currentTheme() {
      return getResolvedTheme(this.themeMode)
    },
    currentPreset() {
      return this.themeOptions.find(item => item.value === this.themeMode) || this.themeOptions[0]
    }
  },
  onShow() {
    this.themeMode = getThemeMode()
    this.refreshProfile()
  },
  methods: {
    refreshProfile() {
      const profile = uni.getStorageSync('userProfile') || null
      this.baseCurrency = profile && profile.baseCurrency ? profile.baseCurrency : 'CNY'
      try {
        const userStore = useUserStore()
        this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : this.baseCurrency
        userStore.fetchProfile().then(() => {
          this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : 'CNY'
        })
      } catch (error) {}
    },
    async handleCurrencyChange(event) {
      const index = Number(event.detail.value)
      const nextCurrency = this.currencyOptions[index].value
      try {
        const userStore = useUserStore()
        const res = await userStore.updateBaseCurrency(nextCurrency)
        if (res && res.code === 200) {
          this.baseCurrency = nextCurrency
          uni.showToast({ title: '已更新', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : '更新失败'
        uni.showToast({ title: message, icon: 'none' })
      }
    },
    goTheme() { uni.navigateTo({ url: '/pages/mine/theme' }) },
    goDataManage() { uni.navigateTo({ url: '/pages/mine/data' }) },
    goAccountSettings() { uni.navigateTo({ url: '/pages/mine/account' }) }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.section-group {
  margin-bottom: 20rpx;
  overflow: hidden;
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26,42,58,0.045));
}

.section-item {
  min-height: 96rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.section-item:last-child { border-bottom: none; }

.item-copy {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: block;
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
}

.item-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  line-height: 32rpx;
  color: var(--app-muted, #94a3b8);
}

.item-arrow {
  color: var(--app-muted, #64748b);
  font-size: 32rpx;
  flex-shrink: 0;
}

.inline-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: var(--app-primary-dark, #226f63);
  font-size: 26rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.theme-mini-preview {
  width: 68rpx;
  height: 68rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 5rpx;
  padding-bottom: 12rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.mini-bar {
  width: 9rpx;
  border-radius: 999rpx;
  height: 24rpx;
}

.mini-bar.short { height: 15rpx; }
</style>
