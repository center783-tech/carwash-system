import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || '' // MEMBER / ADMIN / STAFF / EMPLOYEE
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    // 後台完整功能：ADMIN / STAFF
    isAdmin: (state) => state.role === 'ADMIN' || state.role === 'STAFF',
    // 能進後台（含只能看班表的 EMPLOYEE）
    isBackoffice: (state) => ['ADMIN', 'STAFF', 'EMPLOYEE'].includes(state.role)
  },

  actions: {
    // 對應後端 LoginResponse { token, userId, username, role }
    setAuth({ token, userId, username, role }) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = role

      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('username', username)
      localStorage.setItem('role', role)
    },

    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.role = ''
      localStorage.clear()
    }
  }
})
