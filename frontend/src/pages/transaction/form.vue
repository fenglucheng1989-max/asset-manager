<template>
  <view class="container">
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

      <view class="inline-category" v-if="form.transactionType !== 'TRANSFER'">
        <view class="inline-category-head" @click="toggleCategoryCreator">
          <text>{{ showCategoryCreator || categoryOptions.length === 0 ? '新增分类' : '没有合适分类？' }}</text>
          <text class="inline-category-action">{{ showCategoryCreator || categoryOptions.length === 0 ? '收起' : '自定义' }}</text>
        </view>
        <view class="inline-category-form" v-if="showCategoryCreator || categoryOptions.length === 0">
          <input class="category-input" v-model="newCategoryName" maxlength="30" placeholder="例如：餐饮、工资" />
          <view class="category-color-row">
            <view
              v-for="color in categoryColors"
              :key="color"
              :class="['category-color', { active: newCategoryColor === color }]"
              :style="{ backgroundColor: color }"
              @click="newCategoryColor = color"
            ></view>
          </view>
          <button class="category-save-btn" :disabled="isCategorySaving" @click="createInlineCategory">
            {{ isCategorySaving ? '添加中' : '添加并选中' }}
          </button>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">金额</text>
        <input class="form-input" v-model="form.amount" type="digit" placeholder="0.00" />
      </view>

      <view class="form-item">
        <text class="form-label">标签</text>
        <input class="form-input" v-model="form.tag" maxlength="50" placeholder="选填" />
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <input class="form-input" v-model="form.remark" maxlength="200" placeholder="选填" />
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

const MONEY_PATTERN = /^(0|[1-9]\d{0,14})(\.\d{1,4})?$/

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
        tag: '',
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
      showCategoryCreator: false,
      newCategoryName: '',
      newCategoryColor: '#FF6B6B',
      isCategorySaving: false,
      categoryColors: ['#FF6B6B', '#2EBD85', '#5B8FF9', '#FFC107', '#00BCD4', '#8B5CF6', '#64748B']
    }
  },
  onLoad(options = {}) {
    this.recordId = options.id ? Number(options.id) : null
    this.refreshData()
  },
  methods: {
    async refreshData() {
      const assetStore = useAssetStore()
      const transactionStore = useTransactionStore()
      await assetStore.fetchAccounts()
      this.accountOptions = Array.isArray(assetStore.accounts) ? [...assetStore.accounts] : []
      if (this.accountOptions.length > 0) {
        this.applyAccount(this.accountOptions[0], false)
        if (this.accountOptions.length > 1) {
          this.targetAccountIndex = 1
          this.form.targetAccountId = this.accountOptions[1].id
        }
      }
      await transactionStore.fetchCategories(this.form.transactionType)
      this.categoryOptions = Array.isArray(transactionStore.categories) ? [...transactionStore.categories] : []
      if (this.categoryOptions.length > 0) {
        this.form.categoryId = this.categoryOptions[0].id
      }
      this.syncCategoryCreatorColor()
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
      this.form.tag = record.tag || ''
      this.form.remark = record.remark || ''
      this.accountIndex = Math.max(0, this.accountOptions.findIndex(item => item.id === record.accountId))
      this.targetAccountIndex = Math.max(0, this.accountOptions.findIndex(item => item.id === record.targetAccountId))
      await transactionStore.fetchCategories(this.form.transactionType)
      this.categoryOptions = Array.isArray(transactionStore.categories) ? [...transactionStore.categories] : []
      this.categoryIndex = Math.max(0, this.categoryOptions.findIndex(item => item.id === record.categoryId))
      this.syncCategoryCreatorColor()
    },
    async selectType(type) {
      this.form.transactionType = type
      this.form.categoryId = null
      const transactionStore = useTransactionStore()
      await transactionStore.fetchCategories(type)
      this.categoryOptions = Array.isArray(transactionStore.categories) ? [...transactionStore.categories] : []
      this.categoryIndex = 0
      if (this.categoryOptions.length > 0) {
        this.form.categoryId = this.categoryOptions[0].id
      }
      this.syncCategoryCreatorColor()
    },
    selectAccount(event) {
      this.accountIndex = Number(event.detail.value)
      this.applyAccount(this.accountOptions[this.accountIndex], false)
    },
    selectTargetAccount(event) {
      this.targetAccountIndex = Number(event.detail.value)
      this.form.targetAccountId = this.accountOptions[this.targetAccountIndex].id
    },
    selectCategory(event) {
      this.categoryIndex = Number(event.detail.value)
      this.form.categoryId = this.categoryOptions[this.categoryIndex].id
    },
    toggleCategoryCreator() {
      if (this.categoryOptions.length === 0) return
      this.showCategoryCreator = !this.showCategoryCreator
    },
    syncCategoryCreatorColor() {
      this.newCategoryColor = this.form.transactionType === 'INCOME' ? '#2EBD85' : '#FF6B6B'
    },
    async createInlineCategory() {
      const name = this.newCategoryName.trim()
      if (!name) {
        uni.showToast({ title: '请输入分类名称', icon: 'none' })
        return
      }
      this.isCategorySaving = true
      try {
        const transactionStore = useTransactionStore()
        const res = await transactionStore.createCategory({
          name,
          type: this.form.transactionType,
          colorHex: this.newCategoryColor,
          sortOrder: 100
        })
        if (res && res.code === 200) {
          this.categoryOptions = Array.isArray(transactionStore.categories) ? [...transactionStore.categories] : []
          const createdIndex = this.categoryOptions.findIndex(item => item.name === name)
          this.categoryIndex = createdIndex >= 0 ? createdIndex : Math.max(0, this.categoryOptions.length - 1)
          if (this.categoryOptions[this.categoryIndex]) {
            this.form.categoryId = this.categoryOptions[this.categoryIndex].id
          }
          this.newCategoryName = ''
          this.showCategoryCreator = false
          uni.showToast({ title: '分类已添加', icon: 'success' })
        }
      } finally {
        this.isCategorySaving = false
      }
    },
    applyAccount(account) {
      if (!account) return
      this.form.accountId = account.id
      this.form.currency = account.currency || 'CNY'
      this.form.exchangeRateToCny = account.exchangeRateToCny || 1
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
}

.type-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6rpx;
  padding: 6rpx;
  border-radius: 16rpx;
  background: #eef2f5;
  margin-bottom: 24rpx;
}

