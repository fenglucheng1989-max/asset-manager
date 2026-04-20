<template>
  <view class="container">
    <view v-if="isLoggedIn" class="profile-section">
      <view class="avatar-area">
        <view class="avatar">
          <text class="avatar-text">{{ username.substring(0, 1).toUpperCase() }}</text>
        </view>
        <text class="username">{{ username }}</text>
        <text class="profile-subtitle">当前账号已登录，资产数据可同步使用</text>
      </view>

      <view class="summary-card">
        <view class="summary-row">
          <text class="summary-label">当前账号</text>
          <text class="summary-value">{{ username }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">登录状态</text>
          <text class="summary-value active">已登录</text>
        </view>
      </view>

      <view class="menu-list">
        <view class="menu-item" @click="handleLogout">
          <text class="menu-text">退出登录</text>
          <text class="menu-arrow">></text>
        </view>
      </view>
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
      }
    }
  },
  computed: {
    isLoggedIn() {
      const userStore = useUserStore()
      return userStore.isLoggedIn
    },
    username() {
      const userStore = useUserStore()
      return userStore.username
    }
  },
  methods: {
    async handleSubmit() {
      if (!this.loginForm.username.trim() || !this.loginForm.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }

      const userStore = useUserStore()
      const res = this.isRegister
        ? await userStore.register(this.loginForm.username, this.loginForm.password, this.loginForm.email)
        : await userStore.login(this.loginForm.username, this.loginForm.password)

      if (res && res.code === 200) {
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

          const userStore = useUserStore()
          userStore.logout()
          uni.showToast({ title: '已退出', icon: 'success' })
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20rpx 20rpx calc(140rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.profile-section {
  padding-top: 56rpx;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 42rpx;
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 64rpx;
  background: linear-gradient(135deg, #2EBD85, #16a085);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.avatar-text {
  color: #ffffff;
  font-size: 48rpx;
  font-weight: bold;
}

.username {
  font-size: 36rpx;
  font-weight: 600;
  color: #333333;
  margin-bottom: 10rpx;
}

.profile-subtitle {
  font-size: 26rpx;
  color: #888888;
  text-align: center;
  line-height: 38rpx;
}

.menu-list,
.login-card,
.summary-card {
  background: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
}

.summary-card {
  padding: 8rpx 30rpx;
  margin-bottom: 24rpx;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 76rpx;
  border-bottom: 1rpx solid #f3f3f3;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  color: #777777;
  font-size: 28rpx;
}

.summary-value {
  color: #333333;
  font-size: 28rpx;
  font-weight: 500;
}

.summary-value.active {
  color: #2EBD85;
}

.menu-list {
  padding: 0 30rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 0;
}

.menu-text {
  font-size: 30rpx;
  color: #FF6B6B;
}

.menu-arrow {
  color: #cccccc;
  font-size: 28rpx;
}

.login-section {
  padding-top: 100rpx;
}

.login-card {
  padding: 60rpx 40rpx;
}

.login-title {
  font-size: 44rpx;
  font-weight: bold;
  color: #333333;
  display: block;
  text-align: center;
  margin-bottom: 60rpx;
}

.input-group {
  margin-bottom: 40rpx;
}

.login-input {
  background: #f5f5f5;
  border-radius: 16rpx;
  height: 88rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
  margin-bottom: 24rpx;
}

.login-btn {
  background: #2EBD85;
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
  color: #2EBD85;
  font-size: 28rpx;
}
</style>
