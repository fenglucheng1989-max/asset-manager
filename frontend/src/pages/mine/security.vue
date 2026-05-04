<template>
  <view class="container" :style="themeVars">
    <view class="page-hero">
      <text class="page-title">安全账户</text>
      <text class="page-sub">修改登录密码</text>
    </view>

    <view class="form-card">
      <view class="input-row">
        <input class="input" v-model="passwordForm.currentPassword" type="password" placeholder="当前密码" />
      </view>
      <view class="input-row">
        <input class="input" v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
      </view>
      <view class="input-row">
        <input class="input" v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
      </view>
      <text class="form-hint">建议使用 6-50 位且不与当前密码相同的新密码</text>
    </view>

    <view class="btn-group">
      <button class="primary-btn" @click="changePassword">保存新密码</button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
  },
  methods: {
    async changePassword() {
      if (!this.passwordForm.currentPassword || !this.passwordForm.newPassword) {
        uni.showToast({ title: '请填写密码', icon: 'none' })
        return
      }
      if (this.passwordForm.newPassword.length < 6 || this.passwordForm.newPassword.length > 50) {
        uni.showToast({ title: '新密码长度应为 6-50 位', icon: 'none' })
        return
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        uni.showToast({ title: '两次新密码不一致', icon: 'none' })
        return
      }
      try {
        const res = await useUserStore().changePassword(this.passwordForm.currentPassword, this.passwordForm.newPassword)
        if (res && res.code === 200) {
          this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' }
          uni.showToast({ title: '密码已更新', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : '密码更新失败'
        uni.showToast({ title: message, icon: 'none' })
      }
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
  padding: 28rpx;
  margin-bottom: 18rpx;
  background: var(--app-card-bg, #ffffff);
  border-radius: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.form-hint {
  display: block;
  margin-bottom: 20rpx;
  color: var(--app-muted, #94a3b8);
  font-size: 22rpx;
  line-height: 32rpx;
}

.input-row {
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.input-row:last-of-type {
  border-bottom: none;
}

.input {
  width: 100%;
  height: 96rpx;
  padding: 0;
  font-size: 28rpx;
}

.btn-group {
  padding: 6rpx 4rpx 0;
}

.primary-btn {
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  font-size: 30rpx;
  color: #ffffff;
  background: var(--app-primary, #d3a414);
}

.primary-btn[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
}
</style>
