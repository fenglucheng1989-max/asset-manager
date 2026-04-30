<template>
  <view class="container">
    <view class="type-tabs">
      <view v-for="item in typeOptions" :key="item.value" :class="['type-tab', { active: form.type === item.value }]" @click="switchType(item.value)">
        {{ item.label }}
      </view>
    </view>

    <view class="form-card">
      <view class="form-row">
        <text class="form-label">名称</text>
        <input class="form-input" v-model="form.name" maxlength="30" placeholder="例如：餐饮" />
      </view>
      <view class="color-row">
        <view
          v-for="color in colors"
          :key="color"
          :class="['color-dot', { active: form.colorHex === color }]"
          :style="{ backgroundColor: color }"
          @click="form.colorHex = color"
        ></view>
      </view>
      <button class="save-btn" @click="saveCategory">{{ editingId ? '保存分类' : '新增分类' }}</button>
    </view>

    <view class="list-card">
      <view class="section-header">
        <text class="section-title">我的分类</text>
        <text class="section-subtitle">系统分类不可编辑</text>
      </view>
      <view class="category-item" v-for="item in filteredCategories" :key="item.id">
        <view class="category-info">
          <view class="category-mark" :style="{ backgroundColor: item.colorHex || '#2EBD85' }"></view>
          <view>
            <text class="category-name">{{ item.name }}</text>
            <text class="category-sub">{{ item.systemDefault ? '系统默认' : '自定义' }}</text>
          </view>
        </view>
        <view class="category-actions" v-if="!item.systemDefault">
          <text @click="editCategory(item)">编辑</text>
          <text class="danger" @click="deleteCategory(item)">删除</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { useTransactionStore } from '../../store/transaction'

export default {
  data() {
    return {
      categories: [],
      editingId: null,
      form: {
        name: '',
        type: 'EXPENSE',
        colorHex: '#FF6B6B',
        sortOrder: 100
      },
      typeOptions: [
        { label: '支出', value: 'EXPENSE' },
        { label: '收入', value: 'INCOME' }
      ],
      colors: ['#FF6B6B', '#2EBD85', '#5B8FF9', '#FFC107', '#00BCD4', '#8B5CF6', '#64748B']
    }
  },
  computed: {
    filteredCategories() {
      return this.categories.filter(item => item.type === this.form.type)
    }
  },
  onShow() {
    this.loadCategories()
  },
  methods: {
    async loadCategories() {
      const store = useTransactionStore()
      await store.fetchCategories()
      this.categories = Array.isArray(store.categories) ? [...store.categories] : []
    },
    switchType(type) {
      this.form.type = type
      this.form.colorHex = type === 'INCOME' ? '#2EBD85' : '#FF6B6B'
      this.resetEditing()
    },
    editCategory(item) {
      this.editingId = item.id
      this.form = {
        name: item.name,
        type: item.type,
        colorHex: item.colorHex || '#2EBD85',
        sortOrder: item.sortOrder || 100
      }
    },
    resetEditing() {
      this.editingId = null
      this.form.name = ''
      this.form.sortOrder = 100
    },
    async saveCategory() {
      if (!this.form.name.trim()) {
        uni.showToast({ title: '请输入分类名称', icon: 'none' })
        return
      }
      const store = useTransactionStore()
      const payload = { ...this.form, name: this.form.name.trim() }
      const res = this.editingId
        ? await store.updateCategory(this.editingId, payload)
        : await store.createCategory(payload)
      if (res && res.code === 200) {
        uni.showToast({ title: '已保存', icon: 'success' })
        this.resetEditing()
        this.loadCategories()
      }
    },
    deleteCategory(item) {
      uni.showModal({
        title: '删除分类',
        content: `确认删除“${item.name}”？已被记录使用的分类不能删除。`,
        success: async (result) => {
          if (!result.confirm) return
          const store = useTransactionStore()
          const res = await store.deleteCategory(item.id)
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            this.loadCategories()
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
}

.type-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6rpx;
  padding: 6rpx;
  border-radius: 16rpx;
  background: var(--app-soft-bg, #eef2f5);
  margin-bottom: 22rpx;
}

.type-tab {
  height: 70rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-muted, #64748b);
  font-size: 28rpx;
  font-weight: 800;
}

.type-tab.active {
  background: var(--app-primary, #e8c56d);
  color: #ffffff;
}

.form-card,
.list-card {
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  border-radius: 20rpx;
  box-shadow: var(--app-shadow, 0 12rpx 30rpx rgba(26, 42, 58, 0.06));
}

.form-card {
  padding: 28rpx;
  margin-bottom: 22rpx;
}

.form-row,
.section-header,
.category-item,
.category-info,
.category-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.form-row {
  height: 86rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.form-label {
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 800;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 30rpx;
}

.color-row {
  display: flex;
  gap: 18rpx;
  padding: 28rpx 0;
}

.color-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 5rpx solid transparent;
}

.color-dot.active {
  border-color: var(--app-text, #17202a);
}

.save-btn {
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #e8c56d);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
}

.list-card {
  padding: 30rpx;
}

.section-header {
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.section-title {
  color: var(--app-text, #17202a);
  font-size: 32rpx;
  font-weight: 850;
}

.section-subtitle,
.category-sub {
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
}

.category-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.category-item:last-child {
  border-bottom: none;
}

.category-mark {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.category-name {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 30rpx;
  font-weight: 800;
}

.category-actions {
  color: var(--app-primary-dark, #226f63);
  font-size: 26rpx;
  font-weight: 800;
}

.category-actions .danger {
  color: var(--app-danger, #d94a62);
}
</style>
