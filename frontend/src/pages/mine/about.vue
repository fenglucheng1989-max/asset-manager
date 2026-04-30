<template>
  <view class="container">
    <view v-if="section === 'about'" class="section-group">
      <view class="section-item" @click="switchTo('terms')">
        <text class="item-title">用户协议</text>
        <text class="item-arrow">›</text>
      </view>
      <view class="section-item" @click="switchTo('privacy')">
        <text class="item-title">隐私政策</text>
        <text class="item-arrow">›</text>
      </view>
      <view class="section-item" @click="switchTo('donate')">
        <text class="item-title">打赏支持</text>
        <text class="item-arrow">›</text>
      </view>
    </view>

    <view v-else-if="section === 'donate'" class="content-card">
      <text class="section-title">请作者喝杯咖啡</text>
      <text class="body intro">如果这个应用对你有帮助，欢迎打赏支持，让我有动力持续维护和改进。</text>
      <view class="donate-row">
        <view class="donate-item">
          <image class="donate-qr" src="/static/donate-wechat.png" mode="aspectFit" @error="onImageError('wechat')" v-if="!imgFailed.wechat" />
          <view class="donate-qr donate-placeholder" v-else>
            <text class="donate-placeholder-text">微信收款码</text>
            <text class="donate-placeholder-hint">请替换 /static/donate-wechat.png</text>
          </view>
          <text class="donate-label">微信打赏</text>
        </view>
        <view class="donate-item">
          <image class="donate-qr" src="/static/donate-alipay.png" mode="aspectFit" @error="onImageError('alipay')" v-if="!imgFailed.alipay" />
          <view class="donate-qr donate-placeholder" v-else>
            <text class="donate-placeholder-text">支付宝收款码</text>
            <text class="donate-placeholder-hint">请替换 /static/donate-alipay.png</text>
          </view>
          <text class="donate-label">支付宝打赏</text>
        </view>
      </view>
      <text class="body thanks">感谢每一份支持 ❤️</text>
    </view>

    <view v-else-if="section !== 'feedback'" class="content-card">
      <text class="section-title">{{ content.title }}</text>
      <text v-if="legalDocument && legalDocument.version" class="version">版本 {{ legalDocument.version }} · 生效日期 {{ legalDocument.effectiveDate }}</text>
      <view v-for="item in content.sections" :key="item.title" class="policy-section">
        <text class="policy-title">{{ item.title }}</text>
        <text class="body">{{ item.body }}</text>
      </view>
    </view>

    <view v-else class="content-card">
      <text class="section-title">意见反馈</text>
      <text class="body intro">你可以提交问题、建议或导出异常。反馈会记录到当前账号下，方便后续跟进。</text>

      <view class="form-row">
        <text class="label">反馈类型</text>
        <picker :range="feedbackTypes" range-key="label" :value="feedbackTypeIndex" @change="handleTypeChange">
          <view class="picker-value">{{ feedbackTypes[feedbackTypeIndex].label }}</view>
        </picker>
      </view>

      <view class="form-row">
        <text class="label">反馈内容</text>
        <textarea
          class="textarea"
          v-model="feedbackForm.content"
          maxlength="1000"
          placeholder="请描述遇到的问题、期望改进或复现步骤"
        />
        <text class="counter">{{ feedbackForm.content.length }}/1000</text>
      </view>

      <view class="form-row">
        <text class="label">联系方式（选填）</text>
        <input class="input" v-model="feedbackForm.contact" maxlength="120" placeholder="邮箱、手机号或其他联系方式" />
      </view>

      <button class="submit-btn" :loading="submitting" @click="submitFeedback" @tap="submitFeedback">提交反馈</button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'

const CONTENT = {
  terms: {
    title: '用户协议',
    sections: [
      {
        title: '服务说明',
        body: '资产管家用于记录个人账户、资产负债、收支流水、预算和快照。你应确保录入的信息真实、合法，并自行核对关键资产数据。'
      },
      {
        title: '账号与安全',
        body: '请妥善保管账号和密码。因账号泄露、误操作或设备环境异常导致的数据变化，由账号使用者自行承担相应风险。'
      },
      {
        title: '数据操作',
        body: '新增、编辑、归档、删除、清空数据和注销账号等操作会影响当前账号下的数据。执行高风险操作前，请仔细确认页面提示。'
      },
      {
        title: '责任边界',
        body: '本应用仅提供记录和统计能力，不构成投资、税务、法律或财务建议。涉及资金决策时，请以真实账户和专业意见为准。'
      }
    ]
  },
  privacy: {
    title: '隐私政策',
    sections: [
      {
        title: '收集范围',
        body: '应用会保存你主动录入的账号资料、资产账户、流水、预算、快照、默认货币、安全设置和反馈内容。'
      },
      {
        title: '使用目的',
        body: '这些数据用于展示资产概览、生成趋势和报表、完成 CSV 导出、处理账户安全操作以及定位你提交的问题反馈。'
      },
      {
        title: '数据隔离',
        body: '业务数据按当前登录用户隔离。普通用户只能访问自己的数据，后台管理能力应仅由授权管理员使用。'
      },
      {
        title: '删除与导出',
        body: '你可以在数据与同步页面导出 CSV 或清空业务数据；也可以在安全账户页面注销账号。删除类操作完成后通常不可恢复。'
      }
    ]
  },
}

