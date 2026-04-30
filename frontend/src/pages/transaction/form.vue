<template>
  <view class="container" :style="themeVars">
    <view class="type-tabs">
      <view
        v-for="item in typeOptions"
        :key="item.value"
        class="type-tab"
        :class="{ active: form.transactionType === item.value }"
        @click="selectType(item.value)"
      >
        <text>{{ item.label }}</text>
      </view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="form-label">{{ form.transactionType === 'TRANSFER' ? '转出账户' : '账户' }}</text>
        <picker :range="accountOptions" range-key="name" :value="accountIndex" @change="selectAccount">
          <view class="form-picker">
            <text>{{ accountOptions[accountIndex] ? accountOptions[accountIndex].name : '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item" v-if="form.transactionType === 'TRANSFER'">
        <text class="form-label">转入账户</text>
        <picker :range="accountOptions" range-key="name" :value="targetAccountIndex" @change="selectTargetAccount">
          <view class="form-picker">
            <text>{{ accountOptions[targetAccountIndex] ? accountOptions[targetAccountIndex].name : '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item" v-if="form.transactionType !== 'TRANSFER'">
        <text class="form-label">分类</text>
        <picker :range="categoryOptions" range-key="name" :value="categoryIndex" @change="selectCategory">
          <view class="form-picker">
            <text>{{ categoryOptions[categoryIndex] ? categoryOptions[categoryIndex].name : '请选择分类' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">金额</text>
        <input class="form-input" v-model="form.amount" type="digit" placeholder="0.00" />
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <input class="form-input" v-model="form.remark" type="text" confirm-type="done" maxlength="200" placeholder="选填" />
      </view>
    </view>

    <button class="save-btn" :disabled="isSaving" @click="handleSave">
      {{ isSaving ? '保存中' : (recordId ? '保存修改' : '保存记录') }}
    </button>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'
import { useTransactionStore } from '../../store/transaction'
import { getThemeVars } from '../../utils/theme'

const MONEY_PATTERN = /^(0|[1-9]\d{0,14})(\.\d{1,4})?$/
const PREFERENCE_KEY = 'transaction_quick_preferences'

export default {
  data() {
    return {
      form: {
        transactionType: 'EXPENSE',
        accountId: null,
        targetAccountId: null,
        categoryId: null,
        amount: '',
        currency: 'CNY',
        exchangeRateToCny: 1,
        occurredAt: '',
        remark: ''
      },
      recordId: null,
      originalRecord: null,
      typeOptions: [
        { label: '支出', value: 'EXPENSE' },
        { label: '收入', value: 'INCOME' },
        { label: '转账', value: 'TRANSFER' }
      ],
      accountOptions: [],
      categoryOptions: [],
      accountIndex: 0,
      targetAccountIndex: 0,
      categoryIndex: 0,
      isSaving: false,
      themeVars: getThemeVars()
    }
  },
  onLoad(options = {}) {
    this.recordId = options.id ? Number(options.id) : null
    this.themeVars = getThemeVars()
    this.refreshData()
  },
  methods: {
    async refreshData() {
      const assetStore = useAssetStore()
      const transactionStore = useTransactionStore()
      await assetStore.fetchAccounts()
      this.accountOptions = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : []
      if (this.accountOptions.length > 0) {
        this.applyPreferredAccounts()
        if (this.accountOptions.length > 1) {
          this.applyPreferredTargetAccount()
        }
      }
      await transactionStore.fetchCategories(this.form.transactionType)
      this.categoryOptions = this.buildCategoryOptions(transactionStore.categories)
      if (this.categoryOptions.length > 0) {
        this.form.categoryId = this.categoryOptions[0].id
      }
      if (!this.recordId) {
        this.applyPreferredCategory()
      }
      if (this.recordId) {
        await this.loadRecord()
      }
    },
    async loadRecord() {
      const transactionStore = useTransactionStore()
      await transactionStore.fetchRecords(200)
      const record = transactionStore.records.find(item => Number(item.id) === Number(this.recordId))
      if (!record) {
        uni.showToast({ title: '记录不存在', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 600)
        return
      }
      this.originalRecord = record
      this.form.transactionType = record.transactionType
      this.form.accountId = record.accountId
      this.form.targetAccountId = record.targetAccountId
      this.form.categoryId = record.categoryId
      this.form.amount = String(record.amount || '')
      this.form.currency = record.currency || 'CNY'
      this.form.exchangeRateToCny = record.exchangeRateToCny || 1
      this.form.occurredAt = record.occurredAt || ''
      this.form.remark = record.remark || ''
      this.accountIndex = Math.max(0, this.accountOptions.findIndex(item => item.id === record.accountId))
      this.targetAccountIndex = Math.max(0, this.accountOptions.findIndex(item => item.id === record.targetAccountId))
      await transactionStore.fetchCategories(this.form.transactionType)
      this.categoryOptions = this.buildCategoryOptions(transactionStore.categories)
      this.categoryIndex = Math.max(0, this.categoryOptions.findIndex(item => item.id === record.categoryId))
    },
    async selectType(type) {
      this.form.transactionType = type
      this.form.categoryId = null
      const transactionStore = useTransactionStore()
      await transactionStore.fetchCategories(type)
      this.categoryOptions = this.buildCategoryOptions(transactionStore.categories)
      this.categoryIndex = 0
      if (this.categoryOptions.length > 0) {
        this.form.categoryId = this.categoryOptions[0].id
      }
      this.applyPreferredAccounts()
      this.applyPreferredCategory()
    },
    selectAccount(event) {
      this.accountIndex = Number(event.detail.value)
      this.applyAccount(this.accountOptions[this.accountIndex], false)
      if (this.form.transactionType === 'TRANSFER' && this.form.accountId === this.form.targetAccountId) {
        this.applyPreferredTargetAccount()
      }
    },
    selectTargetAccount(event) {
      this.targetAccountIndex = Number(event.detail.value)
      this.form.targetAccountId = this.accountOptions[this.targetAccountIndex].id
    },
    selectCategory(event) {
      this.categoryIndex = Number(event.detail.value)
      this.form.categoryId = this.categoryOptions[this.categoryIndex].id
    },
    buildCategoryOptions(categories = []) {
      if (this.form.transactionType === 'TRANSFER') return []
      const list = Array.isArray(categories) ? [...categories] : []
      const fallbackName = this.form.transactionType === 'INCOME' ? '其他收入' : '其他支出'
      const hasFallback = list.some(item => item && item.name === fallbackName)
      return hasFallback
        ? list
        : [...list, { id: null, name: fallbackName, type: this.form.transactionType, colorHex: '#94A3B8', fallback: true }]
    },
    applyAccount(account) {
      if (!account) return
      this.form.accountId = account.id
      this.form.currency = account.currency || 'CNY'
      this.form.exchangeRateToCny = account.exchangeRateToCny || 1
    },
    getPreferences() {
      try {
        const value = uni.getStorageSync(PREFERENCE_KEY)
        return value && typeof value === 'object' ? value : {}
      } catch (_) {
        return {}
      }
    },
    savePreferences(preferences) {
      try {
        uni.setStorageSync(PREFERENCE_KEY, preferences || {})
      } catch (_) {}
    },
    findAccountIndex(id) {
      return this.accountOptions.findIndex(item => Number(item.id) === Number(id))
    },
    applyPreferredAccounts() {
      if (!this.accountOptions.length) return
      const preferences = this.getPreferences()
      const current = preferences[this.form.transactionType] || {}
      const preferredIndex = this.findAccountIndex(current.accountId)
      this.accountIndex = preferredIndex >= 0 ? preferredIndex : 0
      this.applyAccount(this.accountOptions[this.accountIndex])
      if (this.form.transactionType === 'TRANSFER') {
        this.applyPreferredTargetAccount()
      }
    },
    applyPreferredTargetAccount() {
      if (this.accountOptions.length < 2) return
      const preferences = this.getPreferences()
      const current = preferences.TRANSFER || {}
      const preferredIndex = this.findAccountIndex(current.targetAccountId)
      const fallbackIndex = this.accountOptions.findIndex(item => Number(item.id) !== Number(this.form.accountId))
      this.targetAccountIndex = preferredIndex >= 0 && Number(this.accountOptions[preferredIndex].id) !== Number(this.form.accountId)
        ? preferredIndex
        : Math.max(0, fallbackIndex)
      this.form.targetAccountId = this.accountOptions[this.targetAccountIndex]
        ? this.accountOptions[this.targetAccountIndex].id
        : null
    },
    applyPreferredCategory() {
      if (this.form.transactionType === 'TRANSFER' || !this.categoryOptions.length) return
      const preferences = this.getPreferences()
      const current = preferences[this.form.transactionType] || {}
      const preferredIndex = this.categoryOptions.findIndex(item => Number(item.id) === Number(current.categoryId))
      if (preferredIndex >= 0) {
        this.categoryIndex = preferredIndex
        this.form.categoryId = this.categoryOptions[preferredIndex].id
      }
    },
    rememberCurrentPreference() {
      const preferences = this.getPreferences()
      preferences[this.form.transactionType] = {
        accountId: this.form.accountId,
        targetAccountId: this.form.transactionType === 'TRANSFER' ? this.form.targetAccountId : null,
        categoryId: this.form.transactionType === 'TRANSFER' ? null : this.form.categoryId,
        updatedAt: Date.now()
      }
      this.savePreferences(preferences)
    },
    validateForm() {
      if (!this.form.accountId) return '请选择账户'
      if (this.form.transactionType === 'TRANSFER' && !this.form.targetAccountId) return '请选择转入账户'
      if (this.form.transactionType === 'TRANSFER' && this.form.accountId === this.form.targetAccountId) return '转出和转入账户不能相同'
      if (!String(this.form.amount).trim()) return '请输入金额'
      if (!MONEY_PATTERN.test(String(this.form.amount).trim())) return '金额格式不正确'
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
        const store = useTransactionStore()
        const payload = {
          ...this.form,
          amount: Number(String(this.form.amount).trim()),
          categoryId: this.form.transactionType === 'TRANSFER' ? null : this.form.categoryId,
          targetAccountId: this.form.transactionType === 'TRANSFER' ? this.form.targetAccountId : null,
          occurredAt: this.form.occurredAt || new Date().toISOString().slice(0, 19)
        }
        const res = this.recordId
          ? await store.updateRecord(this.recordId, payload)
          : await store.createRecord(payload)
        if (res && res.code === 200) {
          this.rememberCurrentPreference()
          uni.showToast({ title: '已保存', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 700)
        }
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
  padding: 28rpx 24rpx calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.type-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6rpx;
  padding: 6rpx;
  border-radius: 16rpx;
  background: var(--app-soft-bg, #eef2f5);
  margin-bottom: 24rpx;
}

.type-tab {
  height: 68rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-muted, #64748b);
  font-size: 28rpx;
  font-weight: 700;
}

.type-tab.active {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.16);
}

.form-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  padding: 10rpx 30rpx;
  box-shadow: var(--app-shadow, 0 12rpx 30rpx rgba(26, 42, 58, 0.06));
  margin-bottom: 30rpx;
}

.form-item {
  min-height: 94rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  min-width: 0;
  text-align: right;
  color: var(--app-muted, #334155);
  font-size: 30rpx;
}

.form-picker {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  color: var(--app-muted, #334155);
  font-size: 30rpx;
}

.picker-arrow {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-primary, #d3a414);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  line-height: 40rpx;
  font-weight: 300;
}

.save-btn {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 750;
}
</style>
