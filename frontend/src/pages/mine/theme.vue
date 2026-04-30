<template>
  <view class="container" :style="themeVars">
    <view class="page-hero">
      <text class="page-title">视觉装扮</text>
      <text class="page-sub">选一个风格，一键切换整套配色</text>
    </view>

    <view class="card-grid">
      <view
        v-for="item in themeOptions"
        :key="item.value"
        class="theme-card"
        :class="{ active: themeMode === item.value }"
        @click="selectTheme(item.value)"
      >
        <view class="card-surface">
          <view class="card-gradient" :style="{ background: themeGradients[item.value] }">
            <view class="gradient-shape s1" :style="{ background: themeAccents[item.value] }"></view>
            <view class="gradient-shape s2" :style="{ background: themeAccents[item.value], opacity: 0.4 }"></view>
            <view class="gradient-shape s3 circle" :style="{ background: themeAccents[item.value] }"></view>
          </view>
          <view class="card-info">
            <text class="card-label">{{ item.label }}</text>
            <text class="card-desc">{{ item.subtitle }}</text>
          </view>
          <view v-if="themeMode === item.value" class="check-dot">
            <text class="check-icon">✓</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getThemeMode, getThemeOptions, getThemeVars, saveThemeMode } from '../../utils/theme'

const GRADIENTS = {
  'warm-gold':  'linear-gradient(180deg, #ffe066 0%, #fff7d1 100%)',
  'town-night': 'linear-gradient(180deg, #1a2340 0%, #2a3a68 54%, #e8ecf4 100%)',
  'lucky-red':  'linear-gradient(180deg, #c9383b 0%, #d9544a 56%, #fff0e8 100%)'
}

const ACCENTS = {
  'warm-gold': '#d3a414', 'town-night': '#ffd166', 'lucky-red': '#ffd4a8'
}

export default {
  data() {
    return {
      themeMode: 'warm-gold',
      themeOptions: getThemeOptions(),
      themeGradients: GRADIENTS,
      themeAccents: ACCENTS,
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeMode = getThemeMode()
    this.themeVars = getThemeVars(this.themeMode)
  },
  methods: {
    selectTheme(mode) {
      if (this.themeMode === mode) return
      this.themeMode = mode
      saveThemeMode(mode)
      this.themeVars = getThemeVars(mode)
      uni.showToast({ title: '装扮已切换', icon: 'success' })
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.page-hero {
  padding: 20rpx 0 28rpx;
}

.page-title {
  display: block;
  font-size: 40rpx;
  line-height: 52rpx;
  font-weight: 900;
  color: var(--app-text, #17202a);
}

.page-sub {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #94a3b8);
  font-size: 25rpx;
  line-height: 34rpx;
}

.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.theme-card {
  width: calc(50% - 9rpx);
}

.theme-card:active {
  transform: scale(0.97);
}

.card-surface {
  position: relative;
  border-radius: 20rpx;
  overflow: hidden;
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 4rpx 18rpx rgba(26,42,58,0.05));
  border: 2rpx solid transparent;
  transition: all 0.22s ease;
}

.theme-card.active .card-surface {
  border-color: var(--app-primary, #2ebd85);
  box-shadow: var(--app-shadow-lg, 0 8rpx 28rpx rgba(26,42,58,0.08));
}

.card-gradient {
  height: 160rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gradient-shape {
  position: absolute;
  border-radius: 999rpx;
}

.gradient-shape.s1 {
  width: 60rpx;
  height: 8rpx;
  bottom: 32rpx;
  left: 24rpx;
}

.gradient-shape.s2 {
  width: 80rpx;
  height: 8rpx;
  bottom: 22rpx;
  left: 24rpx;
}

.gradient-shape.s3.circle {
  width: 48rpx;
  height: 48rpx;
  right: 28rpx;
  bottom: 22rpx;
}

.card-info {
  padding: 22rpx 22rpx 24rpx;
}

.card-label {
  display: block;
  font-size: 28rpx;
  line-height: 36rpx;
  font-weight: 800;
  color: var(--app-text, #17202a);
}

.card-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  line-height: 30rpx;
  color: var(--app-muted, #94a3b8);
}

.check-dot {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--app-primary, #2ebd85);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3rpx 10rpx rgba(46,189,133,0.3);
}

.check-icon {
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 900;
}
</style>
