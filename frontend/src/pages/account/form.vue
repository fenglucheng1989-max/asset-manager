<template>
  <view class="container" :style="themeVars">
    <view class="page-head">
      <text class="page-title">{{ isEdit ? '编辑账户' : '新增账户' }}</text>
    </view>
    <view class="account-kind-tabs">
      <view :class="['kind-tab', { active: accountKind === 'ASSET' }]" @click="selectKind('ASSET')">资产</view>
      <view :class="['kind-tab', { active: accountKind === 'LIABILITY' }]" @click="selectKind('LIABILITY')">负债</view>
    </view>

    <view class="form-card">
      <text class="group-title">基础信息</text>
      <view class="form-item">
        <text class="form-label">账户名称</text>
        <input
          class="form-input"
          v-model="form.name"
          maxlength="50"
          placeholder="请输入账户名称"
          @input="markDirty"
        />
      </view>
      <text class="field-hint">{{ nameLength }}/50</text>

      <view class="form-item type-item">
        <text class="form-label">账户类型</text>
        <picker class="field-picker" :range="filteredTypeOptions" range-key="label" :value="typeIndex" @change="selectType">
          <view class="form-picker">
            <text>{{ filteredTypeOptions[typeIndex] ? filteredTypeOptions[typeIndex].label : '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <view class="form-card">
      <text class="group-title">金额与币种</text>
      <view class="form-item">
        <text class="form-label">当前余额</text>
        <input
          class="form-input"
          v-model="form.currentBalance"
          type="digit"
          placeholder="最多4位小数"
          @input="markDirty"
        />
      </view>
      <text class="field-hint">支持非负金额，最多保留4位小数。</text>

      <view class="form-item">
        <text class="form-label">币种</text>
        <picker class="field-picker" :range="currencyOptions" range-key="label" :value="currencyIndex" @change="selectCurrency">
          <view class="form-picker">
            <text>{{ currencyOptions[currencyIndex].label }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item" v-if="form.currency !== 'CNY'">
        <text class="form-label">折算汇率</text>
        <input
          class="form-input"
          v-model="form.exchangeRateToCny"
          type="digit"
          placeholder="1外币可折算多少人民币"
          @input="markDirty"
        />
      </view>
      <text class="field-hint" v-if="form.currency !== 'CNY'">用于总资产、净资产和快照折算为 CNY。</text>
    </view>

    <view class="form-card">
      <text class="group-title">统计与展示</text>
      <view class="form-item">
        <text class="form-label">计入净资产</text>
        <switch :checked="form.includeInTotal" @change="handleSwitch('includeInTotal', $event)" :color="switchColor" />
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <input
          class="form-input remark-input"
          v-model="form.remark"
          maxlength="200"
          placeholder="选填"
          @input="markDirty"
        />
      </view>
      <text class="field-hint">{{ remarkLength }}/200</text>
    </view>

    <view class="btn-group">
      <button class="btn-save" :disabled="isSaving" @click="handleSave">
        {{ isSaving ? '保存中' : '保存' }}
      </button>
      <button class="btn-delete" v-if="isEdit" @click="handleArchive">归档账户</button>
    </view>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { useUserStore } from '../../store/user'
import { getResolvedTheme, getThemeMode, getThemeVars } from '../../utils/theme'

const MONEY_PATTERN = /^(0|[1-9]\d{0,14})(\.\d{1,4})?$/

const ACCOUNT_COLORS = {
  BANK: '#5B8FF9',
  CASH: '#2EBD85',
  E_WALLET: '#00BCD4',
  INVESTMENT: '#FFC107',
  REAL_ESTATE: '#607D8B',
  OTHER_ASSET: '#8B5CF6',
  CREDIT_CARD: '#FF6B6B',
  LOAN: '#FF6B6B',
  OTHER_LIABILITY: '#FF6B6B'
}

export default {
  data() {
    return {
      isEdit: false,
      isSaving: false,
      isDirty: false,
      hasSaved: false,
      accountId: null,
      initialSnapshot: '',
      form: {
        name: '',
        accountType: 'BANK',
        currency: 'CNY',
        exchangeRateToCny: '1',
        currentBalance: '',
        isLiability: false,
        includeInTotal: true,
        icon: '',
        colorHex: '#2EBD85',
        remark: ''
      },
      accountKind: 'ASSET',
      typeIndex: 0,
      typeOptions: [
        { label: '银行卡', value: 'BANK', kind: 'ASSET' },
        { label: '现金', value: 'CASH', kind: 'ASSET' },
        { label: '电子钱包', value: 'E_WALLET', kind: 'ASSET' },
        { label: '投资理财', value: 'INVESTMENT', kind: 'ASSET' },
        { label: '房产', value: 'REAL_ESTATE', kind: 'ASSET' },
        { label: '其他资产', value: 'OTHER_ASSET', kind: 'ASSET' },
        { label: '信用卡', value: 'CREDIT_CARD', kind: 'LIABILITY' },
        { label: '贷款', value: 'LOAN', kind: 'LIABILITY' },
        { label: '其他负债', value: 'OTHER_LIABILITY', kind: 'LIABILITY' }
      ],
      currencyOptions: [
        { label: '人民币 CNY', value: 'CNY', rate: '1' },
        { label: '美元 USD', value: 'USD', rate: '7.20' },
        { label: '港币 HKD', value: 'HKD', rate: '0.92' },
        { label: '欧元 EUR', value: 'EUR', rate: '7.80' },
        { label: '日元 JPY', value: 'JPY', rate: '0.05' },
        { label: '英镑 GBP', value: 'GBP', rate: '9.10' }
      ],
      currencyIndex: 0,
      themeVars: getThemeVars()
    }
  },
  computed: {
    nameLength() {
      return this.form.name.length
    },
    remarkLength() {
      return this.form.remark.length
    },
    filteredTypeOptions() {
      return this.typeOptions.filter(item => item.kind === this.accountKind)
    },
    switchColor() {
      return getResolvedTheme(getThemeMode()).primary
    }
  },
  onLoad(options) {
    if (options.id) {
      this.isEdit = true
      this.accountId = Number(options.id)
      this.loadAccount()
    } else {
      this.applyDefaultCurrency()
    }
    this.captureInitialSnapshot()
  },
  onShow() {
    this.themeVars = getThemeVars()
  },
  onBackPress() {
    if (!this.hasUnsavedChanges()) return false
    uni.showModal({
      title: '放弃编辑？',
      content: '当前修改尚未保存，确定要离开吗？',
      success: (result) => {
        if (result.confirm) {
          this.hasSaved = true
          uni.navigateBack()
        }
      }
    })
    return true
  },
  methods: {
    loadAccount() {
      const assetStore = useAssetStore()
      const account = assetStore.accounts.find(item => item.id === this.accountId)
      if (!account) return

      this.form = {
        name: account.name,
        accountType: account.accountType,
        currency: account.currency || 'CNY',
        exchangeRateToCny: String(account.exchangeRateToCny || 1),
        currentBalance: String(account.currentBalance),
        isLiability: account.isLiability,
        includeInTotal: account.includeInTotal,
        icon: account.icon || '',
        colorHex: account.colorHex || '#2EBD85',
        remark: account.remark || ''
      }
      this.typeIndex = this.typeOptions.findIndex(item => item.value === account.accountType)
      const currentType = this.typeOptions.find(item => item.value === account.accountType)
      this.accountKind = account.isLiability || (currentType && currentType.kind === 'LIABILITY') ? 'LIABILITY' : 'ASSET'
      this.typeIndex = this.filteredTypeOptions.findIndex(item => item.value === account.accountType)
      if (this.typeIndex < 0) this.typeIndex = 0
      this.currencyIndex = this.currencyOptions.findIndex(item => item.value === this.form.currency)
      if (this.currencyIndex < 0) this.currencyIndex = 0
    },
    async applyDefaultCurrency() {
      try {
        const userStore = useUserStore()
        if (!userStore.profile && userStore.isLoggedIn) {
          await userStore.fetchProfile()
        }
        const baseCurrency = userStore.profile && userStore.profile.baseCurrency ? userStore.profile.baseCurrency : 'CNY'
        const index = this.currencyOptions.findIndex(item => item.value === baseCurrency)
        if (index >= 0) {
          this.currencyIndex = index
          this.form.currency = this.currencyOptions[index].value
          this.form.exchangeRateToCny = this.currencyOptions[index].rate
        }
      } finally {
        this.captureInitialSnapshot()
      }
    },
    captureInitialSnapshot() {
      this.initialSnapshot = JSON.stringify(this.normalizedForm())
      this.isDirty = false
    },
    normalizedForm() {
      return {
        ...this.form,
        name: this.form.name.trim(),
        currency: this.form.currency,
        exchangeRateToCny: String(this.form.exchangeRateToCny).trim(),
        currentBalance: String(this.form.currentBalance).trim(),
        remark: this.form.remark.trim()
      }
    },
    hasUnsavedChanges() {
      return !this.hasSaved && JSON.stringify(this.normalizedForm()) !== this.initialSnapshot
    },
    markDirty() {
      this.isDirty = this.hasUnsavedChanges()
    },
    handleSwitch(field, event) {
      this.form[field] = event.detail.value
      this.markDirty()
    },
    selectKind(kind) {
      this.accountKind = kind
      this.form.isLiability = kind === 'LIABILITY'
      this.typeIndex = 0
      this.form.accountType = this.filteredTypeOptions[0].value
      this.markDirty()
    },
    selectType(event) {
      this.typeIndex = Number(event.detail.value)
      this.form.accountType = this.filteredTypeOptions[this.typeIndex].value
      this.markDirty()
    },
    selectCurrency(event) {
      const index = Number(event.detail.value)
      this.currencyIndex = index
      const option = this.currencyOptions[index]
      this.form.currency = option.value
      if (option.value === 'CNY') {
        this.form.exchangeRateToCny = '1'
      } else if (!this.form.exchangeRateToCny || this.form.exchangeRateToCny === '1') {
        this.form.exchangeRateToCny = option.rate
      }
      this.markDirty()
    },
    validateForm() {
      const name = this.form.name.trim()
      const balance = String(this.form.currentBalance).trim()
      const exchangeRate = String(this.form.exchangeRateToCny).trim()

      if (!name) return '请输入账户名称'
      if (name.length > 50) return '账户名称不能超过50个字符'
      if (!balance) return '请输入当前余额'
      if (!MONEY_PATTERN.test(balance)) return '金额格式不正确，最多15位整数和4位小数'
      if (!exchangeRate) return '请输入折算汇率'
      if (!/^(0|[1-9]\d{0,5})(\.\d{1,8})?$/.test(exchangeRate)) return '折算汇率格式不正确'
      if (Number(exchangeRate) <= 0) return '折算汇率必须大于0'
      if (Number(balance) > 999999999999999) return '金额过大，请重新输入'
      if (this.form.remark.length > 200) return '备注不能超过200个字符'
      return ''
    },
    async handleSave() {
      const error = this.validateForm()
      if (error) {
        uni.showToast({ title: error, icon: 'none' })
        return
      }

      this.isSaving = true
      try {
        const assetStore = useAssetStore()
        const autoColor = !this.isEdit ? (ACCOUNT_COLORS[this.form.accountType] || '#607D8B') : this.form.colorHex
        const data = {
          ...this.form,
          name: this.form.name.trim(),
          colorHex: autoColor,
          currentBalance: Number(String(this.form.currentBalance).trim()),
          exchangeRateToCny: Number(String(this.form.exchangeRateToCny).trim()),
          remark: this.form.remark.trim()
        }

        const res = this.isEdit
          ? await assetStore.updateAccount(this.accountId, data)
          : await assetStore.createAccount(data)

        if (res && res.code === 200) {
          this.hasSaved = true
          uni.showToast({ title: '保存成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        }
      } finally {
        this.isSaving = false
      }
    },
    async handleArchive() {
      uni.showModal({
        title: '确认归档',
        content: '归档后账户不再显示在首页，也不参与总资产和净资产统计。',
        success: async (result) => {
          if (!result.confirm) return

          const assetStore = useAssetStore()
          const res = await assetStore.archiveAccount(this.accountId, true)
          if (res && res.code === 200) {
            this.hasSaved = true
            uni.showToast({ title: '已归档', icon: 'success' })
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
  padding: 28rpx 24rpx calc(144rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.page-head {
  padding: 8rpx 4rpx 22rpx;
}

.page-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 38rpx;
  line-height: 48rpx;
  font-weight: 850;
}

.account-kind-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8rpx;
  padding: 8rpx;
  margin-bottom: 22rpx;
  border-radius: 20rpx;
  background: var(--app-soft-bg, #eef2f5);
  border: 1rpx solid var(--app-border, #edf1f4);
}

.kind-tab {
  height: 64rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-faint, #94a3b8);
  font-size: 28rpx;
  font-weight: 600;
}

.kind-tab.active {
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  font-weight: 700;
}

.form-card {
  background: var(--app-card-bg, #ffffff);
  border-radius: 20rpx;
  padding: 22rpx 28rpx 10rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 10rpx 26rpx rgba(26,42,58,0.045));
  position: relative;
  z-index: 1;
}

.group-title {
  display: block;
  color: var(--app-primary, #d3a414);
  font-size: 23rpx;
  line-height: 34rpx;
  margin-bottom: 6rpx;
  font-weight: 800;
}

.form-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 90rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
  gap: 22rpx;
}

.form-item:last-child { border-bottom: none; }

.field-hint {
  display: block;
  margin: -2rpx 0 10rpx;
  padding-left: 160rpx;
  color: var(--app-muted, #7b8798);
  font-size: 22rpx;
  line-height: 32rpx;
  text-align: right;
}

.type-item {
  position: relative;
  z-index: 3;
}

.form-label {
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  width: 160rpx;
  flex-shrink: 0;
  font-weight: 650;
}

.form-input {
  width: 0;
  flex: 1;
  height: 56rpx;
  padding: 0;
  border: none !important;
  background: transparent !important;
  text-align: right;
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  min-width: 0;
  box-sizing: border-box;
}

.field-picker {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 4;
}

.remark-input {
  color: var(--app-text, #17202a);
}

.form-picker {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  height: 56rpx;
  padding: 0;
  border: none !important;
  background: transparent !important;
  font-size: 30rpx;
  color: var(--app-text, #17202a);
  min-width: 0;
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;
}

.picker-arrow {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-faint, #94a3b8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  line-height: 32rpx;
  font-weight: 400;
  flex-shrink: 0;
}

.btn-group {
  padding: 6rpx 4rpx 0;
}

.btn-save {
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  border-radius: 999rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
  font-weight: 700;
  border: 2rpx solid var(--app-primary, #d3a414);
  margin-bottom: 20rpx;
}

.btn-save[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
  border-color: var(--app-border, #e5e7eb);
}

.btn-delete {
  background: var(--app-card-bg, #ffffff);
  color: var(--app-danger, #d94a62);
  border: 2rpx solid var(--app-danger, #d94a62);
  border-radius: 999rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
}

.btn-delete[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
  border-color: var(--app-border, #e5e7eb);
}
</style>
