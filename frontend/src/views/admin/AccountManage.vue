<template>
  <div>
    <AdminNav />
    <div class="page-container">
      <h2>帳號管理</h2>
      <p class="hint">
        管理員：全部功能；後台人員：除營收報表外的後台功能；員工：只能登入查看總班表。
      </p>

      <div class="header-row">
        <el-button type="primary" @click="openDialog">新增帳號</el-button>
      </div>

      <el-table :data="accounts" v-loading="loading" empty-text="尚無帳號">
        <el-table-column prop="username" label="帳號" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ ROLE_LABEL[row.role] || row.role }}</template>
        </el-table-column>
        <el-table-column label="狀態" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '啟用' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
                v-if="row.id !== Number(authStore.userId)"
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '啟用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" title="新增帳號">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="帳號" prop="username">
            <el-input v-model="form.username" maxlength="50" />
          </el-form-item>
          <el-form-item label="密碼" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="至少 8 碼，含英文與數字" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" style="width: 100%">
              <el-option v-for="(label, value) in ROLE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">建立</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listAccounts, createAccount, updateAccountStatus } from '@/api/account'
import { useAuthStore } from '@/store/auth'

const ROLE_LABEL = { ADMIN: '管理員', STAFF: '後台人員', EMPLOYEE: '員工' }

const authStore = useAuthStore()
const accounts = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', role: 'EMPLOYEE' })

const rules = {
  username: [{ required: true, min: 3, max: 50, message: '帳號需為 3~50 字', trigger: 'blur' }],
  password: [
    { required: true, message: '請輸入密碼', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/, message: '至少 8 碼，需同時包含英文字母與數字', trigger: 'blur' }
  ],
  role: [{ required: true, message: '請選擇角色', trigger: 'change' }]
}

async function fetchAccounts() {
  loading.value = true
  try {
    accounts.value = await listAccounts()
  } finally {
    loading.value = false
  }
}

function openDialog() {
  Object.assign(form, { username: '', password: '', role: 'EMPLOYEE' })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await createAccount({ ...form })
    ElMessage.success('帳號建立成功')
    dialogVisible.value = false
    await fetchAccounts()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`確定要${next === 1 ? '啟用' : '停用'}帳號「${row.username}」嗎？`, '確認', { type: 'warning' })
  await updateAccountStatus(row.id, next)
  ElMessage.success('已更新')
  await fetchAccounts()
}

onMounted(fetchAccounts)
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 0 16px;
}
.hint {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.header-row {
  margin-bottom: 12px;
}
</style>
