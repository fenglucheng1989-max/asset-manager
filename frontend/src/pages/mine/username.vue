<template>
  <view class="container" :style="themeVars">
    <view class="page-hero">
      <text class="page-title">修改用户名</text>
      <text class="page-sub" v-if="currentUsername">当前: {{ currentUsername }}</text>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="form-label">新用户名</text>
        <input
          class="form-input"
          v-model="usernameForm.username"
          placeholder="请输入用户名"
          @input="usernameError = ''"
        />
      </view>
      <text v-if="usernameError" class="field-error">{{ usernameError }}</text>
      <text class="field-hint">支持中英文、数字和下划线，1-50个字符</text>
    </view>

    <view class="btn-group">
      <button class="btn-save" :disabled="!canSubmit || isSaving" @click="handleSave">
        {{ isSaving ? '保存中' : '保存' }}
      </button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'
import { getThemeVars } from '../../utils/theme'

const USERNAME_RE = /^[a-zA-Z0-9_一-龥]{1,50}$/

export default {
  data() {
    return {
      currentUsername: '',
      usernameForm: { username: '' },
      usernameError: '',
      isSaving: false,
      themeVars: getThemeVars()
    }
  },
  computed: {
    canSubmit() {
      return this.usernameForm.username.trim().length > 0
    }
  },
  onLoad() {
    const userStore = useUserStore()
    const profile = userStore.profile || uni.getStorageSync('userProfile') || {}
    this.currentUsername = profile.username || ''
    this.usernameForm.username = this.currentUsername
  },
  onShow() {
    this.themeVars = getThemeVars()
  },
  methods: {
    async handleSave() {
      const username = this.usernameForm.username.trim()
      if (!username) {
        this.usernameError = '请输入用户名'
        return
      }
      if (!USERNAME_RE.test(username)) {
        this.usernameError = '用户名仅支持中英文、数字和下划线'
        return
      }
      this.usernameError = ''
      this.isSaving = true
      try {
        const userStore = useUserStore()
        const result = await userStore.updateProfile({ username })
        if (result && result.code === 200) {
          uni.showToast({ title: '用户名已更新', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        }
      } catch (error) {
        const message = error && error.message ? error.message : '更新失败'
        uni.showToast({ title: message, icon: 'none' })
      } finally {
        this.isSaving = false
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
  background: var(--app-card-bg, #ffffff);
  border-radius: 20rpx;
  padding: 22rpx 28rpx 10rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.form-item {
  display: flex;
  align-items: center;
  min-height: 90rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  gap: 22rpx;
}

.form-label {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  width: 155rpx;
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

.field-error {
  display: block;
  margin: 8rpx 0 0;
  color: var(--app-danger, #d94a62);
  font-size: 24rpx;
  padding-left: 177rpx;
}

.field-hint {
  display: block;
  margin: 4rpx 0 10rpx;
  padding-left: 177rpx;
  color: var(--app-muted, #7b8798);
  font-size: 22rpx;
  line-height: 32rpx;
  text-align: right;
}

.btn-group {
  padding: 6rpx 4rpx 0;
}

.btn-save {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  border-radius: 999rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.btn-save[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
}
</style>
