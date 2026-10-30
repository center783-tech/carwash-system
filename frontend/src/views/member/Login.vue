<template>
  <div class="auth-page">
    <div class="auth-art">
      <CarWashScene />
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
        <router-link to="/forgot-username">忘記帳號？</router-link>
        <span class="sep">|</span>
        <router-link to="/forgot-password">忘記密碼？</router-link>
      </div>
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
import CarWashScene from '@/components/CarWashScene.vue'
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
  width: 420px;
  flex-shrink: 0;
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
.sep {
  margin: 0 8px;
  color: #ccc;
}
@media (max-width: 900px) {
  .auth-art { display: none; }
}
</style>
