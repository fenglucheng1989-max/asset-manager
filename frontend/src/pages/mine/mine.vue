<template>
  <view class="container">
    <view v-if="isLoggedIn" class="profile-section">
      <view class="profile-header">
        <view class="avatar">
          <text class="avatar-text">{{ username.substring(0, 1).toUpperCase() }}</text>
        </view>
        <view class="profile-copy">
          <text class="username">{{ username }}</text>
          <text class="profile-subtitle">当前账号已登录，资产数据可同步使用</text>
        </view>
      </view>

      <view class="profile-meta">
        <text class="meta-label">登录状态</text>
        <text class="meta-value">已登录</text>
      </view>

      <view class="profile-meta currency-meta">
        <text class="meta-label">默认本位币</text>
        <picker :range="currencyOptions" range-key="label" :value="currencyIndex" @change="handleCurrencyChange">
          <view class="currency-picker">
            <text>{{ currencyOptions[currencyIndex].label }}</text>
            <text class="picker-arrow">></text>
          </view>
        </picker>
      </view>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

    <view v-else class="login-section">
      <view class="login-card">
        <text class="login-title">{{ isRegister ? '注册' : '登录' }}</text>

        <view class="input-group">
          <input class="login-input" v-model="loginForm.username" placeholder="用户名" />
          <input class="login-input" v-model="loginForm.password" type="password" placeholder="密码" />
          <input
            v-if="isRegister"
            class="login-input"
            v-model="loginForm.email"
            placeholder="邮箱（可选）"
          />
        </view>

        <button class="login-btn" @click="handleSubmit">
          {{ isRegister ? '注册' : '登录' }}
        </button>

        <view class="switch-mode" @click="isRegister = !isRegister">
          <text class="switch-text">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'

export default {
  data() {
    return {
      isRegister: false,
      loginForm: {
        username: '',
        password: '',
        email: ''
      },
      isLoggedIn: false,
      username: 'preview',
      baseCurrency: 'CNY',
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
    }
  },
  onShow() {
    this.refreshUser()
  },
  methods: {
    refreshUser() {
      const token = uni.getStorageSync('token') || ''
      const username = uni.getStorageSync('username') || 'preview'
      const profile = uni.getStorageSync('userProfile') || null

      this.isLoggedIn = !!token
      this.username = username
      this.baseCurrency = profile && profile.baseCurrency ? profile.baseCurrency : 'CNY'

      try {
        const userStore = useUserStore()
        if (userStore) {
          this.isLoggedIn = userStore.isLoggedIn
          this.username = userStore.username || username
          this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : this.baseCurrency
          if (this.isLoggedIn) {
            userStore.fetchProfile().then(() => {
              this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : 'CNY'
            })
          }
        }
      } catch (error) {
        console.error('Read user store failed:', error)
      }
    },
    async handleCurrencyChange(event) {
      const index = Number(event.detail.value)
      const nextCurrency = this.currencyOptions[index].value
      try {
        const userStore = useUserStore()
        const res = await userStore.updateBaseCurrency(nextCurrency)
        if (res && res.code === 200) {
          this.baseCurrency = nextCurrency
          uni.showToast({ title: '已更新默认币种', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : '币种更新失败'
        uni.showToast({ title: message, icon: 'none' })
      }
    },
    async handleSubmit() {
      if (!this.loginForm.username.trim() || !this.loginForm.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }

      let res
      try {
        const userStore = useUserStore()
        res = this.isRegister
          ? await userStore.register(this.loginForm.username, this.loginForm.password, this.loginForm.email)
          : await userStore.login(this.loginForm.username, this.loginForm.password)
      } catch (error) {
        const message = error && error.message ? error.message : '登录失败'
        uni.showToast({ title: message, icon: 'none' })
        return
      }

      if (res && res.code === 200) {
        this.refreshUser()
        uni.showToast({ title: this.isRegister ? '注册成功' : '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      }
    },
    handleLogout() {
      uni.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        success: (result) => {
          if (!result.confirm) return

          try {
            const userStore = useUserStore()
            userStore.logout()
          } catch (error) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('username')
          }
          this.refreshUser()
          uni.showToast({ title: '已退出', icon: 'success' })
        }
      })
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

.profile-section {
  padding-top: 24rpx;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 34rpx;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #14202d, #226f63);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 800;
}

.profile-copy {
  min-width: 0;
  flex: 1;
}

.username {
  display: block;
  font-size: 36rpx;
  line-height: 48rpx;
  font-weight: 700;
  color: #17202a;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-subtitle {
  font-size: 26rpx;
  color: #64748b;
  line-height: 38rpx;
}

.login-card {
  background: #ffffff;
  border-radius: 16rpx;
  border: 1rpx solid #edf1f4;
  box-shadow: 0 12rpx 30rpx rgba(26, 42, 58, 0.06);
}

.profile-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 26rpx 0;
  border-top: 1rpx solid #e6edf2;
  border-bottom: 1rpx solid #e6edf2;
}

.currency-meta {
  border-top: none;
  margin-bottom: 32rpx;
}

.meta-label {
  color: #64748b;
  font-size: 28rpx;
}

.meta-value {
  color: #226f63;
  font-size: 28rpx;
  font-weight: 650;
}

.currency-picker {
  display: flex;
  align-items: center;
  color: #226f63;
  font-size: 28rpx;
  font-weight: 650;
}

.picker-arrow {
  margin-left: 10rpx;
  color: #94a3b8;
}

.logout-btn {
  margin: 0;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 14rpx;
  background: #ffffff;
  font-size: 30rpx;
  color: #d94a62;
  border: 1rpx solid #f0c3ca;
}

.login-section {
  padding-top: 24rpx;
}

.login-card {
  padding: 42rpx 34rpx;
}

.login-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #17202a;
  display: block;
  margin-bottom: 34rpx;
}

.input-group {
  margin-bottom: 40rpx;
}

.login-input {
  background: #f6f8fb;
  border-radius: 14rpx;
  height: 88rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #edf1f4;
}

.login-btn {
  background: linear-gradient(135deg, #2ebd85, #239a88);
  color: #ffffff;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  margin-bottom: 30rpx;
}

.switch-mode {
  text-align: center;
}

.switch-text {
  color: #226f63;
  font-size: 28rpx;
}
</style>
