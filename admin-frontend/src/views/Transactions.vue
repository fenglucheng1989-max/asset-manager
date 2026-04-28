<template>
  <div>
    <div class="toolbar">
      <div>
        <h1 class="page-title">流水管理</h1>
        <div class="page-subtitle">观察全平台记账活跃度、收支规模和异常流水</div>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div class="transaction-hero">
      <div class="hero-copy">
        <div class="hero-label">{{ filters.month }} 平台收支</div>
        <div class="hero-value" :class="{ negative: Number(report.net || 0) < 0 }">{{ formatSigned(report.net) }}</div>
      </div>
      <div class="hero-metrics">
        <div>
          <span>收入</span>
          <strong class="money-positive">{{ formatMoney(report.income) }}</strong>
        </div>
        <div>
          <span>支出</span>
          <strong class="money-negative">{{ formatMoney(report.expense) }}</strong>
        </div>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-date-picker v-model="filters.month" type="month" value-format="YYYY-MM" placeholder="月份" />
          <el-select v-model="filters.type" clearable placeholder="流水类型" style="width: 140px">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
            <el-option label="转账" value="TRANSFER" />
          </el-select>
          <el-input v-model="filters.userId" clearable placeholder="用户 ID" style="width: 140px" />
        </div>
        <div class="toolbar-right">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadData">查询</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType(row.transactionType)">{{ typeName(row.transactionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column label="账户" min-width="180">
          <template #default="{ row }">
            <span v-if="row.transactionType === 'TRANSFER'">{{ row.accountName }} → {{ row.targetAccountName }}</span>
            <span v-else>{{ row.accountName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="150" align="right">
          <template #default="{ row }">
            <span :class="amountClass(row.transactionType)">{{ signedAmount(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tag" label="标签" width="120" />
        <el-table-column label="发生时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getTransactionReport, getTransactions } from '../api/admin'
import { formatMoney } from '../utils/format'

const loading = ref(false)
const records = ref([])
const report = ref({ income: 0, expense: 0, net: 0 })
const filters = ref({
  month: currentMonth(),
  type: '',
  userId: ''
})

function currentMonth() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function monthRange(month) {
  const [year, value] = month.split('-').map(Number)
  const start = new Date(year, value - 1, 1)
  const end = new Date(year, value, 0)
  const format = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return [format(start), format(end)]
}

async function loadData() {
  loading.value = true
  try {
    const [startDate, endDate] = monthRange(filters.value.month)
    const params = {
      startDate,
      endDate,
      type: filters.value.type || undefined,
      userId: filters.value.userId || undefined,
      limit: 300
    }
    const [recordData, reportData] = await Promise.all([
      getTransactions(params),
      getTransactionReport({ month: filters.value.month })
    ])
    records.value = Array.isArray(recordData) ? recordData : []
    report.value = reportData || report.value
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = { month: currentMonth(), type: '', userId: '' }
  loadData()
}

function typeName(type) {
  return { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账' }[type] || type
}

function tagType(type) {
  return { INCOME: 'success', EXPENSE: 'danger', TRANSFER: 'info' }[type] || ''
}

function amountClass(type) {
  if (type === 'INCOME') return 'money-positive strong'
  if (type === 'EXPENSE') return 'money-negative strong'
  return 'strong'
}

function signedAmount(row) {
  const amount = formatMoney(row.baseAmount || row.amount)
  if (row.transactionType === 'INCOME') return `+${amount}`
  if (row.transactionType === 'EXPENSE') return `-${amount}`
  return amount
}

function formatSigned(value) {
  const amount = Number(value || 0)
  if (amount === 0) return formatMoney(0)
  return `${amount > 0 ? '+' : '-'}${formatMoney(Math.abs(amount))}`
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

onMounted(loadData)
</script>

<style scoped>
.page-subtitle {
  color: #64748b;
  font-size: 14px;
}

.transaction-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 28px;
  border-radius: 14px;
  background: linear-gradient(135deg, #111827 0%, #14534a 100%);
  color: #fff;
}

.hero-label {
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
}

.hero-value {
  margin-top: 8px;
  color: #ffd166;
  font-size: 36px;
  font-weight: 800;
}

.hero-value.negative {
  color: #ff8fa3;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 16px;
}

.hero-metrics > div {
  min-width: 150px;
  padding: 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-metrics span,
.hero-metrics strong {
  display: block;
}

.hero-metrics span {
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 8px;
}

.filter-card {
  margin-bottom: 18px;
}

.strong {
  font-weight: 800;
}
</style>
