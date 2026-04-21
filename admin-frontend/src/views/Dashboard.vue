<template>
  <div>
    <div class="toolbar">
      <h1 class="page-title">数据总览</h1>
      <el-button type="primary" :loading="loading" @click="loadDashboard">刷新</el-button>
    </div>

    <div class="stat-grid">
      <el-card class="stat-card">
        <div class="stat-label">用户数</div>
        <div class="stat-value">{{ dashboard.userCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">账户数</div>
        <div class="stat-value">{{ dashboard.accountCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">平台总资产</div>
        <div class="stat-value money-positive">{{ formatMoney(dashboard.totalAsset) }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">平台总负债</div>
        <div class="stat-value money-negative">{{ formatMoney(dashboard.totalLiability) }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">平台净资产</div>
        <div class="stat-value">{{ formatMoney(dashboard.netWorth) }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">资产账户</div>
        <div class="stat-value">{{ dashboard.assetAccountCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">负债账户</div>
        <div class="stat-value">{{ dashboard.liabilityAccountCount || 0 }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDashboard } from '../api/admin'
import { formatMoney } from '../utils/format'

const loading = ref(false)
const dashboard = ref({})

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>
