<template>
  <view class="tab-bar">
    <view
      v-for="item in tabs"
      :key="item.path"
      :class="['tab-item', { active: current === item.path, primary: item.primary }]"
      @click="switchTab(item)"
    >
      <view v-if="item.primary" class="tab-primary">
        <view class="tab-fab">
          <view class="fab-plus"></view>
        </view>
        <text class="tab-text">记账</text>
      </view>
      <template v-else>
        <view :class="['tab-icon', `icon-${item.icon}`]"></view>
        <text class="tab-text">{{ item.text }}</text>
      </template>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      current: '',
      tabs: [
        { path: '/pages/index/index', text: '资产', icon: 'wallet' },
        { path: '/pages/transaction/list', text: '账本', icon: 'receipt' },
        { path: '/pages/transaction/form', text: '记账', primary: true },
        { path: '/pages/transaction/budget', text: '预算', icon: 'target' },
        { path: '/pages/mine/mine', text: '我的', icon: 'user' }
      ]
    }
  },
  created() {
    this.updateCurrent()
  },
  mounted() {
    this.updateCurrent()
  },
  methods: {
    updateCurrent() {
      const pages = getCurrentPages()
      if (pages.length) {
        const page = pages[pages.length - 1]
        if (page) this.current = '/' + page.route
      }
    },
    switchTab(item) {
      if (item.primary) {
        uni.navigateTo({ url: item.path })
        return
      }
      if (this.current === item.path) return
      uni.switchTab({ url: item.path })
    }
  }
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 56px;
  padding-bottom: env(safe-area-inset-bottom);
  display: flex;
  align-items: stretch;
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 -8rpx 28rpx rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(18px);
  z-index: 999;
  box-sizing: content-box;
}

.tab-item {
  flex: 1;
  min-width: 0;
  height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.tab-item::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 6px;
  width: 46px;
  height: 26px;
  border-radius: 999px;
  background: transparent;
  transform: translateX(-50%) scale(0.92);
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
  pointer-events: none;
}

.tab-item::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 6px;
  width: 14px;
  height: 3px;
  border-radius: 999px;
  background: transparent;
  transform: translateX(-50%) scaleX(0.6);
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
  pointer-events: none;
}

.tab-item.primary::before,
.tab-item.primary::after {
  display: none;
}

.tab-primary {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ---- FAB ---- */
.tab-fab {
  width: 48px;
  height: 48px;
  margin-top: -18px;
  border-radius: 50%;
  background: var(--app-tabbar-bg, #ffffff);
  color: var(--app-tabbar-selected, var(--app-primary, #d3a414));
  border: 2px solid var(--app-tabbar-selected, var(--app-primary, #d3a414));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 0 6px rgba(255, 255, 255, 0.94),
    var(--app-tabbar-selected-shadow, 0 10rpx 28rpx rgba(15, 23, 42, 0.14));
}

.fab-plus {
  position: relative;
  width: 22px;
  height: 22px;
}

.fab-plus::before,
.fab-plus::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 999px;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.fab-plus::before {
  width: 22px;
  height: 2px;
}

.fab-plus::after {
  width: 2px;
  height: 22px;
}

.tab-primary .tab-text {
  margin-top: 2px;
  font-size: 11px;
  line-height: 15px;
  font-weight: 650;
  color: var(--app-tabbar-text, #9aa8b8);
}

/* ---- Normal tabs ---- */
.tab-icon {
  width: 22px;
  height: 22px;
  color: var(--app-tabbar-text, #9aa8b8);
  position: relative;
  z-index: 1;
  transition: color 0.2s, transform 0.2s;
  box-sizing: border-box;
}

.tab-item.active .tab-icon {
  color: var(--app-tabbar-selected, var(--app-primary, #d3a414));
  transform: scale(1.12);
}

.tab-icon::before,
.tab-icon::after {
  content: '';
  position: absolute;
  box-sizing: border-box;
}

.icon-wallet {
  border: 2px solid currentColor;
  border-radius: 6px;
  height: 18px;
  margin-top: 2px;
}

.icon-wallet::before {
  right: -2px;
  top: 5px;
  width: 9px;
  height: 8px;
  border: 2px solid currentColor;
  border-right: 0;
  border-radius: 5px 0 0 5px;
  background: var(--app-tabbar-bg, #ffffff);
}

.icon-wallet::after {
  right: 3px;
  top: 8px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}

.icon-receipt {
  border: 0;
}

.icon-receipt::before {
  left: 3px;
  right: 3px;
  top: 4px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 7px 0 currentColor, 0 14px 0 currentColor;
}

.icon-receipt::after {
  left: 0;
  top: 0;
  width: 22px;
  height: 22px;
  border: 2px solid currentColor;
  border-radius: 6px;
}

.icon-target {
  border: 2px solid currentColor;
  border-radius: 50%;
}

.icon-target::before {
  inset: 5px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.icon-target::after {
  left: 50%;
  top: 50%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.icon-user::before {
  left: 7px;
  top: 2px;
  width: 8px;
  height: 8px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.icon-user::after {
  left: 3px;
  bottom: 2px;
  width: 16px;
  height: 9px;
  border: 2px solid currentColor;
  border-radius: 10px 10px 4px 4px;
}

.tab-text {
  margin-top: 2px;
  font-size: 11px;
  line-height: 15px;
  font-weight: 650;
  color: var(--app-tabbar-text, #9aa8b8);
  position: relative;
  z-index: 1;
  transition: color 0.2s, font-weight 0.2s;
}

.tab-item.active .tab-text {
  color: var(--app-tabbar-selected, var(--app-primary, #d3a414));
  font-weight: 750;
}

.tab-item.active::before {
  background: var(--app-tabbar-selected-bg, rgba(211, 164, 20, 0.12));
  opacity: 1;
  transform: translateX(-50%) scale(1);
}

.tab-item.active::after {
  background: var(--app-tabbar-selected, var(--app-primary, #d3a414));
  opacity: 1;
  transform: translateX(-50%) scaleX(1);
}

@media (max-width: 480px) {
  .tab-bar {
    left: 0;
    max-width: none;
    transform: none;
  }
}
</style>
