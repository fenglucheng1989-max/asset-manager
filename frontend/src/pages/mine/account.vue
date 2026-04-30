<template>
  <view class="container" :style="themeVars">
    <view class="section-group">
      <view class="section-item" @click="handleChangeAvatar">
        <view class="item-icon"><text class="item-icon-text">◉</text></view>
        <view class="item-copy">
          <text class="item-title">头像</text>
          <text class="item-desc">点击更换头像</text>
        </view>
        <view class="avatar-preview" :style="{ backgroundImage: avatarUrl ? 'url(' + avatarUrl + ')' : 'none' }">
          <text v-if="!avatarUrl" class="avatar-placeholder">{{ username.substring(0, 1).toUpperCase() }}</text>
        </view>
        <text class="item-arrow">›</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item">
        <view class="item-icon"><text class="item-icon-text">☺</text></view>
        <view class="item-copy">
          <text class="item-title">用户名</text>
          <text class="item-desc">注册后不可修改</text>
        </view>
        <text class="item-value">{{ username }}</text>
      </view>

      <view class="section-item" @click="goEmail">
        <view class="item-icon"><text class="item-icon-text">@</text></view>
        <view class="item-copy">
          <text class="item-title">邮箱</text>
          <text class="item-desc">{{ email || '未设置' }}</text>
        </view>
        <text class="item-arrow">›</text>
      </view>

      <view class="section-item">
        <view class="item-icon"><text class="item-icon-text">☎</text></view>
        <view class="item-copy">
          <text class="item-title">手机号</text>
          <text class="item-desc">暂不支持修改</text>
        </view>
        <text class="item-value muted">--</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item" @click="goSecurity">
        <view class="item-icon"><text class="item-icon-text">*</text></view>
        <view class="item-copy">
          <text class="item-title">修改密码</text>
          <text class="item-desc">更新登录密码</text>
        </view>
        <text class="item-arrow">›</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item danger" @click="handleDeleteAccount">
        <view class="item-icon danger-icon"><text class="item-icon-text">!</text></view>
        <text class="danger-text">注销账号</text>
        <text class="item-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      username: '',
      email: '',
      avatarUrl: '',
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.refreshProfile()
  },
  methods: {
    refreshProfile() {
      const userStore = useUserStore()
      if (userStore.isLoggedIn) {
        userStore.fetchProfile().then(() => {
          const profile = userStore.profile
          this.username = profile.username || ''
          this.email = profile.email || ''
          this.avatarUrl = profile.avatarUrl || ''
        })
      }
      const profile = uni.getStorageSync('userProfile') || null
      if (profile) {
        this.username = this.username || profile.username || ''
        this.email = this.email || profile.email || ''
        this.avatarUrl = this.avatarUrl || profile.avatarUrl || ''
      }
    },
    handleChangeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const tempFilePath = res.tempFilePaths[0]
          const base64 = await this.fileToBase64(tempFilePath)
          if (!base64) {
            uni.showToast({ title: '图片读取失败', icon: 'none' })
            return
          }
          try {
            const userStore = useUserStore()
            const result = await userStore.updateProfile({ avatarUrl: base64 })
            if (result && result.code === 200) {
              this.avatarUrl = base64
              uni.showToast({ title: '头像已更新', icon: 'success' })
            }
          } catch (error) {
            const message = error && error.message ? error.message : '更新失败'
            uni.showToast({ title: message, icon: 'none' })
          }
        }
      })
    },
    fileToBase64(filePath) {
      return new Promise((resolve) => {
        // #ifdef H5
        const xhr = new XMLHttpRequest()
        xhr.open('GET', filePath, true)
        xhr.responseType = 'blob'
        xhr.onload = () => {
          const reader = new FileReader()
          reader.onloadend = () => resolve(reader.result)
          reader.onerror = () => resolve(null)
          reader.readAsDataURL(xhr.response)
        }
        xhr.onerror = () => resolve(null)
        xhr.send()
        // #endif
        // #ifndef H5
        uni.getFileSystemManager().readFile({
          filePath,
          encoding: 'base64',
          success: (res) => resolve('data:image/png;base64,' + res.data),
          fail: () => resolve(null)
        })
        // #endif
      })
    },
    goEmail() {
      uni.navigateTo({ url: '/pages/mine/email' })
    },
    goSecurity() {
      uni.navigateTo({ url: '/pages/mine/security' })
    },
    handleDeleteAccount() {
      uni.navigateTo({ url: '/pages/mine/delete-account' })
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

.section-group {
  margin-bottom: 20rpx;
  overflow: hidden;
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.section-item {
  min-height: 96rpx;
  padding: 0 26rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.section-item:last-child { border-bottom: none; }

.item-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: var(--app-card-bg-alt, var(--app-soft-bg, #eff1f5));
  border: 1rpx solid var(--app-border, #edf1f4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon-text {
  color: var(--app-primary, #d3a414);
  font-size: 26rpx;
  line-height: 26rpx;
  font-weight: 600;
}

.danger-icon {
  background: var(--app-status-risk-bg, #fff1f2);
  border-color: rgba(217, 74, 98, 0.16);
}

.danger-icon .item-icon-text {
  color: var(--app-danger, #d94a62);
}

.item-copy {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: block;
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
}

.item-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  line-height: 32rpx;
  color: var(--app-muted, #94a3b8);
}

.item-arrow {
  color: var(--app-muted, #64748b);
  font-size: 32rpx;
  flex-shrink: 0;
}

.item-value {
  font-size: 28rpx;
  color: var(--app-text, #17202a);
  flex-shrink: 0;
}

.item-value.muted {
  color: var(--app-faint, #94a3b8);
}

.avatar-preview {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: var(--app-primary, #d3a414);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid var(--app-card-bg, #ffffff);
  box-shadow:
    0 0 0 2rpx var(--app-border, #edf1f4),
    0 8rpx 18rpx rgba(15, 23, 42, 0.10);
  flex-shrink: 0;
}

.avatar-placeholder {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.danger-text {
  font-size: 29rpx;
  font-weight: 700;
  color: var(--app-danger, #d94a62);
  flex: 1;
}

.danger .item-arrow {
  color: var(--app-danger, #d94a62);
}
</style>
