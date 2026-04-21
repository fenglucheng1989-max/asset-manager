import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import AdminLayout from '../layout/AdminLayout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Users from '../views/Users.vue'
import Accounts from '../views/Accounts.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: AdminLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: Dashboard },
        { path: 'users', component: Users },
        { path: 'accounts', component: Accounts }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.path !== '/login' && !authStore.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && authStore.isLoggedIn) {
    return '/dashboard'
  }
  return true
})

export default router
