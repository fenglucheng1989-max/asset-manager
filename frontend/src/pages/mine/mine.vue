<template>
  <view class="container" :style="themeVars">
    <view v-if="isLoggedIn" class="profile-section">
      <view class="profile-card">
        <view class="profile-main">
          <view
            class="profile-avatar"
            :style="{ backgroundImage: avatarUrl ? 'url(' + avatarUrl + ')' : 'none' }"
            @click="goAccountSettings"
          >
            <text v-if="!avatarUrl" class="profile-avatar-text">{{ username.substring(0, 1).toUpperCase() }}</text>
            <view class="profile-avatar-badge">
              <text class="profile-avatar-badge-text">+</text>
            </view>
          </view>
          <view class="profile-copy">
            <text class="profile-name">{{ username }}</text>
            <text class="profile-info">注册于 {{ registeredDate }}</text>
            <text class="profile-hint">点击头像管理账号资料</text>
          </view>
        </view>
      </view>

      <text class="menu-section-title">资产工具</text>
      <view class="menu-group">
        <view class="menu-item" @click="goTrend">
          <view class="menu-icon"><text class="menu-icon-text">↗</text></view>
          <view class="menu-copy">
            <text class="menu-title">资产趋势</text>
            <text class="menu-subtitle">查看净资产和分类资产走势</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goSnapshotManage">
          <view class="menu-icon"><text class="menu-icon-text">◉</text></view>
          <view class="menu-copy">
            <text class="menu-title">快照管理</text>
            <text class="menu-subtitle">查看历史快照或记录今日资产</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <text class="menu-section-title">财务洞察</text>
      <view class="menu-group">
        <view class="menu-item" @click="goHealth">
          <view class="menu-icon"><text class="menu-icon-text">♡</text></view>
          <view class="menu-copy">
            <text class="menu-title">财务健康</text>
            <text class="menu-subtitle">查看评级、指标解释和改进建议</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goSummary">
          <view class="menu-icon"><text class="menu-icon-text">≡</text></view>
          <view class="menu-copy">
            <text class="menu-title">财务月报</text>
            <text class="menu-subtitle">基于已记录流水生成月度摘要</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goMilestones">
          <view class="menu-icon"><text class="menu-icon-text">★</text></view>
          <view class="menu-copy">
            <text class="menu-title">资产里程碑</text>
            <text class="menu-subtitle">记录净资产节点和自定义事件</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <text class="menu-section-title">系统服务</text>
      <view class="menu-group">
        <view class="menu-item" @click="goSettings">
          <view class="menu-icon"><text class="menu-icon-text">⚙</text></view>
          <view class="menu-copy">
            <text class="menu-title">设置</text>
            <text class="menu-subtitle">视觉装扮、默认货币、数据管理和账号设置</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goFeedback">
          <view class="menu-icon"><text class="menu-icon-text">✉</text></view>
          <view class="menu-copy">
            <text class="menu-title">意见反馈</text>
            <text class="menu-subtitle">问题、建议和导出异常</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goAbout('about')">
          <view class="menu-icon"><text class="menu-icon-text">i</text></view>
          <view class="menu-copy">
            <text class="menu-title">关于</text>
            <text class="menu-subtitle">用户协议、隐私政策等</text>
          </view>
          <text class="menu-version">v{{ appVersion }}</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

    </view>

    <view v-else class="login-section">
      <view class="login-card">
        <text class="login-title">{{ isRegister ? '注册' : '登录' }}</text>

        <view class="input-group">
          <input class="login-input" v-model="loginForm.email" placeholder="邮箱" />
          <input class="login-input" v-model="loginForm.password" type="password" placeholder="密码" />
          <input
            v-if="isRegister"
            class="login-input"
            v-model="loginForm.username"
            placeholder="用户名（选填，自动生成）"
          />
          <view v-if="isRegister" class="legal-row" @click="loginForm.acceptLegal = !loginForm.acceptLegal">
            <view class="legal-check" :class="{ checked: loginForm.acceptLegal }">
              <text v-if="loginForm.acceptLegal" class="legal-check-mark">✓</text>
            </view>
            <text class="legal-copy">我已阅读并同意</text>
            <text class="legal-link" @click.stop="goAbout('terms')">《用户协议》</text>
            <text class="legal-copy">和</text>
            <text class="legal-link" @click.stop="goAbout('privacy')">《隐私政策》</text>
          </view>
        </view>

        <view class="login-btn" @click="handleSubmit" @tap="handleSubmit">
          {{ isRegister ? '注册' : '登录' }}
        </view>

        <view class="switch-mode" @click="isRegister = !isRegister">
          <text class="switch-text">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </text>
        </view>
      </view>
    </view>

    <custom-tab-bar />
  </view>
