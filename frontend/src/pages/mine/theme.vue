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
            <view class="gradient-bar" :style="{ background: themeAccents[item.value] }"></view>
          </view>
          <view class="card-info">
            <text class="card-label">{{ item.label }}<text v-if="item.value === 'mist-mono'" class="default-tag">默认</text></text>
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
  'mist-mono':   'linear-gradient(180deg, #e8ecf1 0%, #f0f3f7 50%, #f8f9fb 100%)',
  'mint-fresh':  'linear-gradient(180deg, #c8dfd0 0%, #d8e8dc 44%, #eaf2ec 100%)',
  'sakura-blossom': 'linear-gradient(180deg, #fce4ec 0%, #f8d5e0 44%, #fef9fa 100%)',
  'iris-dusk':   'linear-gradient(180deg, #e0d8f0 0%, #ece4f5 48%, #f6f2fb 100%)',
  'warm-gold':   'linear-gradient(180deg, #ffe066 0%, #fff7d1 100%)',
  'lucky-red':   'linear-gradient(180deg, #c9383b 0%, #d9544a 56%, #fff0e8 100%)',
  'river-snow':  'linear-gradient(180deg, #1a212b 0%, #2d3848 50%, #c8d2de 100%)',
  'town-night':  'linear-gradient(180deg, #1a2340 0%, #2a3a68 54%, #e8ecf4 100%)',
  'deep-ocean':  'linear-gradient(180deg, #0a1625 0%, #132238 50%, #1e3048 100%)',
  'tycoon-gold': 'linear-gradient(180deg, #1a1814 0%, #2a2318 56%, #3d3528 100%)'
}

const ACCENTS = {
  'mist-mono': '#5b7a9a', 'mint-fresh': '#7fa998', 'sakura-blossom': '#d4788f',
  'iris-dusk': '#7c6f9e', 'warm-gold': '#d3a414', 'lucky-red': '#ffd4a8',
  'river-snow': '#5a7a90', 'town-night': '#ffd166', 'deep-ocean': '#4a90d9',
  'tycoon-gold': '#c8a44e'
}

export default {
  data() {
    return {
      themeMode: 'mist-mono',
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
  background: var(--app-page-bg, #f8f9fb);
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
  border-color: var(--app-primary, #d3a414);
  box-shadow: var(--app-shadow-lg, 0 8rpx 28rpx rgba(26,42,58,0.08));
}

.card-gradient {
  height: 100rpx;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 0 24rpx 18rpx;
}

.gradient-bar {
  width: 72rpx;
  height: 12rpx;
  border-radius: 999rpx;
}

.card-info {
  padding: 18rpx 22rpx 20rpx;
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

.default-tag {
  display: inline;
  margin-left: 10rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  background: var(--app-soft-bg, #f6f8fb);
  color: var(--app-muted, #94a3b8);
  font-size: 20rpx;
  font-weight: 600;
  vertical-align: middle;
}

.check-dot {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--app-primary, #d3a414);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3rpx 10rpx rgba(211,164,20,0.3);
}

.check-icon {
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 900;
}
</style>
