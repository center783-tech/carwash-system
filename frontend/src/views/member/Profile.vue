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
        <el-button type="primary" :loading="savingPassword" native-type="submit">修改密碼</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMember, updateProfile, changePassword } from '@/api/member'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const profileForm = reactive({ username: '', email: '', realName: '', phone: '' })
const pwForm = reactive({ oldPassword: '', newPassword: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)

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

async function handleChangePassword() {
  savingPassword.value = true
  try {
    await changePassword(authStore.userId, pwForm)
    ElMessage.success('密碼已修改')
    pwForm.oldPassword = ''
    pwForm.newPassword = ''
  } finally {
    savingPassword.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.page-container {
  max-width: 600px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