export default {
  data() {
    return {
      section: 'about',
      legalDocument: null,
      submitting: false,
      feedbackTypeIndex: 0,
      feedbackTypes: [
        { label: '问题反馈', value: 'BUG' },
        { label: '功能建议', value: 'SUGGESTION' },
        { label: '导出问题', value: 'EXPORT' },
        { label: '账户与安全', value: 'ACCOUNT' },
        { label: '其他', value: 'OTHER' }
      ],
      feedbackForm: {
        content: '',
        contact: ''
      },
      imgFailed: {
        wechat: false,
        alipay: false
      }
    }
  },
  computed: {
    content() {
      if (this.legalDocument) {
        return {
          title: this.legalDocument.title,
          sections: this.parseDocumentSections(this.legalDocument.content)
        }
      }
      return CONTENT[this.section] || {}
    }
  },
  onLoad(options) {
    this.section = options && options.section ? options.section : 'about'
    this.fetchLegalDocument()
  },
  methods: {
    switchTo(section) {
      this.section = section
      this.legalDocument = null
      this.fetchLegalDocument()
    },
    async fetchLegalDocument() {
      this.legalDocument = null
      if (this.section !== 'terms' && this.section !== 'privacy') return
      try {
        const type = this.section === 'terms' ? 'TERMS' : 'PRIVACY'
        const res = await useUserStore().fetchLatestLegalDocument(type)
        if (res && res.code === 200) {
          this.legalDocument = res.data
        }
      } catch (error) {
        this.legalDocument = null
      }
    },
    parseDocumentSections(content) {
      const lines = String(content || '').split(/\r?\n/).map(line => line.trim()).filter(Boolean)
      const sections = []
      for (let index = 0; index < lines.length; index += 2) {
        sections.push({
          title: lines[index] || '说明',
          body: lines[index + 1] || ''
        })
      }
      return sections.length > 0 ? sections : [{ title: '说明', body: String(content || '') }]
    },
    onImageError(type) {
      this.imgFailed[type] = true
    },
    handleTypeChange(event) {
      this.feedbackTypeIndex = Number(event.detail.value)
    },
    async submitFeedback() {
      const content = this.feedbackForm.content.trim()
      if (!content) {
        uni.showToast({ title: '请填写反馈内容', icon: 'none' })
        return
      }
      if (this.submitting) return
      this.submitting = true
      try {
        const res = await useUserStore().submitFeedback({
          type: this.feedbackTypes[this.feedbackTypeIndex].value,
          content,
          contact: this.feedbackForm.contact.trim()
        })
        if (res && res.code === 200) {
          this.feedbackForm = { content: '', contact: '' }
          uni.showToast({ title: '反馈已提交', icon: 'success' })
        }
      } catch (error) {
        const message = error && error.message ? error.message : '反馈提交失败'
        uni.showToast({ title: message, icon: 'none' })
      } finally {
        this.submitting = false
      }
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

.item-title {
  flex: 1;
  display: block;
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 700;
  color: var(--app-text, #17202a);
}

.item-arrow {
  color: var(--app-muted, #64748b);
  font-size: 32rpx;
  flex-shrink: 0;
}

.content-card {
  padding: 30rpx;
  margin-bottom: 18rpx;
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.section-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 32rpx;
  line-height: 44rpx;
  font-weight: 820;
  margin-bottom: 22rpx;
}

.version {
  display: block;
  margin-top: -12rpx;
  margin-bottom: 18rpx;
  color: var(--app-faint, #94a3b8);
  font-size: 23rpx;
  line-height: 32rpx;
}

.policy-section {
  padding: 22rpx 0;
  border-top: 1rpx solid var(--app-border, #edf1f4);
}

.policy-section:first-of-type {
  border-top: none;
  padding-top: 0;
}

.policy-title,
.label {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
  margin-bottom: 10rpx;
}

.body {
  display: block;
  color: var(--app-muted, #64748b);
  font-size: 27rpx;
  line-height: 42rpx;
}

.intro {
  margin-bottom: 24rpx;
}

.form-row {
  margin-bottom: 24rpx;
}

.picker-value,
.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  border-radius: 14rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-input-bg, #f6f8fb);
  color: var(--app-text, #17202a);
  font-size: 28rpx;
}

.picker-value,
.input {
  height: 84rpx;
  line-height: 84rpx;
  padding: 0 24rpx;
}

.textarea {
  min-height: 220rpx;
  padding: 22rpx 24rpx;
  line-height: 38rpx;
}

.counter {
  display: block;
  margin-top: 8rpx;
  color: var(--app-faint, #94a3b8);
  font-size: 22rpx;
  text-align: right;
}

.submit-btn {
  margin: 8rpx 0 0;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 16rpx;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-size: 30rpx;
}

.donate-row {
  display: flex;
  justify-content: center;
  gap: 48rpx;
  margin: 36rpx 0 28rpx;
}

.donate-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.donate-qr {
  width: 280rpx;
  height: 280rpx;
  border-radius: 14rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-input-bg, #f6f8fb);
}

.donate-label {
  font-size: 25rpx;
  color: var(--app-muted, #64748b);
}

.donate-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.donate-placeholder-text {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-muted, #64748b);
}

.donate-placeholder-hint {
  font-size: 22rpx;
  color: var(--app-faint, #94a3b8);
}

.thanks {
  text-align: center;
}
</style>
