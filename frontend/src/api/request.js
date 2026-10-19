import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 每個請求自動帶上 Authorization header
request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

// 統一處理後端的 ApiResponse { code, message, data } 格式，以及 401/403
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '發生錯誤')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message

    if (status === 401) {
      ElMessage.error('請先登入或登入已過期')
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error('權限不足，無法存取此功能')
    } else {
      ElMessage.error(message || '連線發生錯誤')
    }
    return Promise.reject(error)
  }
)

export default request
