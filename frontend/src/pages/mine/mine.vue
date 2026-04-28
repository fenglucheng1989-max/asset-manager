<template>
  <view class="container">
    <view v-if="isLoggedIn" class="profile-section">
      <view class="profile-card">
        <view class="profile-header">
          <view class="avatar">
            <text class="avatar-text">{{ username.substring(0, 1).toUpperCase() }}</text>
          </view>
          <view class="profile-copy">
            <text class="username">{{ username }}</text>
            <text class="profile-email">{{ profileEmail }}</text>
            <text class="profile-subtitle">注册于 {{ registeredDate }}</text>
          </view>
        </view>
      </view>

      <view class="net-card" @click="goHome">
        <view>
          <text class="net-label">净资产</text>
          <text class="net-value">{{ formatMoney(overview.netWorth) }}</text>
        </view>
        <text class="net-arrow">›</text>
      </view>

      <view class="menu-group">
        <view class="menu-item" @click="goTrend">
          <text class="menu-title">资产趋势</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goHome">
          <text class="menu-title">账户管理</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goTrend">
          <view class="menu-copy">
            <text class="menu-title">快照管理</text>
            <text class="menu-subtitle">查看历史快照或记录今日资产</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item disabled" @click="showComingSoon('主题设置')">
          <text class="menu-title">主题设置</text>
          <text class="menu-badge">预留</text>
        </view>
        <view class="menu-item">
          <text class="menu-title">默认货币</text>
          <picker :range="currencyOptions" range-key="label" :value="currencyIndex" @change="handleCurrencyChange">
            <view class="inline-picker">
              <text>{{ baseCurrency }}</text>
              <text class="menu-arrow">›</text>
            </view>
          </picker>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @click="showComingSoon('导出数据')">
          <text class="menu-title">导出数据</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item disabled" @click="showComingSoon('备份与同步')">
          <text class="menu-title">备份与同步</text>
          <text class="menu-badge">预留</text>
        </view>
        <view class="menu-item danger" @click="confirmClearData">
          <text class="menu-title">清空所有数据</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @click="showComingSoon('修改密码')">
          <text class="menu-title">修改密码</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item disabled" @click="showComingSoon('生物识别解锁')">
          <text class="menu-title">生物识别解锁</text>
          <switch disabled :checked="false" color="#2EBD85" />
        </view>
        <view class="menu-item danger" @click="showComingSoon('注销账号')">
          <text class="menu-title">注销账号</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="about-card">
        <text class="about-title">关于资产管家 v1.0</text>
        <view class="about-links">
          <text @click="showComingSoon('用户协议')">用户协议</text>
          <text class="about-divider">|</text>
          <text @click="showComingSoon('隐私政策')">隐私政策</text>
        </view>
        <text class="feedback" @click="showComingSoon('意见反馈')">意见反馈</text>
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
import { useAssetStore } from '../../store/asset'
import { useUserStore } from '../../store/user'
import { formatMoney } from '../../utils/money'

