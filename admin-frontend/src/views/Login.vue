<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <h1>资产管家后台</h1>
      <p>使用管理员账号登录</p>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" type="password" show-password size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="login-hint">本地预览管理员：admin / admin123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123456'
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f4f6f8 0%, #dfe7ef 100%);
}

.login-card {
  width: 380px;
  border-radius: 8px;
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

p {
  margin: 0 0 28px;
  color: #64748b;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin-top: 18px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}
</style>
