<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <router-link to="/" class="auth-brand">洗享快</router-link>
      <h2>{{ isPassword ? '忘記密碼' : '忘記帳號' }}</h2>

      <template v-if="!sent">
        <p class="hint">
          請輸入註冊時使用的 Email，我們會寄一封信給您，
          {{ isPassword ? '點開信中的連結即可設定新密碼。' : '點開信中的連結即可查看您的帳號。' }}
        </p>
        <el-form :model="form" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="Email">
            <el-input v-model="form.email" type="email" placeholder="請輸入註冊的 Email" />
          </el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" round style="width: 100%">
            寄送連結
          </el-button>
        </el-form>
      </template>

      <el-result v-else icon="success" title="信件已寄出">
        <template #sub-title>
          若此 Email 已註冊，您會收到一封信，連結 15 分鐘內有效。<br />
          沒收到請檢查垃圾郵件匣，或稍候再重新申請。
        </template>
      </el-result>

      <div class="auth-links">
        <router-link to="/login">返回登入</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forgotPassword, forgotUsername } from '@/api/member'

const route = useRoute()
const isPassword = computed(() => route.name === 'ForgotPassword')

const form = reactive({ email: '' })
const loading = ref(false)
const sent = ref(false)

async function handleSubmit() {
  if (!form.email.trim()) {
    ElMessage.warning('請輸入 Email')
    return
  }
  loading.value = true
  try {
    await (isPassword.value ? forgotPassword : forgotUsername)(form.email.trim())
    sent.value = true
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
.hint {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}
.auth-links {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
}
</style>
