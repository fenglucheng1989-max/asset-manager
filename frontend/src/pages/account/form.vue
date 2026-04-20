<template>
  <view class="container">
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">账户名称</text>
        <input class="form-input" v-model="form.name" placeholder="请输入账户名称" />
      </view>

      <view class="form-item">
        <text class="form-label">账户类型</text>
        <picker :value="typeIndex" :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="form-picker">
            <text>{{ typeOptions[typeIndex].label }}</text>
            <text class="picker-arrow">></text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">当前余额</text>
        <input class="form-input" v-model="form.currentBalance" type="digit" placeholder="请输入余额" />
      </view>

      <view class="form-item">
        <text class="form-label">是否为负债</text>
        <switch :checked="form.isLiability" @change="form.isLiability = $event.detail.value" color="#2EBD85" />
      </view>

      <view class="form-item">
        <text class="form-label">计入净资产</text>
        <switch :checked="form.includeInTotal" @change="form.includeInTotal = $event.detail.value" color="#2EBD85" />
      </view>

      <view class="form-item">
        <text class="form-label">颜色标识</text>
        <view class="color-picker">
          <view
            v-for="color in colorOptions"
            :key="color"
            class="color-option"
            :class="{ active: form.colorHex === color }"
            :style="{ backgroundColor: color }"
            @click="form.colorHex = color"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <input class="form-input" v-model="form.remark" placeholder="可选备注" />
      </view>
    </view>

    <view class="btn-group">
      <button class="btn-save" @click="handleSave">保存</button>
      <button class="btn-delete" v-if="isEdit" @click="handleDelete">删除账户</button>
    </view>
  </view>
</template>

<script>
import { useAssetStore } from '../../store/asset'

export default {
  data() {
    return {
      isEdit: false,
      accountId: null,
      form: {
        name: '',
        accountType: 'BANK',
        currentBalance: '',
        isLiability: false,
        includeInTotal: true,
        icon: '',
        colorHex: '#2EBD85',
        remark: ''
      },
      typeIndex: 0,
      typeOptions: [
        { label: '银行卡', value: 'BANK' },
        { label: '现金', value: 'CASH' },
        { label: '信用卡', value: 'CREDIT_CARD' },
        { label: '电子钱包', value: 'E_WALLET' },
        { label: '投资理财', value: 'INVESTMENT' },
        { label: '贷款', value: 'LOAN' },
        { label: '房产', value: 'REAL_ESTATE' },
        { label: '其他资产', value: 'OTHER_ASSET' },
        { label: '其他负债', value: 'OTHER_LIABILITY' }
      ],
      colorOptions: ['#2EBD85', '#5B8FF9', '#FF6B6B', '#FFC107', '#00BCD4', '#607D8B']
    }
  },
  onLoad(options) {
    if (options.id) {
      this.isEdit = true
      this.accountId = Number(options.id)
      this.loadAccount()
    }
  },
  methods: {
    loadAccount() {
      const assetStore = useAssetStore()
      const account = assetStore.accounts.find(item => item.id === this.accountId)
      if (!account) return

      this.form = {
        name: account.name,
        accountType: account.accountType,
        currentBalance: String(account.currentBalance),
        isLiability: account.isLiability,
        includeInTotal: account.includeInTotal,
        icon: account.icon || '',
        colorHex: account.colorHex || '#2EBD85',
        remark: account.remark || ''
      }
      this.typeIndex = this.typeOptions.findIndex(item => item.value === account.accountType)
      if (this.typeIndex < 0) this.typeIndex = 0
    },
    onTypeChange(e) {
      this.typeIndex = Number(e.detail.value)
      this.form.accountType = this.typeOptions[this.typeIndex].value
    },
    async handleSave() {
      if (!this.form.name.trim()) {
        uni.showToast({ title: '请输入账户名称', icon: 'none' })
        return
      }

      if (this.form.currentBalance === '' || Number.isNaN(Number(this.form.currentBalance))) {
        uni.showToast({ title: '请输入有效余额', icon: 'none' })
        return
      }

      const assetStore = useAssetStore()
      const data = {
        ...this.form,
        currentBalance: Number(this.form.currentBalance)
      }

      const res = this.isEdit
        ? await assetStore.updateAccount(this.accountId, data)
        : await assetStore.createAccount(data)

      if (res && res.code === 200) {
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      }
    },
    async handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '删除后不可恢复，确认要删除该账户吗？',
        success: async (result) => {
          if (!result.confirm) return

          const assetStore = useAssetStore()
          const res = await assetStore.deleteAccount(this.accountId)
          if (res && res.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 1000)
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20rpx;
  min-height: 100vh;
}

.form-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.form-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  font-size: 30rpx;
  color: #333333;
  min-width: 180rpx;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 30rpx;
  color: #666666;
}

.form-picker {
  display: flex;
  align-items: center;
  font-size: 30rpx;
  color: #666666;
}

.picker-arrow {
  margin-left: 10rpx;
  color: #cccccc;
}

.color-picker {
  display: flex;
  gap: 16rpx;
}

.color-option {
  width: 48rpx;
  height: 48rpx;
  border-radius: 24rpx;
  border: 4rpx solid transparent;
}

.color-option.active {
  border-color: #333333;
}

.btn-group {
  padding: 0 20rpx;
}

.btn-save {
  background: #2EBD85;
  color: #ffffff;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  margin-bottom: 20rpx;
}

.btn-delete {
  background: #ffffff;
  color: #FF6B6B;
  border: 2rpx solid #FF6B6B;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
}
</style>
