<template>
  <view class="container" :style="themeVars">
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
        <view class="menu-item" @click="goSnapshotManage">
          <view class="menu-copy">
            <text class="menu-title">快照管理</text>
            <text class="menu-subtitle">查看历史快照或记录今日资产</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @click="goHealth">
          <view class="menu-copy">
            <text class="menu-title">财务健康</text>
            <text class="menu-subtitle">查看评级、指标解释和改进建议</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goSummary">
          <view class="menu-copy">
            <text class="menu-title">财务月报</text>
            <text class="menu-subtitle">基于已记录流水生成月度摘要</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goMilestones">
          <view class="menu-copy">
            <text class="menu-title">资产里程碑</text>
            <text class="menu-subtitle">记录净资产节点和自定义事件</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @click="goSettings">
          <view class="menu-copy">
            <text class="menu-title">设置</text>
            <text class="menu-subtitle">视觉装扮、默认货币、数据管理和账号设置</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goFeedback">
          <view class="menu-copy">
            <text class="menu-title">意见反馈</text>
            <text class="menu-subtitle">问题、建议和导出异常</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goAbout('about')">
          <view class="menu-copy">
            <text class="menu-title">关于</text>
            <text class="menu-subtitle">用户协议、隐私政策等</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
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
import { useAssetStore } from '../../store/asset'
import { useUserStore } from '../../store/user'
import { formatMoney } from '../../utils/money'
import { getThemeMode, getThemeVars } from '../../utils/theme'

const DEFAULT_OVERVIEW = {
  totalAsset: 0,
  totalLiability: 0,
  netWorth: 0,
  accountCount: 0,
  lastUpdateTime: null
}

export default {
  components: { CustomTabBar },
  data() {
    return {
      isRegister: false,
      authSubmitting: false,
      loginForm: {
        username: '',
        password: '',
        email: '',
        acceptLegal: false
      },
      legalDocuments: {
        terms: null,
        privacy: null
      },
      isLoggedIn: false,
      username: 'preview',
      profile: null,
      overview: { ...DEFAULT_OVERVIEW },
      themeVars: getThemeVars()
    }
  },
  computed: {
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
    this.themeVars = getThemeVars(getThemeMode())
    this.refreshUser()
    this.fetchLegalDocuments()
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

      try {
        const userStore = useUserStore()
        this.isLoggedIn = userStore.isLoggedIn
        this.username = userStore.username || username
        this.profile = userStore.profile || profile
        if (this.isLoggedIn) {
          userStore.fetchProfile().then(() => {
            this.profile = userStore.profile
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
    async handleSubmit() {
      if (this.authSubmitting) return
      if (!this.loginForm.username.trim() || !this.loginForm.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
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
          ? await userStore.register(this.loginForm.username, this.loginForm.password, this.loginForm.email, {
              acceptLegal: this.loginForm.acceptLegal,
              acceptedTermsVersion: this.legalDocuments.terms && this.legalDocuments.terms.version,
              acceptedPrivacyVersion: this.legalDocuments.privacy && this.legalDocuments.privacy.version
            })
          : await userStore.login(this.loginForm.username, this.loginForm.password)
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
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.profile-card {
  position: relative;
  overflow: hidden;
  padding: 30rpx;
  margin-bottom: 18rpx;
  background: var(--app-outfit-header-bg, var(--app-card-bg, #ffffff)) !important;
  color: var(--app-outfit-header-text, var(--app-text, #17202a));
  border-color: transparent !important;
}

.profile-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--app-outfit-header-pattern, none);
  opacity: 0.82;
  pointer-events: none;
}

.profile-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.70);
  border: 1rpx solid rgba(255, 255, 255, 0.52);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  color: var(--app-outfit-header-accent, var(--app-primary, #2ebd85));
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
  color: var(--app-outfit-header-text, var(--app-text, #17202a));
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-email,
.profile-subtitle,
.menu-subtitle {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.profile-card .profile-email,
.profile-card .profile-subtitle {
  color: var(--app-outfit-header-sub, var(--app-muted, #7b8798));
}

.net-card {
  position: relative;
  overflow: hidden;
  padding: 28rpx 30rpx;
  margin-bottom: 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--app-outfit-header-bg, var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%)));
}

.net-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--app-outfit-header-pattern, none);
  opacity: 0.7;
  pointer-events: none;
}

.net-card > view,
.net-card > text {
  position: relative;
  z-index: 1;
}

.net-label {
  display: block;
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255, 255, 255, 0.72)));
  font-size: 24rpx;
  line-height: 34rpx;
}

.net-value {
  display: block;
  margin-top: 8rpx;
  color: var(--app-outfit-header-accent, var(--app-hero-accent, #ffd166));
  font-size: 42rpx;
  line-height: 52rpx;
  font-weight: 850;
  word-break: break-all;
}

.net-arrow {
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255, 255, 255, 0.72)));
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
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item.disabled {
  opacity: 0.72;
}

.menu-title {
  color: var(--app-text, #17202a);
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
  color: var(--app-faint, #94a3b8);
  font-size: 32rpx;
  flex-shrink: 0;
}

.inline-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: var(--app-primary-dark, #226f63);
  font-size: 26rpx;
  font-weight: 800;
}

.about-card {
  padding: 26rpx 28rpx;
  margin-bottom: 22rpx;
}

.about-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
  margin-bottom: 14rpx;
}

.about-links {
  display: flex;
  align-items: center;
  gap: 14rpx;
  color: var(--app-primary-dark, #226f63);
  font-size: 24rpx;
  line-height: 34rpx;
}

.about-divider {
  color: var(--app-faint, #c8d1da);
}

.feedback {
  display: block;
  margin-top: 14rpx;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  line-height: 34rpx;
}

.logout-btn {
  margin: 0;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 18rpx;
  background: var(--app-card-bg, #ffffff);
  font-size: 30rpx;
  color: var(--app-danger, #d94a62);
  border: 1rpx solid var(--app-border, #edf1f4);
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
  border-color: var(--app-primary, #2ebd85);
  background: var(--app-soft-bg, #ecfdf5);
}

.legal-check-mark {
  color: var(--app-primary, #2ebd85);
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
  color: var(--app-primary-dark, #226f63);
  font-weight: 800;
}

.login-btn {
  background: var(--app-primary, #2ebd85);
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
  color: var(--app-primary-dark, #226f63);
  font-size: 28rpx;
}
</style>
