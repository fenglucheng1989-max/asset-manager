<template>
  <view class="container">
    <view class="security-hero">
      <text class="hero-title">安全账户</text>
      <text class="hero-subtitle">管理登录密码和账号注销。注销会删除账号及所有业务数据，请谨慎操作。</text>
    </view>

    <view class="form-card">
      <view class="section-head">
        <text class="section-title">修改密码</text>
        <text class="section-subtitle">建议使用 6-50 位且不与当前密码相同的新密码。</text>
      </view>
      <input class="input" v-model="passwordForm.currentPassword" type="password" placeholder="当前密码" />
      <input class="input" v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
      <input class="input" v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
      <button class="primary-btn" @click="changePassword">保存新密码</button>
    </view>

    <view class="form-card danger-card">
      <view class="section-head">
        <text class="section-title danger-text">注销账号</text>
        <text class="section-subtitle">注销后账号、资产、账本、预算和快照都会被删除。</text>
      </view>
      <input class="input" v-model="deletePassword" type="password" placeholder="输入当前密码确认注销" />
      <button class="danger-btn" @click="confirmDeleteAccount">注销账号</button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'

export default {
  data() {
    return {
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      deletePassword: ''
    }
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
    },
    confirmDeleteAccount() {
      if (!this.deletePassword) {
        uni.showToast({ title: '请输入当前密码', icon: 'none' })
        return
      }
      uni.showModal({
        title: '注销账号',
        content: '账号和所有数据将被删除，操作不可恢复。',
        confirmText: '注销',
        confirmColor: '#d94a62',
        success: async (result) => {
          if (!result.confirm) return
          try {
            const userStore = useUserStore()
            const res = await userStore.deleteAccount(this.deletePassword)
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
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.security-hero,
.form-card {
  padding: 28rpx;
  margin-bottom: 18rpx;
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.security-hero {
  background: var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%));
  color: var(--app-hero-text, #ffffff);
}

.hero-title {
  display: block;
  font-size: 40rpx;
  line-height: 52rpx;
  font-weight: 850;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  color: var(--app-hero-sub, rgba(255, 255, 255, 0.72));
  font-size: 25rpx;
  line-height: 38rpx;
}

.section-head {
  margin-bottom: 22rpx;
}

.section-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 32rpx;
  line-height: 42rpx;
  font-weight: 850;
}

.section-subtitle {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.input {
  height: 86rpx;
  padding: 0 26rpx;
  margin-bottom: 20rpx;
  border-radius: 14rpx;
  background: var(--app-input-bg, #f6f8fb);
  border: 1rpx solid var(--app-border, #edf1f4);
  font-size: 28rpx;
}

.primary-btn,
.danger-btn {
  margin: 8rpx 0 0;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #ffffff;
}

.primary-btn {
  background: var(--app-primary, #2ebd85);
}

.danger-btn {
  background: var(--app-danger, #d94a62);
}

.danger-card {
  border-color: #f0c3ca;
}

.danger-text {
  color: var(--app-danger, #d94a62);
}
</style>
