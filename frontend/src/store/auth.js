import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || '' // MEMBER / ADMIN / STAFF
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === 'ADMIN' || state.role === 'STAFF'
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
