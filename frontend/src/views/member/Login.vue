<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>會員登入</h2>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="帳號">
          <el-input v-model="form.username" placeholder="請輸入帳號" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="form.password" type="password" placeholder="請輸入密碼" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
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
  height: 100vh;
  background: var(--el-fill-color-light, #f5f7fa);
}
.auth-card {
  width: 360px;
}
.auth-links {
  margin-top: 12px;
  text-align: center;
}
</style>
