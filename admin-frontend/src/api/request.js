import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data.data
  },
  (error) => {
    const status = error.response && error.response.status
    const message = error.response && error.response.data && error.response.data.message
    if (status === 401 || status === 403) {
      ElMessage.error(message || '没有后台访问权限')
    } else {
      ElMessage.error(message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
