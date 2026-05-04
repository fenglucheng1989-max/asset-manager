<template>
  <view class="container" :style="themeVars">
    <view class="form-card">
      <!-- Period Type -->
      <view class="form-row">
        <text class="form-label">周期类型</text>
        <view class="type-options">
          <view
            v-for="opt in periodTypeOptions"
            :key="opt.value"
            :class="['type-opt', { active: form.periodType === opt.value, disabled: !!editingId }]"
            @click="!editingId && switchPeriodType(opt.value)"
          >
            {{ opt.label }}
          </view>
        </view>
      </view>

      <!-- Period Key -->
      <view class="form-row">
        <text class="form-label">周期</text>
        <input
          class="form-input"
          v-model="form.periodKey"
          :placeholder="periodKeyPlaceholder"
          :disabled="!!editingId"
        />
      </view>

      <!-- Category -->
      <view class="form-row">
        <text class="form-label">分类</text>
        <picker :range="categoryOptions" range-key="name" :value="categoryIndex" @change="selectCategory">
          <view class="picker-text">{{ categoryOptions[categoryIndex] ? categoryOptions[categoryIndex].name : '全部支出' }}</view>
        </picker>
      </view>

      <!-- Amount -->
      <view class="form-row">
        <text class="form-label">金额</text>
        <input class="form-input" v-model="form.amount" type="digit" placeholder="0.00" />
      </view>

      <!-- Warning Rate -->
      <view class="form-row">
        <text class="form-label">预警线</text>
        <view class="warning-options">
          <view
            v-for="opt in warningOptions"
            :key="opt.value"
            :class="['warning-opt', { active: form.warningRate === opt.value }]"
            @click="form.warningRate = opt.value"
          >
            {{ opt.label }}
          </view>
        </view>
      </view>

      <!-- Remark -->
      <view class="form-row">
        <text class="form-label">备注</text>
        <input class="form-input" v-model="form.remark" placeholder="可选" maxlength="200" />
      </view>

      <view class="form-actions">
        <button class="save-btn" @click="handleSave" :disabled="saving">
          {{ saving ? '保存中...' : (editingId ? '保存修改' : '新建预算') }}
        </button>
        <button v-if="editingId" class="delete-btn" @click="handleDelete">
          删除预算
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { useTransactionStore } from '../../store/transaction'
import { formatMoney } from '../../utils/money'
import { getThemeVars } from '../../utils/theme'