const DEFAULT_OVERVIEW = {
  totalAsset: 0,
  totalLiability: 0,
  netWorth: 0,
  accountCount: 0,
  lastUpdateTime: null
}

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
      profile: null,
      overview: { ...DEFAULT_OVERVIEW },
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
    },
    profileEmail() {
      return this.profile && this.profile.email ? this.profile.email : '未设置邮箱'
    },
    registeredDate() {
      const value = this.profile && this.profile.createdAt ? this.profile.createdAt : ''
      if (!value) return '--'
      return String(value).slice(0, 10)
    }
  },
  onShow() {
    this.refreshUser()
  },
  methods: {
    formatMoney,
    refreshUser() {
      const token = uni.getStorageSync('token') || ''
      const username = uni.getStorageSync('username') || 'preview'
      const profile = uni.getStorageSync('userProfile') || null

      this.isLoggedIn = !!token
      this.username = username
      this.profile = profile
      this.baseCurrency = profile && profile.baseCurrency ? profile.baseCurrency : 'CNY'

      try {
        const userStore = useUserStore()
        this.isLoggedIn = userStore.isLoggedIn
        this.username = userStore.username || username
        this.profile = userStore.profile || profile
        this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : this.baseCurrency
        if (this.isLoggedIn) {
          userStore.fetchProfile().then(() => {
            this.profile = userStore.profile
            this.baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : 'CNY'
          })
          this.fetchOverview()
        }
      } catch (error) {
        console.error('Read user store failed:', error)
      }
    },
    async fetchOverview() {
      try {
        const assetStore = useAssetStore()
        await assetStore.fetchOverview()
        this.overview = { ...DEFAULT_OVERVIEW, ...(assetStore.overview || {}) }
      } catch (error) {
        this.overview = { ...DEFAULT_OVERVIEW }
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
    },
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    },
    goTrend() {
      uni.navigateTo({ url: '/pages/trend/trend' })
    },
    showComingSoon(name) {
      uni.showToast({ title: `${name}暂未开放`, icon: 'none' })
    },
    confirmClearData() {
      uni.showModal({
        title: '清空所有数据',
        content: '该能力需要后端数据清理接口支持，当前版本暂未开放。',
        showCancel: false
      })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 24rpx 22rpx calc(144rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.profile-section,
.login-section {
  padding-top: 8rpx;
}

.profile-card,
.net-card,
.menu-group,
.about-card,
.login-card {
  background: #ffffff;
  border-radius: 18rpx;
  border: 1rpx solid #edf1f4;
  box-shadow: 0 8rpx 22rpx rgba(26, 42, 58, 0.045);
}

.profile-card {
  padding: 30rpx;
  margin-bottom: 18rpx;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #14202d, #226f63);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 850;
}

.profile-copy {
  min-width: 0;
  flex: 1;
}

.username {
  display: block;
  font-size: 36rpx;
  line-height: 46rpx;
  font-weight: 850;
  color: #17202a;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-email,
.profile-subtitle,
.menu-subtitle {
  display: block;
  color: #7b8798;
  font-size: 24rpx;
  line-height: 34rpx;
}

.net-card {
  padding: 28rpx 30rpx;
  margin-bottom: 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #14202d 0%, #174a43 100%);
}

.net-label {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-size: 24rpx;
  line-height: 34rpx;
}

.net-value {
  display: block;
  margin-top: 8rpx;
  color: #ffd166;
  font-size: 42rpx;
  line-height: 52rpx;
  font-weight: 850;
  word-break: break-all;
}

.net-arrow {
  color: rgba(255, 255, 255, 0.72);
  font-size: 48rpx;
  line-height: 48rpx;
}

.menu-group {
  margin-bottom: 18rpx;
  overflow: hidden;
}

.menu-item {
  min-height: 88rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  border-bottom: 1rpx solid #edf1f4;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item.disabled {
  opacity: 0.72;
}

.menu-title {
  color: #17202a;
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 760;
  flex: 1;
}

.menu-copy {
  flex: 1;
  min-width: 0;
  padding: 18rpx 0;
}

.menu-arrow,
.inline-picker {
  color: #94a3b8;
  font-size: 32rpx;
  flex-shrink: 0;
}

.inline-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: #226f63;
  font-size: 26rpx;
  font-weight: 800;
}

.menu-badge {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #f3f6f8;
  color: #7b8798;
  font-size: 22rpx;
  flex-shrink: 0;
}

.about-card {
  padding: 26rpx 28rpx;
  margin-bottom: 22rpx;
}

.about-title {
  display: block;
  color: #17202a;
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
  margin-bottom: 14rpx;
}

.about-links {
  display: flex;
  align-items: center;
  gap: 14rpx;
  color: #226f63;
  font-size: 24rpx;
  line-height: 34rpx;
}

.about-divider {
  color: #c8d1da;
}

.feedback {
  display: block;
  margin-top: 14rpx;
  color: #64748b;
  font-size: 24rpx;
  line-height: 34rpx;
}

.logout-btn {
  margin: 0;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 18rpx;
  background: #ffffff;
  font-size: 30rpx;
  color: #d94a62;
  border: 1rpx solid #f0c3ca;
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