</template>

<script>
import CustomTabBar from '../../custom-tab-bar/index.vue'
import { useUserStore } from '../../store/user'
import { getThemeMode, getThemeVars } from '../../utils/theme'

export default {
  components: { CustomTabBar },
  data() {
    return {
      isRegister: false,
      authSubmitting: false,
      loginForm: {
        email: '',
        password: '',
        username: '',
        acceptLegal: false
      },
      legalDocuments: {
        terms: null,
        privacy: null
      },
      isLoggedIn: false,
      username: 'preview',
      profile: null,
      avatarUrl: '',
      appVersion: '1.0.0',
      themeVars: getThemeVars()
    }
  },
  computed: {
    registeredDate() {
      const value = this.profile && this.profile.createdAt ? this.profile.createdAt : ''
      if (!value) return '--'
      return String(value).slice(0, 10)
    }
  },
  onShow() {
    this.themeVars = getThemeVars(getThemeMode())
    this.refreshUser()
    this.fetchLegalDocuments()
  },
  methods: {
    refreshUser() {
      const token = uni.getStorageSync('token') || ''
      const username = uni.getStorageSync('username') || 'preview'
      const profile = uni.getStorageSync('userProfile') || null

      this.isLoggedIn = !!token
      this.username = username
      this.profile = profile
      this.avatarUrl = (profile && profile.avatarUrl) || ''

      try {
        const userStore = useUserStore()
        this.isLoggedIn = userStore.isLoggedIn
        this.username = userStore.username || username
        this.profile = userStore.profile || profile
        this.avatarUrl = (userStore.profile && userStore.profile.avatarUrl) || this.avatarUrl
        if (this.isLoggedIn) {
          userStore.fetchProfile().then(() => {
            this.profile = userStore.profile
            this.avatarUrl = (userStore.profile && userStore.profile.avatarUrl) || ''
          })
        }
      } catch (error) {
        console.error('Read user store failed:', error)
      }
    },
    async handleSubmit() {
      if (this.authSubmitting) return
      const email = this.loginForm.email.trim()
      if (!email || !this.loginForm.password) {
        uni.showToast({ title: '请输入邮箱和密码', icon: 'none' })
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      if (this.isRegister && !this.loginForm.acceptLegal) {
        uni.showToast({ title: '请先阅读并同意用户协议和隐私政策', icon: 'none' })
        return
      }

      let res
      this.authSubmitting = true
      try {
        const userStore = useUserStore()
        if (this.isRegister && (!this.legalDocuments.terms || !this.legalDocuments.privacy)) {
          await this.fetchLegalDocuments()
        }
        res = this.isRegister
          ? await userStore.register(email, this.loginForm.password, this.loginForm.username || undefined, {
              acceptLegal: this.loginForm.acceptLegal,
              acceptedTermsVersion: this.legalDocuments.terms && this.legalDocuments.terms.version,
              acceptedPrivacyVersion: this.legalDocuments.privacy && this.legalDocuments.privacy.version
            })
          : await userStore.login(email, this.loginForm.password)
      } catch (error) {
        const message = error && error.message ? error.message : '登录失败'
        uni.showToast({ title: message, icon: 'none' })
        return
      } finally {
        this.authSubmitting = false
      }

      if (res && res.code === 200) {
        this.refreshUser()
        uni.showToast({ title: this.isRegister ? '注册成功' : '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      }
    },
    async fetchLegalDocuments() {
      try {
        const res = await useUserStore().fetchLatestLegalDocuments()
        if (res && res.code === 200 && res.data) {
          this.legalDocuments = {
            terms: res.data.terms || null,
            privacy: res.data.privacy || null
          }
        }
      } catch (error) {
        this.legalDocuments = { terms: null, privacy: null }
      }
    },
    goTrend() {
      uni.navigateTo({ url: '/pages/trend/trend' })
    },
    goHealth() {
      uni.navigateTo({ url: '/pages/insight/health' })
    },
    goSummary() {
      uni.navigateTo({ url: '/pages/insight/summary' })
    },
    goMilestones() {
      uni.navigateTo({ url: '/pages/insight/milestones' })
    },
    goSnapshotManage() {
      uni.navigateTo({ url: '/pages/mine/snapshots' })
    },
    goFeedback() {
      uni.navigateTo({ url: '/pages/mine/about?section=feedback' })
    },
    goAccountSettings() {
      uni.navigateTo({ url: '/pages/mine/account' })
    },
    goSettings() {
      uni.navigateTo({ url: '/pages/mine/settings' })
    },
    goAbout(section) {
      uni.navigateTo({ url: `/pages/mine/about?section=${section || 'about'}` })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 24rpx 22rpx calc(144rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.profile-section,
.login-section {
  padding-top: 8rpx;
}

.profile-card,
.menu-group,
.login-card {
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.profile-card {
  padding: 30rpx 30rpx 28rpx;
  margin-bottom: 20rpx;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 24rpx;
  min-width: 0;
}

.profile-avatar {
  position: relative;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background-color: var(--app-primary, #d3a414);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 5rpx solid var(--app-card-bg, #ffffff);
  box-shadow:
    0 0 0 2rpx var(--app-border, #edf1f4),
    0 12rpx 26rpx rgba(15, 23, 42, 0.13);
  flex-shrink: 0;
}

.profile-avatar-text {
  color: #ffffff;
  font-size: 46rpx;
  font-weight: 700;
}

.profile-avatar-badge {
  position: absolute;
  right: -2rpx;
  bottom: -2rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: var(--app-primary, #d3a414);
  border: 4rpx solid var(--app-card-bg, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 14rpx rgba(15, 23, 42, 0.14);
}

.profile-avatar-badge-text {
  color: #ffffff;
  font-size: 26rpx;
  line-height: 26rpx;
  font-weight: 700;
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-name {
  display: block;
  font-size: 36rpx;
  line-height: 46rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.profile-info {
  display: block;
  font-size: 24rpx;
  line-height: 34rpx;
  color: var(--app-muted, #7b8798);
}

.profile-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  line-height: 32rpx;
  color: var(--app-faint, #94a3b8);
}

.menu-section-title {
  display: block;
  margin: 0 10rpx 12rpx;
  color: var(--app-muted, #7b8798);
  font-size: 23rpx;
  line-height: 32rpx;
  font-weight: 600;
}

.menu-group {
  margin-bottom: 24rpx;
  overflow: hidden;
}

.menu-item {
  min-height: 98rpx;
  padding: 0 26rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: var(--app-card-bg-alt, var(--app-soft-bg, #ecfdf5));
  border: 1rpx solid var(--app-border, #edf1f4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-icon-text {
  color: var(--app-primary, #d3a414);
  font-size: 27rpx;
  line-height: 27rpx;
  font-weight: 600;
}

.menu-copy {
  flex: 1;
  min-width: 0;
  padding: 18rpx 0;
}

.menu-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 600;
}

.menu-subtitle {
  display: block;
  margin-top: 4rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.menu-version {
  color: var(--app-faint, #94a3b8);
  font-size: 24rpx;
  flex-shrink: 0;
}

.menu-arrow {
  color: var(--app-faint, #94a3b8);
  font-size: 32rpx;
  flex-shrink: 0;
}

.login-card {
  padding: 42rpx 34rpx;
}

.login-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
  display: block;
  margin-bottom: 34rpx;
}

.input-group {
  margin-bottom: 40rpx;
}

.login-input {
  background: var(--app-input-bg, #f6f8fb);
  border-radius: 14rpx;
  height: 88rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
}

.legal-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6rpx;
  margin-top: -4rpx;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  line-height: 36rpx;
}

.legal-check {
  width: 32rpx;
  height: 32rpx;
  border-radius: 4rpx;
  border: 1rpx solid var(--app-border, #cbd5e1);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-card-bg, #ffffff);
  flex-shrink: 0;
}

.legal-check.checked {
  border-color: var(--app-primary, #d3a414);
  background: var(--app-soft-bg, #f6f8fb);
}

.legal-check-mark {
  color: var(--app-primary, #d3a414);
  font-size: 24rpx;
  line-height: 24rpx;
  font-weight: 900;
}

.legal-copy,
.legal-link {
  font-size: 24rpx;
  line-height: 36rpx;
}

.legal-link {
  color: var(--app-primary, #d3a414);
  font-weight: 800;
}

.login-btn {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  margin-bottom: 30rpx;
}

.switch-mode {
  text-align: center;
}

.switch-text {
  color: var(--app-primary, #d3a414);
  font-size: 28rpx;
}
</style>
