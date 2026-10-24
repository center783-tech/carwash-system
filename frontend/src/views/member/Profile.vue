<template>
  <div class="page-container">
    <h2>個人資料</h2>

    <el-card style="margin-bottom: 16px">
      <el-form :model="profileForm" label-width="80px" @submit.prevent="handleUpdateProfile">
        <el-form-item label="帳號">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="profileForm.email" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profileForm.realName" />
        </el-form-item>
        <el-form-item label="手機">
          <el-input v-model="profileForm.phone" />
        </el-form-item>
        <el-button type="primary" :loading="savingProfile" native-type="submit">儲存變更</el-button>
      </el-form>
    </el-card>

    <el-card>
      <h3>修改密碼</h3>
      <el-form :model="pwForm" label-width="80px" @submit.prevent="handleChangePassword">
        <el-form-item label="原密碼">
          <el-input v-model="pwForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密碼">
          <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="至少 8 字元" />
        </el-form-item>
        <el-form-item label="驗證碼">
          <div class="code-row">
            <el-input v-model="pwForm.verificationCode" maxlength="6" placeholder="6 位數 Email 驗證碼" />
            <el-button :loading="sendingCode" :disabled="cooldown > 0" @click="handleSendCode">
              {{ cooldown > 0 ? `${cooldown} 秒後可重寄` : '寄送驗證碼' }}
            </el-button>
          </div>
          <div class="code-hint">驗證碼會寄到 {{ profileForm.email || '您的註冊信箱' }}，5 分鐘內有效</div>
        </el-form-item>
        <el-button type="primary" :loading="savingPassword" native-type="submit">修改密碼</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getMember, updateProfile, changePassword, sendPasswordCode } from '@/api/member'
import { useAuthStore } from '@/store/auth'

const CODE_COOLDOWN_SECONDS = 60

const authStore = useAuthStore()
const profileForm = reactive({ username: '', email: '', realName: '', phone: '' })
const pwForm = reactive({ oldPassword: '', newPassword: '', verificationCode: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)
const sendingCode = ref(false)
const cooldown = ref(0)
let cooldownTimer = null

async function fetchProfile() {
  const member = await getMember(authStore.userId)
  Object.assign(profileForm, member)
}

async function handleUpdateProfile() {
  savingProfile.value = true
  try {
    await updateProfile(authStore.userId, { realName: profileForm.realName, phone: profileForm.phone })
    ElMessage.success('個人資料已更新')
  } finally {
    savingProfile.value = false
  }
}

function startCooldown() {
  cooldown.value = CODE_COOLDOWN_SECONDS
  clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

async function handleSendCode() {
  sendingCode.value = true
  try {
    await sendPasswordCode(authStore.userId)
    ElMessage.success('驗證碼已寄出，請至信箱查看')
    startCooldown()
  } finally {
    sendingCode.value = false
  }
}

async function handleChangePassword() {
  savingPassword.value = true
  try {
    await changePassword(authStore.userId, pwForm)
    ElMessage.success('密碼已修改')
    pwForm.oldPassword = ''
    pwForm.newPassword = ''
    pwForm.verificationCode = ''
  } finally {
    savingPassword.value = false
  }
}

onMounted(fetchProfile)
onBeforeUnmount(() => clearInterval(cooldownTimer))
</script>

<style scoped>
.page-container {
  max-width: 600px;
  margin: 24px auto;
  padding: 0 16px;
}

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
