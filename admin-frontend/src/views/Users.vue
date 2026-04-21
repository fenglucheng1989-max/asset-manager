<template>
  <div>
    <div class="toolbar">
      <h1 class="page-title">用户管理</h1>
      <div class="toolbar-right">
        <el-input v-model="keyword" placeholder="搜索用户名或邮箱" clearable style="width: 240px" @keyup.enter="loadUsers" />
        <el-button type="primary" :loading="loading" @click="loadUsers">查询</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" @change="changeRole(row)">
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="accountCount" label="账户数" width="100" />
        <el-table-column label="总资产" min-width="140">
          <template #default="{ row }">{{ formatMoney(row.totalAsset) }}</template>
        </el-table-column>
        <el-table-column label="总负债" min-width="140">
          <template #default="{ row }">{{ formatMoney(row.totalLiability) }}</template>
        </el-table-column>
        <el-table-column label="净资产" min-width="140">
          <template #default="{ row }">{{ formatMoney(row.netWorth) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateUserRole } from '../api/admin'
import { formatMoney } from '../utils/format'

const loading = ref(false)
const keyword = ref('')
const users = ref([])

async function loadUsers() {
  loading.value = true
  try {
    users.value = await getUsers({ keyword: keyword.value })
  } finally {
    loading.value = false
  }
}

async function changeRole(row) {
  await updateUserRole(row.id, row.role)
  ElMessage.success('角色已更新')
}

onMounted(loadUsers)
</script>
