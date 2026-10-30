<template>
  <div class="auth-page">
    <el-card class="auth-card" v-loading="loading">
      <router-link to="/" class="auth-brand">洗享快</router-link>
      <h2>您的帳號</h2>

      <el-result v-if="username" icon="success" title="找到您的帳號">
        <template #sub-title>
          <div class="username">{{ username }}</div>
          <div class="note">此頁面重新整理後即失效，請記下您的帳號。</div>
        </template>
      </el-result>
      <el-result v-else-if="!loading" icon="error" title="連結無效" sub-title="連結已失效或已使用過，請重新申請。">
        <template #extra>
          <router-link to="/forgot-username">重新申請</router-link>
        </template>
      </el-result>

      <div class="auth-links">
        <router-link to="/login">前往登入</router-link>
        <span class="sep">|</span>
        <router-link to="/forgot-password">忘記密碼？</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { recoverUsername } from '@/api/member'

const route = useRoute()
const username = ref('')
const loading = ref(true)

onMounted(async () => {
  const token = route.query.token
  if (!token) {
    loading.value = false
    return
  }
  try {
    username.value = await recoverUsername(token)
  } catch {
    // 錯誤訊息已由 request 攔截器顯示
  } finally {
    loading.value = false
  }
})
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
.username {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  word-break: break-all;
}
.note {
  margin-top: 8px;
  font-size: 13px;
  color: #888;
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
</style>
