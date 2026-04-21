<template>
  <div>
    <div class="toolbar">
      <h1 class="page-title">账户管理</h1>
      <div class="toolbar-right">
        <el-input v-model="filters.keyword" placeholder="搜索账户名称" clearable style="width: 200px" @keyup.enter="loadAccounts" />
        <el-input v-model="filters.userId" placeholder="用户ID" clearable style="width: 120px" @keyup.enter="loadAccounts" />
        <el-select v-model="filters.accountType" placeholder="账户类型" clearable style="width: 150px">
          <el-option v-for="item in accountTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="loadAccounts">查询</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="accounts" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户" width="130" />
        <el-table-column prop="name" label="账户名称" min-width="150" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ accountTypeName(row.accountType) }}</template>
        </el-table-column>
        <el-table-column label="余额" min-width="140">
          <template #default="{ row }">
            <span :class="{ 'money-negative': row.isLiability }">
              {{ row.isLiability ? '-' : '' }}{{ formatMoney(row.currentBalance) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="计入净资产" width="120">
          <template #default="{ row }">
            <el-tag :type="row.includeInTotal ? 'success' : 'info'">{{ row.includeInTotal ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ row.updatedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeAccount(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑账户" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账户名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="账户类型">
          <el-select v-model="form.accountType" style="width: 100%">
            <el-option v-for="item in accountTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input-number v-model="form.currentBalance" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否负债">
          <el-switch v-model="form.isLiability" />
        </el-form-item>
        <el-form-item label="计入净资产">
          <el-switch v-model="form.includeInTotal" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-input v-model="form.colorHex" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAccount, getAccounts, updateAccount } from '../api/admin'
import { accountTypeName, accountTypeOptions, formatMoney } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const accounts = ref([])
const dialogVisible = ref(false)
const currentId = ref(null)
const filters = reactive({
  keyword: '',
  userId: '',
  accountType: ''
})
const form = reactive({
  name: '',
  accountType: 'BANK',
  currency: 'CNY',
  currentBalance: 0,
  isLiability: false,
  includeInTotal: true,
  icon: '',
  colorHex: '#2EBD85',
  sortOrder: 0,
  remark: ''
})

async function loadAccounts() {
  loading.value = true
  try {
    accounts.value = await getAccounts({
      keyword: filters.keyword,
      userId: filters.userId || undefined,
      accountType: filters.accountType || undefined
    })
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  currentId.value = row.id
  Object.assign(form, {
    name: row.name,
    accountType: row.accountType,
    currency: row.currency || 'CNY',
    currentBalance: Number(row.currentBalance || 0),
    isLiability: !!row.isLiability,
    includeInTotal: row.includeInTotal !== false,
    icon: '',
    colorHex: row.colorHex || '#2EBD85',
    sortOrder: row.sortOrder || 0,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function saveAccount() {
  saving.value = true
  try {
    await updateAccount(currentId.value, { ...form })
    ElMessage.success('账户已更新')
    dialogVisible.value = false
    loadAccounts()
  } finally {
    saving.value = false
  }
}

async function removeAccount(row) {
  await ElMessageBox.confirm(`确认删除账户「${row.name}」吗？`, '删除账户', { type: 'warning' })
  await deleteAccount(row.id)
  ElMessage.success('账户已删除')
  loadAccounts()
}

onMounted(loadAccounts)
</script>
