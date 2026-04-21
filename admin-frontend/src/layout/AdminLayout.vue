<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">资</div>
        <div>
          <div class="brand-name">资产管家</div>
          <div class="brand-subtitle">后台管理</div>
        </div>
      </div>
      <el-menu router :default-active="$route.path" background-color="#111827" text-color="#cbd5e1" active-text-color="#34d399">
        <el-menu-item index="/dashboard">
          <span>数据总览</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/accounts">
          <span>账户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">资产管理后台</div>
        <div class="topbar-actions">
          <span class="username">{{ authStore.username }}</span>
          <el-button @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.sidebar {
  background: #111827;
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 20px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: #10b981;
  font-size: 20px;
  font-weight: 700;
}

.brand-name {
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.topbar-title {
  font-weight: 700;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #64748b;
}

.content {
  padding: 24px;
}
</style>