export default {
  data() {
    return {
      editingId: null,
      saving: false,
      categories: [],
      categoryIndex: 0,
      periodTypeOptions: [
        { value: 'MONTHLY', label: '月度' },
        { value: 'YEARLY', label: '年度' }
      ],
      warningOptions: [
        { label: '70%', value: 0.7 },
        { label: '80%', value: 0.8 },
        { label: '90%', value: 0.9 },
        { label: '100%', value: 1.0 }
      ],
      form: {
        periodType: 'MONTHLY',
        periodKey: '',
        amount: '',
        warningRate: 0.8,
        remark: ''
      },
      selectedCategoryId: null,
      themeVars: getThemeVars()
    }
  },
  computed: {
    categoryOptions() {
      return [{ id: null, name: '全部支出' }, ...this.categories.filter(item => item.type === 'EXPENSE')]
    },
    periodKeyPlaceholder() {
      if (this.form.periodType === 'MONTHLY') return 'yyyy-MM 如 2026-04'
      if (this.form.periodType === 'YEARLY') return 'yyyy 如 2026'
      return 'yyyyWww 如 2026W18'
    }
  },
  onLoad(options = {}) {
    if (options.id) {
      this.editingId = Number(options.id)
    }
    if (options.periodType) {
      this.form.periodType = options.periodType
    }
    if (options.periodKey) {
      this.form.periodKey = options.periodKey
    }
    if (!options.periodKey && !options.id) {
      const now = new Date()
      this.form.periodKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.loadCategories()
    if (this.editingId) {
      this.loadBudget()
    }
  },
  methods: {
    formatMoney,
    async loadCategories() {
      const store = useTransactionStore()
      await store.fetchCategories('EXPENSE')
      this.categories = Array.isArray(store.categories) ? [...store.categories] : []
    },
    async loadBudget() {
      const store = useTransactionStore()
      await store.fetchBudgets({})
      const budgets = Array.isArray(store.budgets) ? store.budgets : []
      const item = budgets.find(b => b.id === this.editingId)
      if (item) {
        this.form.periodType = item.periodType || 'MONTHLY'
        this.form.periodKey = item.budgetMonth || ''
        this.form.amount = String(item.amount || '')
        this.form.warningRate = Number(item.warningRate || 0.8)
        this.form.remark = item.remark || ''
        if (item.categoryId) {
          const idx = this.categoryOptions.findIndex(c => c.id === item.categoryId)
          if (idx >= 0) this.categoryIndex = idx
        } else {
          this.categoryIndex = 0
        }
      }
    },
    switchPeriodType(newType) {
      const oldType = this.form.periodType
      if (oldType === newType) return
      this.form.periodType = newType
      const oldKey = this.form.periodKey.trim()

      if (oldKey) {
        const year = this.extractYear(oldKey, oldType)
        if (newType === 'MONTHLY') {
          const now = new Date()
          this.form.periodKey = year === String(now.getFullYear())
            ? `${year}-${String(now.getMonth() + 1).padStart(2, '0')}`
            : `${year}-01`
        } else if (newType === 'YEARLY') {
          this.form.periodKey = year
        } else if (newType === 'WEEKLY') {
          this.form.periodKey = year === String(new Date().getFullYear())
            ? this.getCurrentWeekKey()
            : `${year}W01`
        }
      } else {
        const now = new Date()
        if (newType === 'MONTHLY') {
          this.form.periodKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
        } else if (newType === 'YEARLY') {
          this.form.periodKey = String(now.getFullYear())
        } else if (newType === 'WEEKLY') {
          this.form.periodKey = this.getCurrentWeekKey()
        }
      }
    },
    extractYear(key, type) {
      if (type === 'MONTHLY') return key.substring(0, 4)
      if (type === 'WEEKLY') return key.substring(0, 4)
      return key // YEARLY
    },
    getCurrentWeekKey() {
      const now = new Date()
      const d = new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()))
      const dayNum = d.getUTCDay() || 7
      d.setUTCDate(d.getUTCDate() + 4 - dayNum)
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
      const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7)
      return `${d.getUTCFullYear()}W${String(weekNo).padStart(2, '0')}`
    },
    selectCategory(event) {
      this.categoryIndex = Number(event.detail.value)
    },
    async handleSave() {
      if (this.saving) return
      const amount = Number(this.form.amount)
      if (!amount || amount <= 0) {
        uni.showToast({ title: '请输入预算金额', icon: 'none' })
        return
      }
      const periodKey = this.form.periodKey.trim()
      if (!periodKey) {
        uni.showToast({ title: '请输入预算周期', icon: 'none' })
        return
      }
      if (this.form.periodType === 'YEARLY' && !/^\d{4}$/.test(periodKey)) {
        uni.showToast({ title: '年度预算期间格式应为 yyyy，如 2026', icon: 'none' })
        return
      }
      if (this.form.periodType === 'MONTHLY' && !/^\d{4}-\d{2}$/.test(periodKey)) {
        uni.showToast({ title: '月度预算期间格式应为 yyyy-MM，如 2026-04', icon: 'none' })
        return
      }
      if (this.form.periodType === 'WEEKLY' && !/^\d{4}W\d{2}$/.test(periodKey)) {
        uni.showToast({ title: '周预算期间格式应为 yyyyWww，如 2026W18', icon: 'none' })
        return
      }
      this.saving = true
      try {
        const store = useTransactionStore()
        const selected = this.categoryOptions[this.categoryIndex] || {}
        const res = await store.saveBudget({
          budgetMonth: periodKey,
          periodType: this.form.periodType,
          categoryId: selected.id || null,
          budgetType: 'EXPENSE',
          amount: amount,
          warningRate: this.form.warningRate,
          remark: this.form.remark.trim() || undefined
        })
        if (res && res.code === 200) {
          uni.showToast({ title: this.editingId ? '已更新' : '已创建', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        }
      } catch (error) {
        // Error toast already shown by request interceptor
      } finally {
        this.saving = false
      }
    },
    handleDelete() {
      uni.showModal({
        title: '删除预算',
        content: '确认删除此预算？',
        success: async (result) => {
          if (!result.confirm) return
          const store = useTransactionStore()
          const res = await store.deleteBudget(this.editingId, {})
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
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
  padding: 24rpx;
  box-sizing: border-box;
  background: var(--app-page-bg, #f8f9fb);
}

.form-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
  padding: 28rpx;
}

.form-row {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.form-label {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.form-input {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  text-align: right;
  flex: 1;
}

.picker-text {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  text-align: right;
}

/* Period Type Options */
.type-options {
  display: flex;
  gap: 8rpx;
}

.type-opt {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 650;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-muted, #7b8798);
  transition: all 0.2s;
}

.type-opt.active {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
}

.type-opt.disabled {
  opacity: 0.6;
  pointer-events: none;
}

/* Warning Options */
.warning-options {
  display: flex;
  gap: 8rpx;
}

.warning-opt {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 650;
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-muted, #7b8798);
  transition: all 0.2s;
}

.warning-opt.active {
  background: var(--app-primary, #d3a414);
  color: #ffffff;
}

/* Actions */
.form-actions {
  margin-top: 40rpx;
}

.save-btn {
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-primary, #d3a414);
  font-size: 30rpx;
  font-weight: 700;
  border: 2rpx solid var(--app-primary, #d3a414);
  margin-bottom: 20rpx;
}

.save-btn[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
  border-color: var(--app-border, #e5e7eb);
}

.delete-btn {
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  background: var(--app-card-bg, #ffffff);
  color: var(--app-danger, #d94a62);
  font-size: 30rpx;
  border: 2rpx solid var(--app-danger, #d94a62);
}

.delete-btn[disabled] {
  background: var(--app-soft-bg, #e5e7eb);
  color: var(--app-faint, #9ca3af);
  border-color: var(--app-border, #e5e7eb);
}
</style>
