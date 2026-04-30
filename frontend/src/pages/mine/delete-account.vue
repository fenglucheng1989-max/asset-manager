<template>
  <view class="container" :style="themeVars">
    <view class="page-hero">
      <text class="page-title">注销账号</text>
      <text class="page-sub">此操作不可恢复，请谨慎决定</text>
    </view>

    <view class="form-card danger-card">
      <view class="warning-box">
        <text class="warning-title">⚠️ 注销后将永久删除以下数据：</text>
        <text class="warning-item">· 账号与登录信息</text>
        <text class="warning-item">· 所有资产账户与余额历史</text>
        <text class="warning-item">· 所有交易记录与预算</text>
        <text class="warning-item">· 所有快照数据</text>
      </view>

      <view class="form-item">
        <text class="form-label">当前密码</text>
        <input
          class="form-input"
          v-model="password"
          type="password"
          placeholder="输入密码以确认"
        />
      </view>

      <button class="delete-btn" :disabled="!password" @click="handleDelete">
        确认注销
      </button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      password: '',
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
  },
  methods: {
    handleDelete() {
      if (!this.password) {
        uni.showToast({ title: '请输入当前密码', icon: 'none' })
        return
      }
      uni.showModal({
        title: '最后确认',
        content: '账号和所有业务数据将被永久删除，操作不可恢复。',
        confirmText: '确认注销',
        confirmColor: '#d94a62',
        success: async (result) => {
          if (!result.confirm) return
          try {
            const userStore = useUserStore()
            const res = await userStore.deleteAccount(this.password)
            if (res && res.code === 200) {
              userStore.logout()
              uni.showToast({ title: '账号已注销', icon: 'success' })
              setTimeout(() => uni.switchTab({ url: '/pages/mine/mine' }), 800)
            }
          } catch (error) {
            const message = error && error.message ? error.message : '注销失败'
            uni.showToast({ title: message, icon: 'none' })
          }
        }
      })
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
  padding: 20rpx 4rpx 28rpx;
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

.form-card {
  background: var(--app-card-bg, #ffffff);
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.danger-card {
  border-color: var(--app-danger-border, #fecaca);
}

.warning-box {
  padding: 24rpx;
  border-radius: 14rpx;
  background: var(--app-danger-bg, #fef2f2);
  margin-bottom: 28rpx;
}

.warning-title {
  display: block;
  font-size: 26rpx;
  font-weight: 800;
  color: var(--app-danger, #d94a62);
  margin-bottom: 10rpx;
}

.warning-item {
  display: block;
  font-size: 24rpx;
  line-height: 38rpx;
  color: var(--app-muted, #7b8798);
}

.form-item {
  display: flex;
  align-items: center;
  min-height: 90rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  gap: 22rpx;
  margin-bottom: 28rpx;
}

.form-label {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  flex-shrink: 0;
  font-weight: 650;
}

.form-input {
  flex: 1;
  min-width: 0;
  height: 56rpx;
  padding: 0;
  border: none;
  background: transparent;
  text-align: right;
  font-size: 30rpx;
  color: var(--app-text, #17202a);
}

.delete-btn {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #ffffff;
  background: var(--app-danger, #d94a62);
}

.delete-btn[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
}
</style>
