<template>
  <view class="container">
    <view class="section-group">
      <view class="section-item" @click="handleChangeAvatar">
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
        <view class="item-copy">
          <text class="item-title">用户名</text>
          <text class="item-desc">注册后不可修改</text>
        </view>
        <text class="item-value">{{ username }}</text>
      </view>

      <view class="section-item" @click="startEditEmail">
        <view class="item-copy">
          <text class="item-title">邮箱</text>
          <text class="item-desc">{{ editEmail ? '' : (email || '未设置') }}</text>
        </view>
        <template v-if="editEmail">
          <input class="inline-input" v-model="emailForm.email" placeholder="请输入邮箱" />
          <text class="action-btn" @click.stop="saveEmail">保存</text>
          <text class="action-btn cancel" @click.stop="editEmail = false">取消</text>
        </template>
        <text v-else class="item-arrow">›</text>
      </view>

      <view class="section-item">
        <view class="item-copy">
          <text class="item-title">手机号</text>
          <text class="item-desc">暂不支持修改</text>
        </view>
        <text class="item-value muted">--</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item" @click="goSecurity">
        <view class="item-copy">
          <text class="item-title">修改密码</text>
          <text class="item-desc">更新登录密码</text>
        </view>
        <text class="item-arrow">›</text>
      </view>

      <view class="section-item" @click="handleExportData">
        <view class="item-copy">
          <text class="item-title">个人信息导出</text>
          <text class="item-desc">下载个人数据的 JSON 备份</text>
        </view>
        <text class="item-arrow">›</text>
      </view>
    </view>

    <view class="section-group">
      <view class="section-item danger" @click="handleDeleteAccount">
        <text class="danger-text">注销账号</text>
        <text class="item-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'

export default {
  data() {
    return {
      username: '',
      email: '',
      avatarUrl: '',
      editEmail: false,
      emailForm: { email: '' }
    }
  },
  onShow() {
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
    startEditEmail() {
      this.emailForm.email = this.email || ''
      this.editEmail = true
    },
    async saveEmail() {
      const email = this.emailForm.email.trim()
      try {
        const userStore = useUserStore()
        const result = await userStore.updateProfile({ email: email || null })
        if (result && result.code === 200) {
          this.email = email
          this.editEmail = false
          uni.showToast({ title: '邮箱已更新', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : '更新失败'
        uni.showToast({ title: message, icon: 'none' })
      }
    },
    handleExportData() {
      uni.showModal({
        title: '导出个人信息',
        content: '将下载包含个人信息的 JSON 备份文件，是否继续？',
        success: async (result) => {
          if (!result.confirm) return
          try {
            const userStore = useUserStore()
            const res = await userStore.exportBackup()
            if (res && res.code === 200) {
              this.downloadBackup(res.data)
            }
          } catch (error) {
            uni.showToast({ title: '导出失败', icon: 'none' })
          }
        }
      })
    },
    downloadBackup(data) {
      // #ifdef H5
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'asset-manager-profile-' + new Date().toISOString().slice(0, 10) + '.json'
      a.click()
      URL.revokeObjectURL(url)
      uni.showToast({ title: '下载已开始', icon: 'success' })
      // #endif
      // #ifndef H5
      uni.showToast({ title: '导出成功，请查看备份文件', icon: 'success' })
      // #endif
    },
    goSecurity() {
      uni.navigateTo({ url: '/pages/mine/security' })
    },
    handleDeleteAccount() {
      uni.showModal({
        title: '账号注销',
        content: '注销后所有数据将被永久删除且不可恢复。确定要继续吗？',
        confirmText: '我确定要注销',
        cancelText: '再想想',
        success: (result) => {
          if (!result.confirm) return
          uni.navigateTo({ url: '/pages/mine/security?action=delete' })
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
}

.section-group {
  margin-bottom: 20rpx;
  overflow: hidden;
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26,42,58,0.045));
}

.section-item {
  min-height: 96rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.section-item:last-child { border-bottom: none; }

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
  background-color: var(--app-soft-bg, #eef3f8);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-placeholder {
  font-size: 34rpx;
  font-weight: 850;
  color: var(--app-muted, #7b8798);
}

.inline-input {
  width: 200rpx;
  height: 64rpx;
  border-radius: 8rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  padding: 0 16rpx;
  font-size: 26rpx;
  color: var(--app-text, #17202a);
  background: var(--app-input-bg, #f6f8fb);
}

.action-btn {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--app-primary-dark, #226f63);
  padding: 8rpx 12rpx;
}

.action-btn.cancel {
  color: var(--app-muted, #64748b);
}

.danger-text {
  font-size: 29rpx;
  font-weight: 700;
  color: var(--app-danger, #d94a62);
}

.danger .item-arrow {
  color: var(--app-danger, #d94a62);
}
</style>
