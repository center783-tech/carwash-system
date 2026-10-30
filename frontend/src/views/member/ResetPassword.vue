<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <router-link to="/" class="auth-brand">洗享快</router-link>
      <h2>重設密碼</h2>

      <el-result v-if="!token" icon="error" title="連結無效" sub-title="請由信件中的連結進入此頁。">
        <template #extra>
          <router-link to="/forgot-password">重新申請</router-link>
        </template>
      </el-result>

      <el-form v-else :model="form" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="新密碼">
          <el-input v-model="form.newPassword" type="password" show-password
                    placeholder="至少 8 碼，需含英文字母與數字" />
        </el-form-item>
        <el-form-item label="確認新密碼">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再輸入一次新密碼" />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" round style="width: 100%">
          重設密碼
        </el-button>
      </el-form>

      <div class="auth-links">
        <router-link to="/login">返回登入</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword } from '@/api/member'

const route = useRoute()
const router = useRouter()
const token = route.query.token

const form = reactive({ newPassword: '', confirm: '' })
const loading = ref(false)

async function handleSubmit() {
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/.test(form.newPassword)) {
    ElMessage.warning('密碼至少 8 碼，需同時包含英文字母與數字')
    return
  }
  if (form.newPassword !== form.confirm) {
    ElMessage.warning('兩次輸入的密碼不一致')
    return
  }
  loading.value = true
  try {
    await resetPassword(token, form.newPassword)
    ElMessage.success('密碼已重設，請使用新密碼登入')
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
  background: var(--ink);
  padding: 32px 24px;
}
.auth-card {
  width: 400px;
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
</style>
