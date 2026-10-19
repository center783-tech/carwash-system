<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>會員註冊</h2>
      <el-form :model="form" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="帳號">
          <el-input v-model="form.username" placeholder="4~50 字元" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="form.password" type="password" placeholder="至少 8 字元" show-password />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="example@mail.com" />
        </el-form-item>
        <el-form-item label="手機">
          <el-input v-model="form.phone" placeholder="0912345678" />
        </el-form-item>
        <el-form-item label="姓名（選填）">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
          註冊
        </el-button>
      </el-form>
      <div class="auth-links">
        <router-link to="/login">已經有帳號？前往登入</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/member'

const router = useRouter()
const form = reactive({ username: '', password: '', email: '', phone: '', realName: '' })
const loading = ref(false)

async function handleRegister() {
  loading.value = true
  try {
    await register(form)
    ElMessage.success('註冊成功，請登入')
    router.push('/login')
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
  min-height: 100vh;
  background: var(--el-fill-color-light, #f5f7fa);
}
.auth-card {
  width: 400px;
}
.auth-links {
  margin-top: 12px;
  text-align: center;
}
</style>