.type-tab {
  height: 68rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 28rpx;
  font-weight: 700;
}

.type-tab.active {
  background: #17202a;
  color: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(23, 32, 42, 0.16);
}

.form-card {
  background: #ffffff;
  border: 1rpx solid #edf1f4;
  border-radius: 20rpx;
  padding: 10rpx 30rpx;
  box-shadow: 0 12rpx 30rpx rgba(26, 42, 58, 0.06);
  margin-bottom: 30rpx;
}

.form-item {
  min-height: 94rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  border-bottom: 1rpx solid #edf1f4;
}

.form-item:last-child {
  border-bottom: none;
}

.inline-category {
  margin: 18rpx 0 10rpx;
  border-radius: 16rpx;
  background: #f6f8fb;
  overflow: hidden;
}

.inline-category-head {
  min-height: 72rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  color: #17202a;
  font-size: 26rpx;
  font-weight: 750;
}

.inline-category-action {
  color: #226f63;
  flex-shrink: 0;
}

.inline-category-form {
  padding: 0 22rpx 22rpx;
}

.category-input {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  background: #ffffff;
  color: #334155;
  font-size: 28rpx;
  box-sizing: border-box;
}

.category-color-row {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
  padding: 20rpx 0;
}

.category-color {
  width: 46rpx;
  height: 46rpx;
  border-radius: 14rpx;
  border: 4rpx solid transparent;
  box-sizing: border-box;
}

.category-color.active {
  border-color: #17202a;
}

.category-save-btn {
  height: 74rpx;
  line-height: 74rpx;
  border-radius: 999rpx;
  background: #17202a;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 750;
}

.form-label {
  color: #17202a;
  font-size: 30rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  min-width: 0;
  text-align: right;
  color: #334155;
  font-size: 30rpx;
}

.form-picker {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  color: #334155;
  font-size: 30rpx;
}

.picker-arrow {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #f2f6f4;
  color: #226f63;
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
  background: #17202a;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 750;
}
</style>
