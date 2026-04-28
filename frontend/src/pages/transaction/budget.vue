<template>
  <view class="container">
    <view class="month-card">
      <text class="month-switch" @click="changeMonth(-1)">‹</text>
      <view>
        <text class="month-title">{{ currentMonth }}</text>
        <text class="month-sub">预算只统计支出流水</text>
      </view>
      <text class="month-switch" @click="changeMonth(1)">›</text>
    </view>

    <view class="form-card">
      <view class="form-row">
        <text class="form-label">分类</text>
        <picker :range="categoryOptions" range-key="name" :value="categoryIndex" @change="selectCategory">
          <view class="picker-text">{{ categoryOptions[categoryIndex] ? categoryOptions[categoryIndex].name : '全部支出' }}</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">金额</text>
        <input class="form-input" v-model="amount" type="digit" placeholder="0.00" />
      </view>
      <view class="form-row">
        <text class="form-label">预警</text>
        <picker :range="warningOptions" range-key="label" :value="warningIndex" @change="selectWarning">
          <view class="picker-text">{{ warningOptions[warningIndex].label }}</view>
        </picker>
      </view>
      <button class="save-btn" @click="saveBudget">保存预算</button>
    </view>

    <view class="budget-list">
      <view class="section-header">
        <text class="section-title">本月预算</text>
        <text class="section-subtitle">{{ budgets.length }} 项</text>
      </view>
      <view v-if="budgets.length">
        <view class="budget-item" v-for="item in budgets" :key="item.id" @longpress="deleteBudget(item)">
          <view class="budget-head">
            <view>
              <text class="budget-name">{{ item.categoryName }}</text>
              <text :class="['budget-state', { warning: item.warning }]">{{ item.warning ? '接近或超过预警线' : '使用正常' }}</text>
            </view>
            <text class="budget-money">{{ formatMoney(item.usedAmount) }} / {{ formatMoney(item.amount) }}</text>
          </view>
          <view class="progress-track">
            <view :class="['progress-fill', { warning: item.warning }]" :style="{ width: budgetPercent(item) }"></view>
          </view>
          <text class="remaining">剩余 {{ formatMoney(item.remainingAmount) }}</text>
        </view>
      </view>
      <view class="empty-card" v-else>
        <text>还没有预算，先设置一项本月支出上限。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useTransactionStore } from '../../store/transaction'
import { formatMoney } from '../../utils/money'

export default {
  data() {
    return {
      currentMonth: '',
      budgets: [],
      categories: [],
      categoryIndex: 0,
      amount: '',
      warningIndex: 1,
      warningOptions: [
        { label: '70%', value: 0.7 },
        { label: '80%', value: 0.8 },
        { label: '90%', value: 0.9 },
        { label: '100%', value: 1 }
      ]
    }
  },
  computed: {
    categoryOptions() {
      return [{ id: null, name: '全部支出' }, ...this.categories.filter(item => item.type === 'EXPENSE')]
    }
  },
  onLoad(options = {}) {
    this.currentMonth = options.month || this.formatMonth(new Date())
  },
  onShow() {
    this.loadData()
  },
  methods: {
    formatMoney,
    async loadData() {
      const store = useTransactionStore()
      await Promise.all([
        store.fetchCategories('EXPENSE'),
        store.fetchBudgets(this.currentMonth)
      ])
      this.categories = Array.isArray(store.categories) ? [...store.categories] : []
      this.budgets = Array.isArray(store.budgets) ? [...store.budgets] : []
    },
    selectCategory(event) {
      this.categoryIndex = Number(event.detail.value)
    },
    selectWarning(event) {
      this.warningIndex = Number(event.detail.value)
    },
    async saveBudget() {
      const value = Number(this.amount)
      if (!value || value <= 0) {
        uni.showToast({ title: '请输入预算金额', icon: 'none' })
        return
      }
      const selected = this.categoryOptions[this.categoryIndex] || {}
      const store = useTransactionStore()
      const res = await store.saveBudget({
        budgetMonth: this.currentMonth,
        categoryId: selected.id || null,
        budgetType: 'EXPENSE',
        amount: value,
        warningRate: this.warningOptions[this.warningIndex].value
      })
      if (res && res.code === 200) {
        uni.showToast({ title: '已保存', icon: 'success' })
        this.amount = ''
        this.loadData()
      }
    },
    deleteBudget(item) {
      uni.showModal({
        title: '删除预算',
        content: `确认删除“${item.categoryName}”预算？`,
        success: async (result) => {
          if (!result.confirm) return
          const store = useTransactionStore()
          const res = await store.deleteBudget(item.id, this.currentMonth)
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            this.loadData()
          }
        }
      })
    },
    changeMonth(delta) {
      const [year, month] = this.currentMonth.split('-').map(Number)
      this.currentMonth = this.formatMonth(new Date(year, month - 1 + delta, 1))
      this.loadData()
    },
    formatMonth(date) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    },
    budgetPercent(item) {
      return `${Math.min(100, Math.round(Number(item.usageRate || 0) * 100))}%`
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.month-card,
.form-card,
.budget-list {
  background: #fff;
  border: 1rpx solid #edf1f4;
  border-radius: 20rpx;
  box-shadow: 0 12rpx 30rpx rgba(26, 42, 58, 0.06);
}

.month-card {
  height: 120rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.month-title {
  display: block;
  color: #17202a;
  font-size: 34rpx;
  font-weight: 850;
  text-align: center;
}

.month-sub,
.section-subtitle,
.budget-state,
.remaining {
  color: #7b8798;
  font-size: 24rpx;
}

.month-switch {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: #f2f6f4;
  color: #226f63;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
}

.form-card,
.budget-list {
  padding: 28rpx;
  margin-bottom: 22rpx;
}

.form-row,
.section-header,
.budget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.form-row {
  min-height: 88rpx;
  border-bottom: 1rpx solid #edf1f4;
}

.form-label {
  color: #17202a;
  font-size: 30rpx;
  font-weight: 800;
}

.form-input,
.picker-text {
  color: #334155;
  font-size: 30rpx;
  text-align: right;
}

.save-btn {
  margin-top: 28rpx;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 999rpx;
  background: #17202a;
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
}

.section-header {
  margin-bottom: 10rpx;
}

.section-title {
  color: #17202a;
  font-size: 32rpx;
  font-weight: 850;
}

.budget-item {
  padding: 26rpx 0;
  border-bottom: 1rpx solid #edf1f4;
}

.budget-item:last-child {
  border-bottom: none;
}

.budget-name {
  display: block;
  color: #17202a;
  font-size: 30rpx;
  font-weight: 800;
}

.budget-state.warning {
  color: #d94a62;
}

.budget-money {
  color: #17202a;
  font-size: 27rpx;
  font-weight: 850;
}

.progress-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: #edf2f5;
  overflow: hidden;
  margin: 18rpx 0 10rpx;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: #2EBD85;
}

.progress-fill.warning {
  background: #FF6B6B;
}

.empty-card {
  padding: 54rpx 0;
  color: #7b8798;
  text-align: center;
  font-size: 26rpx;
}
</style>
