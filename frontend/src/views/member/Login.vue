<template>
  <div class="auth-page">
    <div class="auth-art" aria-hidden="true">
      <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
        <circle cx="100" cy="100" r="92" fill="none" stroke="#2a4350" stroke-width="2" />
        <g fill="#3fb3c2">
          <ellipse cx="70" cy="60" rx="5" ry="9" />
          <ellipse cx="130" cy="50" rx="4" ry="7" />
          <ellipse cx="150" cy="90" rx="5" ry="9" />
        </g>
        <g fill="#e8a33d" opacity="0.85">
          <circle cx="55" cy="120" r="6" />
          <circle cx="140" cy="140" r="4" />
        </g>
      </svg>
    </div>

    <el-card class="auth-card">
      <router-link to="/" class="auth-brand">洗享快</router-link>
      <h2>會員登入</h2>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="帳號">
          <el-input v-model="form.username" placeholder="請輸入帳號" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="form.password" type="password" placeholder="請輸入密碼" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" round style="width: 100%">
          登入
        </el-button>
      </el-form>
      <div class="auth-links">
        <router-link to="/register">還沒有帳號？立即註冊</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/member'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    const result = await login(form)
    authStore.setAuth(result)
    ElMessage.success('登入成功')
    router.push(route.query.redirect || '/services')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 48px;
  height: 100vh;
  background: var(--ink);
  padding: 0 24px;
}
.auth-art {
  width: 180px;
  flex-shrink: 0;
}
.auth-art svg {
  width: 100%;
}
.auth-card {
  width: 360px;
}
.auth-brand {
  display: block;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: var(--rinse-deep);
  text-decoration: none;
  margin-bottom: 4px;
}
.auth-links {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
}
@media (max-width: 640px) {
  .auth-art { display: none; }
}
</style>
