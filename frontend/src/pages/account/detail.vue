<template>
  <view class="container" :style="themeVars">
    <view class="account-card" v-if="account">
      <view class="account-head">
        <view>
          <text class="account-name">{{ account.name }}</text>
          <text class="account-type">{{ getAccountTypeName(account.accountType) }}</text>
        </view>
        <view class="account-mark" :style="getAccountMarkStyle(account)">
          <text>{{ getIconText(account.name) }}</text>
        </view>
      </view>

      <text :class="['balance', { liability: account.isLiability }]">
        {{ account.isLiability ? '-' : '' }}{{ formatMoney(toBaseAmount(account)) }}
      </text>
      <text class="balance-label">折合 CNY 余额</text>
      <text class="currency-line" v-if="account.currency && account.currency !== 'CNY'">
        原币余额 {{ getCurrencySymbol(account.currency) }}{{ Number(account.currentBalance || 0).toFixed(2) }}
        · 汇率 {{ Number(account.exchangeRateToCny || 1).toFixed(4) }}
      </text>
      <view class="account-actions">
        <view class="adjust-action" @click="openAdjustSheet">
          <text class="adjust-action-title">调整余额</text>
          <text class="adjust-action-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="history-section">
      <view class="section-header">
        <text class="section-title">余额变化记录</text>
        <text class="section-subtitle">最近 {{ histories.length }} 条</text>
      </view>

      <view v-if="histories.length > 0" class="history-list">
        <view class="history-item" v-for="item in histories" :key="item.id">
          <view class="history-icon">
            <text>{{ item.changeAmount >= 0 ? '+' : '-' }}</text>
          </view>
          <view class="history-copy">
            <text class="history-title">{{ getChangeTitle(item.changeType) }}</text>
            <view class="history-flow">
              <text>{{ formatOriginalMoney(item.beforeBalance, item.currency) }}</text>
              <text class="flow-arrow">→</text>
              <text>{{ formatOriginalMoney(item.afterBalance, item.currency) }}</text>
            </view>
            <text class="history-date">{{ formatDateTime(item.createdAt) }}</text>
          </view>
          <text :class="['history-amount', { negative: Number(item.changeAmount) < 0 }]">
            {{ formatSignedOriginalMoney(item.changeAmount, item.currency) }}
          </text>
        </view>
      </view>

      <view class="empty-card" v-else>
        <text>暂无余额变化记录。</text>
      </view>
    </view>

    <view class="bottom-actions" v-if="account">
      <button class="action-btn secondary" @click="goEdit">设置</button>
      <button class="action-btn danger" @click="handleArchive">归档资产</button>
      <button class="action-btn danger solid" @click="handleDelete">删除账户</button>
    </view>

    <view v-if="showAdjustSheet" class="sheet-mask" @click="closeAdjustSheet">
      <view class="adjust-sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">调整当前余额</text>
          <text class="sheet-close" @click="closeAdjustSheet">×</text>
        </view>
        <text class="sheet-subtitle">
          当前余额 {{ formatOriginalMoney(account.currentBalance, account.currency) }}
        </text>
        <view class="adjust-input-wrap">
          <text class="adjust-symbol">{{ getCurrencySymbol(account.currency) }}</text>
          <input
            class="adjust-input"
            v-model="adjustBalance"
            type="digit"
            focus
            placeholder="请输入调整后的金额"
          />
        </view>
        <button class="adjust-submit" :disabled="!canSubmitAdjust || isAdjusting" @click="submitAdjust">
          {{ isAdjusting ? '调整中' : '确认调整' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { formatMoney, getAccountTypeName, getCurrencySymbol, toBaseAmount } from '../../utils/money'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      accountId: null,
      account: null,
      histories: [],
      isLoading: false,
      showAdjustSheet: false,
      adjustBalance: '',
      isAdjusting: false,
      themeVars: getThemeVars()
    }
  },
  computed: {
    canSubmitAdjust() {
      return /^(0|[1-9]\d{0,14})(\.\d{1,4})?$/.test(String(this.adjustBalance).trim())
    }
  },
  onLoad(options) {
    this.accountId = Number(options.id)
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.refreshData()
  },
  methods: {
    formatMoney,
    getAccountTypeName,
    getCurrencySymbol,
    toBaseAmount,
    async refreshData() {
      if (!this.accountId || this.isLoading) return
      this.isLoading = true
      try {
        const assetStore = useAssetStore()
        const [accountRes, historyRes] = await Promise.all([
          assetStore.fetchAccount(this.accountId),
          assetStore.fetchAccountHistory(this.accountId)
        ])
        if (accountRes.code === 200) this.account = accountRes.data
        if (historyRes.code === 200) this.histories = Array.isArray(historyRes.data) ? historyRes.data : []
      } finally {
        this.isLoading = false
      }
    },
    getIconText(name) {
      return name ? name.substring(0, 1) : '?'
    },
    getAccountMarkStyle(account) {
      return {
        color: 'var(--app-primary, #d3a414)',
        backgroundColor: 'var(--app-soft-bg, rgba(211, 164, 20, 0.12))',
        borderColor: 'var(--app-border, rgba(211, 164, 20, 0.20))'
      }
    },
    getChangeTitle(type) {
      if (type === 'CREATE') return '创建账户'
      if (type === 'MANUAL_ADJUST') return '手动调整余额'
      if (type === 'TRANSACTION_INCOME') return '收入入账'
      if (type === 'TRANSACTION_EXPENSE') return '支出扣款'
      if (type === 'TRANSACTION_TRANSFER_OUT') return '转账转出'
      if (type === 'TRANSACTION_TRANSFER_IN') return '转账转入'
      if (type === 'REVERSE_INCOME') return '撤销收入'
      if (type === 'REVERSE_EXPENSE') return '撤销支出'
      if (type === 'REVERSE_TRANSFER_OUT') return '撤销转出'
      if (type === 'REVERSE_TRANSFER_IN') return '撤销转入'
      return '余额变化'
    },
    formatOriginalMoney(value, currency) {
      return `${getCurrencySymbol(currency)}${Number(value || 0).toFixed(2)}`
    },
    formatSignedOriginalMoney(value, currency) {
      const amount = Number(value || 0)
      const sign = amount > 0 ? '+' : amount < 0 ? '-' : ''
      return `${sign}${this.formatOriginalMoney(Math.abs(amount), currency)}`
    },
    formatDateTime(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const month = date.getMonth() + 1
      const day = date.getDate()
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      return `${month}月${day}日 ${hour}:${minute}`
    },
    goEdit() {
      uni.navigateTo({ url: `/pages/account/form?id=${this.accountId}` })
    },
    openAdjustSheet() {
      this.adjustBalance = this.account ? String(this.account.currentBalance || '') : ''
      this.showAdjustSheet = true
    },
    closeAdjustSheet() {
      if (this.isAdjusting) return
      this.showAdjustSheet = false
    },
    async submitAdjust() {
      if (!this.canSubmitAdjust) {
        uni.showToast({ title: '请输入正确金额', icon: 'none' })
        return
      }
      this.isAdjusting = true
      try {
        const assetStore = useAssetStore()
        const res = await assetStore.updateAccount(this.accountId, {
          ...this.account,
          currentBalance: Number(String(this.adjustBalance).trim())
        })
        if (res && res.code === 200) {
          this.showAdjustSheet = false
          uni.showToast({ title: '余额已调整', icon: 'success' })
          await this.refreshData()
        }
      } finally {
        this.isAdjusting = false
      }
    },
    handleArchive() {
      uni.showModal({
        title: '确认归档',
        content: '归档后该账户不再显示在首页，也不参与资产统计。',
        success: async (result) => {
          if (!result.confirm) return
          const assetStore = useAssetStore()
          const res = await assetStore.archiveAccount(this.accountId, true)
          if (res && res.code === 200) {
            uni.showToast({ title: '已归档', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 800)
          }
        }
      })
    },
    handleDelete() {
      uni.showModal({
        title: '删除账户',
        content: '确认永久删除该账户？删除后账户和余额历史将不再显示。',
        confirmColor: '#d94a62',
        success: async (result) => {
          if (!result.confirm) return
          const assetStore = useAssetStore()
          const res = await assetStore.deleteAccount(this.accountId)
          if (res && res.code === 200) {
            uni.showToast({ title: '账户已删除', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 800)
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 28rpx 24rpx calc(150rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.account-card,
.history-section {
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

.account-card {
  padding: 34rpx;
  margin-bottom: 26rpx;
}

.account-head,
.section-header,
.history-item,
.bottom-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.account-name {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 38rpx;
  line-height: 50rpx;
  font-weight: 800;
}

.account-type,
.balance-label,
.currency-line,
.section-subtitle,
.history-sub,
.history-date {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.account-mark {
  width: 84rpx;
  height: 84rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-primary, #d3a414);
  font-size: 34rpx;
  font-weight: 800;
  border: 1rpx solid transparent;
}

.balance {
  display: block;
  margin-top: 40rpx;
  color: var(--app-text, #17202a);
  font-size: 48rpx;
  line-height: 60rpx;
  font-weight: 850;
  word-break: break-all;
}

.balance.liability {
  color: var(--app-liability-color, #d94a62);
}

.balance-label,
.currency-line {
  display: block;
}

.currency-line {
  margin-top: 10rpx;
}

.account-actions {
  margin-top: 34rpx;
}

.adjust-action {
  height: 82rpx;
  padding: 0 22rpx 0 26rpx;
  border-radius: 18rpx;
  background: var(--app-input-bg, #f6f8fb);
  border: 1rpx solid var(--app-border, #edf1f4);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.adjust-action-title {
  color: var(--app-text, #17202a);
  font-size: 28rpx;
  font-weight: 700;
}

.adjust-action-arrow {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: var(--app-soft-bg, #eef5f2);
  color: var(--app-primary, #d3a414);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  line-height: 40rpx;
  font-weight: 300;
}

.history-section {
  padding: 30rpx;
}

.section-header {
  align-items: flex-start;
  margin-bottom: 24rpx;
}

.section-title {
  color: var(--app-text, #17202a);
  font-size: 32rpx;
  font-weight: 750;
}

.history-item {
  align-items: flex-start;
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.history-item:last-child {
  border-bottom: none;
}

.history-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-soft-bg, #eef5f2);
  color: var(--app-primary, #d3a414);
  font-size: 30rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.history-copy {
  flex: 1;
  min-width: 0;
}

.history-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 700;
  line-height: 40rpx;
}

.history-sub,
.history-date {
  display: block;
  margin-top: 4rpx;
}

.history-flow {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 8rpx;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  line-height: 34rpx;
}

.flow-arrow {
  color: var(--app-faint, #b0bac5);
}

.history-amount {
  color: var(--app-positive-color, #d3a414);
  font-size: 30rpx;
  font-weight: 750;
  flex-shrink: 0;
}

.history-amount.negative {
  color: var(--app-liability-color, #d94a62);
}

.empty-card {
  padding: 44rpx 0;
  text-align: center;
  color: var(--app-muted, #7b8798);
  font-size: 26rpx;
}

.bottom-actions {
  position: fixed;
  left: 50%;
  bottom: 0;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: var(--app-page-bg, #f3f6f8);
  border-top: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: 0 -6rpx 18rpx rgba(15, 23, 42, 0.06);
  z-index: 20;
}

.action-btn {
  flex: 1;
  margin: 0;
  height: 82rpx;
  line-height: normal;
  border-radius: 14rpx;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  padding: 0 8rpx;
}

.action-btn.secondary {
  background: var(--app-soft-bg, #eef5f2);
  color: var(--app-text, #17202a);
  border: 1rpx solid var(--app-border, #edf1f4);
}

.action-btn.danger {
  background: var(--app-card-bg-alt, #ffffff);
  color: var(--app-danger, #d94a62);
  border: 1rpx solid var(--app-border, #edf1f4);
}

.action-btn.danger.solid {
  background: var(--app-danger, #d94a62);
  color: #ffffff !important;
  border: none;
}

.sheet-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 50;
  background: rgba(15, 23, 42, 0.48);
  display: flex;
  align-items: flex-end;
}

.adjust-sheet {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  padding: 38rpx 32rpx calc(42rpx + env(safe-area-inset-bottom));
  border-radius: 34rpx 34rpx 0 0;
  background: var(--app-card-bg, #ffffff);
  box-sizing: border-box;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.sheet-title {
  color: var(--app-text, #17202a);
  font-size: 36rpx;
  line-height: 48rpx;
  font-weight: 800;
}

.sheet-close {
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-soft-bg, #f3f5f7);
  color: var(--app-text, #17202a);
  font-size: 44rpx;
  line-height: 52rpx;
}

.sheet-subtitle {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 26rpx;
  line-height: 36rpx;
  margin-bottom: 34rpx;
}

.adjust-input-wrap {
  display: flex;
  align-items: center;
  height: 96rpx;
  padding: 0 28rpx;
  border-radius: 18rpx;
  background: var(--app-input-bg, #f6f8fb);
  border: 2rpx solid var(--app-border, #e6edf2);
  margin-bottom: 34rpx;
}

.adjust-symbol {
  color: var(--app-text, #17202a);
  font-size: 34rpx;
  font-weight: 800;
  margin-right: 16rpx;
}

.adjust-input {
  flex: 1;
  min-width: 0;
  color: var(--app-text, #17202a);
  font-size: 34rpx;
}

.adjust-submit {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-size: 31rpx;
  font-weight: 700;
}

.adjust-submit[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
}

@media (max-width: 480px) {
  .bottom-actions {
    left: 0;
    max-width: none;
    transform: none;
  }
}
</style>
