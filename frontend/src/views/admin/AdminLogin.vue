<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>後台管理登入</h2>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="帳號">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
          登入
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/admin'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    const result = await adminLogin(form)
    authStore.setAuth(result)
    ElMessage.success('登入成功')
    router.push('/admin/dashboard')
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
  background: #1f2937;
}
.auth-card {
  width: 360px;
}
</style>
